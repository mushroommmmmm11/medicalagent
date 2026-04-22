#!/usr/bin/env python
# -*- coding: utf-8 -*-

import base64
import hashlib
import json
import logging
import os
import re
from pathlib import Path
from typing import Any, Dict, List, Optional

import httpx
import redis
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

logging.basicConfig(level=os.getenv("PYTHON_LOG_LEVEL", "INFO"))
logger = logging.getLogger(__name__)


def _first_env(*names: str, default: str = "") -> str:
    for name in names:
        value = os.getenv(name)
        if value:
            return value
    return default


def _normalize_openai_base_url(url: str) -> str:
    normalized = (url or "").strip().rstrip("/")
    if normalized.endswith("/chat/completions"):
        return normalized[: -len("/chat/completions")]
    return normalized


LLM_API_KEY = _first_env("LLM_API_KEY", "DASHSCOPE_API_KEY", "OPENAI_API_KEY", default="")
LLM_BASE_URL = _normalize_openai_base_url(
    _first_env("LLM_BASE_URL", "DASHSCOPE_BASE_URL", default="https://dashscope.aliyuncs.com/compatible-mode/v1")
)
VISION_MODEL = _first_env("VISION_MODEL", "LLM_MODEL", "DASHSCOPE_MODEL", default="qwen-vl-plus-2025-05-07")
LLM_TEMPERATURE = float(_first_env("LLM_TEMPERATURE", default="0.2"))
HTTP_TIMEOUT = float(os.getenv("HTTP_TIMEOUT", "30"))
LLM_API_TIMEOUT = float(os.getenv("LLM_API_TIMEOUT", "120"))
REDIS_HOST = os.getenv("REDIS_HOST", "redis")
REDIS_PORT = int(os.getenv("REDIS_PORT", "6379"))
REDIS_TTL = int(os.getenv("OCR_CACHE_TTL_SECONDS", "604800"))
UPLOAD_DIR = os.getenv("UPLOAD_DIR", "/app/uploads")
USE_MOCK_LLM = os.getenv("USE_MOCK_LLM", "false").lower() == "true"
CACHE_SCHEMA_VERSION = "v3"

app = FastAPI(title="OCR Service", version="2.0")


class AnalyzeVisionRequest(BaseModel):
    path: str
    model: Optional[str] = None
    force_recheck: bool = False
    focus_item: Optional[str] = None


def _get_redis_client() -> redis.Redis:
    return redis.Redis(
        host=REDIS_HOST,
        port=REDIS_PORT,
        decode_responses=True,
        socket_connect_timeout=5,
    )


def _cache_key(file_path: str, focus_item: Optional[str]) -> str:
    raw = f"{CACHE_SCHEMA_VERSION}|{file_path}|{focus_item or ''}|{VISION_MODEL}"
    digest = hashlib.md5(raw.encode("utf-8")).hexdigest()
    return f"ocr_result:{digest}"


def _read_cache(file_path: str, focus_item: Optional[str]) -> Optional[Dict[str, Any]]:
    try:
        raw = _get_redis_client().get(_cache_key(file_path, focus_item))
        if not raw:
            return None
        payload = json.loads(raw)
        if isinstance(payload, dict):
            payload["cached"] = True
            payload["cache_hit"] = True
            return payload
    except Exception as exc:
        logger.warning("Redis read failed: %s", exc)
    return None


def _write_cache(file_path: str, focus_item: Optional[str], result: Dict[str, Any]) -> None:
    try:
        _get_redis_client().setex(
            _cache_key(file_path, focus_item),
            REDIS_TTL,
            json.dumps(result, ensure_ascii=False),
        )
    except Exception as exc:
        logger.warning("Redis write failed: %s", exc)


def _resolve_local_path(path_str: str) -> Path:
    candidate = Path(path_str)
    if candidate.is_absolute():
        return candidate
    normalized = path_str.lstrip("/\\")
    if normalized.startswith("uploads/"):
        normalized = normalized[len("uploads/") :]
    return Path(UPLOAD_DIR) / normalized


async def _load_image_as_base64(path_or_url: str) -> str:
    if path_or_url.startswith(("http://", "https://")):
        async with httpx.AsyncClient(timeout=HTTP_TIMEOUT, trust_env=False) as client:
            response = await client.get(path_or_url)
            response.raise_for_status()
            return base64.b64encode(response.content).decode("utf-8")

    local_path = _resolve_local_path(path_or_url)
    if not local_path.exists():
        raise FileNotFoundError(f"Image not found: {local_path}")
    return base64.b64encode(local_path.read_bytes()).decode("utf-8")


def _extract_numeric_value(value: Any) -> Optional[float]:
    if isinstance(value, (int, float)):
        return float(value)
    match = re.search(r"[-+]?[0-9]*\.?[0-9]+", str(value or ""))
    if not match:
        return None
    try:
        return float(match.group())
    except ValueError:
        return None


def _normalize_status(raw_status: Any, value: Optional[float], normal_range: str) -> str:
    status = str(raw_status or "").strip().lower()
    if status in {"h", "high", "up", "↑", "偏高", "升高"}:
        return "high"
    if status in {"l", "low", "down", "↓", "偏低", "降低"}:
        return "low"
    if status in {"n", "normal", "正常", ""}:
        numbers = re.findall(r"[-+]?[0-9]*\.?[0-9]+", normal_range or "")
        if value is not None and len(numbers) >= 2:
            low = float(numbers[0])
            high = float(numbers[1])
            if value < low:
                return "low"
            if value > high:
                return "high"
        return "normal"
    return status


def _item_name(entry: Dict[str, Any]) -> str:
    return str(
        entry.get("item")
        or entry.get("name")
        or entry.get("indicator")
        or entry.get("test_name")
        or entry.get("test_item")
        or ""
    ).strip()


def _normal_range(entry: Dict[str, Any]) -> str:
    return str(
        entry.get("normal_range")
        or entry.get("reference_range")
        or entry.get("range")
        or entry.get("reference")
        or ""
    ).strip()


def _coerce_analysis_items(payload: Any) -> List[Dict[str, str]]:
    if isinstance(payload, str):
        try:
            payload = json.loads(_strip_json_fence(payload))
        except json.JSONDecodeError:
            payload = _parse_colon_lines(payload)

    if isinstance(payload, dict):
        payload = (
            payload.get("analysis")
            or payload.get("items")
            or payload.get("tests")
            or payload.get("results")
            or []
        )

    if not isinstance(payload, list):
        return []

    items: List[Dict[str, str]] = []
    for entry in payload:
        if isinstance(entry, str):
            parsed = _parse_colon_line(entry)
            if parsed:
                items.append(parsed)
            continue
        if not isinstance(entry, dict):
            continue

        name = _item_name(entry)
        value_raw = entry.get("value", entry.get("result", ""))
        numeric_value = _extract_numeric_value(value_raw)
        if not name or numeric_value is None:
            continue

        normal_range = _normal_range(entry)
        status_raw = (
            entry.get("status")
            or entry.get("abnormal_flag")
            or entry.get("flag")
            or entry.get("result_flag")
        )
        items.append(
            {
                "item": name,
                "value": str(value_raw).strip(),
                "unit": str(entry.get("unit") or "").strip(),
                "normal_range": normal_range,
                "status": _normalize_status(status_raw, numeric_value, normal_range),
            }
        )
    return items


def _strip_json_fence(text: str) -> str:
    cleaned = text.strip()
    if cleaned.startswith("```"):
        cleaned = re.sub(r"^```(?:json)?\s*", "", cleaned, flags=re.IGNORECASE)
        cleaned = re.sub(r"\s*```$", "", cleaned)
    return cleaned.strip()


def _parse_colon_lines(text: str) -> List[Dict[str, str]]:
    items: List[Dict[str, str]] = []
    for line in text.splitlines():
        parsed = _parse_colon_line(line)
        if parsed:
            items.append(parsed)
    return items


def _parse_colon_line(line: str) -> Optional[Dict[str, str]]:
    if ":" not in line:
        return None
    name, rest = [part.strip() for part in line.split(":", 1)]
    value = _extract_numeric_value(rest)
    if not name or value is None:
        return None
    unit_match = re.search(r"[-+]?[0-9]*\.?[0-9]+\s*(.*)", rest)
    unit = unit_match.group(1).strip() if unit_match else ""
    return {
        "item": name,
        "value": str(value),
        "unit": unit,
        "normal_range": "",
        "status": "unknown",
    }


def _normalize_indicator_key(item_name: str, unit: str = "") -> Optional[str]:
    text = item_name.upper()
    compact = re.sub(r"[^A-Z0-9#%+-]", "", text)

    if "CRP" in compact:
        return "CRP"
    if "WBC" in compact:
        return "WBC"
    if "NRBC" in compact and "%" in compact:
        return "NRBC%"
    if "NRBC" in compact:
        return "NRBC#"
    if ("RDW" in compact or "分布宽度" in item_name) and "CV" in compact:
        return "RDW-CV"
    if ("RDW" in compact or "分布宽度" in item_name) and "SD" in compact:
        return "RDW-SD"
    if "RBC" in compact and "RDW" not in compact:
        return "RBC"
    if "HGB" in compact or re.search(r"(?<![A-Z])HB(?![A-Z])", compact):
        return "Hb"
    if "HCT" in compact:
        return "HCT"
    if "MCHC" in compact:
        return "MCHC"
    if "MCH" in compact:
        return "MCH"
    if "MCV" in compact:
        return "MCV"
    if "PLT" in compact:
        return "PLT"
    if "MPV" in compact:
        return "MPV"
    if "PDW" in compact:
        return "PDW"
    if "PCT" in compact:
        return "PCT"
    if "PLCR" in compact or "P-LCR" in compact:
        return "P-LCR"

    family = None
    for token in ("NEUT", "BASO", "LY", "MO", "EO"):
        if token in compact:
            family = token
            break
    if family:
        is_count = "10^" in unit or "/L" in unit.upper() or "#" in compact or "计数" in item_name
        suffix = "n" if is_count else "p"
        return f"{family}-{suffix}"

    return None


def _build_structured_payload(llm_analysis: str) -> Dict[str, Any]:
    analysis_items = _coerce_analysis_items(llm_analysis)
    patient_labs: Dict[str, float] = {}
    full_extraction: List[str] = []

    for item in analysis_items:
        line = f"{item['item']}: {item['value']} {item['unit']}".strip()
        full_extraction.append(line)
        key = _normalize_indicator_key(item["item"], item.get("unit", ""))
        numeric_value = _extract_numeric_value(item.get("value"))
        if key and numeric_value is not None and key not in patient_labs:
            patient_labs[key] = numeric_value

    return {
        "analysis": analysis_items,
        "full_extraction": full_extraction,
        "gat_structured": {
            "patient_labs": patient_labs,
            "mapped_count": len(patient_labs),
            "total_items": len(analysis_items),
            "coverage": f"{len(patient_labs) * 100 // max(1, len(analysis_items))}%" if analysis_items else "0%",
        },
    }


def _mock_ocr_response(focus_item: Optional[str]) -> str:
    items = [
        {"item": "C反应蛋白(超敏) CRP", "value": "18.11", "unit": "mg/L", "normal_range": "0.00 - 10.00", "status": "high"},
        {"item": "白细胞计数 WBC", "value": "11.42", "unit": "x10^9/L", "normal_range": "4.00 - 10.00", "status": "high"},
        {"item": "中性粒细胞比例 NEUT", "value": "0.777", "unit": "", "normal_range": "0.460 - 0.750", "status": "high"},
        {"item": "淋巴细胞比例 LY", "value": "0.161", "unit": "", "normal_range": "0.190 - 0.470", "status": "low"},
    ]
    if focus_item:
        items.insert(0, {"item": focus_item, "value": "", "unit": "", "normal_range": "", "status": "unknown"})
    return json.dumps({"analysis": items}, ensure_ascii=False)


async def _call_vision_api(image_base64: str, focus_item: Optional[str], model_name: Optional[str]) -> str:
    if USE_MOCK_LLM or not LLM_API_KEY or LLM_API_KEY.strip() in {"", "sk-"}:
        logger.info("Using mock OCR response")
        return _mock_ocr_response(focus_item)

    prompt = (
        "Extract every lab test item from this medical report image. "
        "Return JSON only with this shape: "
        '{"analysis":[{"item":"指标名","value":"数值","unit":"单位","normal_range":"参考范围","status":"high|low|normal|unknown"}]}. '
        "Use the reference range and abnormal flag printed on the report. "
        "Keep ratios such as 0.777 as decimals if the report prints decimals. "
        "If a field is absent, use an empty string."
    )
    if focus_item:
        prompt += f" Prioritize the item related to: {focus_item}."

    payload = {
        "model": model_name or VISION_MODEL,
        "messages": [
            {
                "role": "user",
                "content": [
                    {"type": "image_url", "image_url": {"url": f"data:image/jpeg;base64,{image_base64}"}},
                    {"type": "text", "text": prompt},
                ],
            }
        ],
        "temperature": LLM_TEMPERATURE,
    }

    async with httpx.AsyncClient(timeout=LLM_API_TIMEOUT, trust_env=False) as client:
        response = await client.post(
            f"{LLM_BASE_URL}/chat/completions",
            json=payload,
            headers={
                "Authorization": f"Bearer {LLM_API_KEY}",
                "Content-Type": "application/json",
            },
        )

    if response.status_code >= 400:
        logger.error("Vision API failed: status=%s body=%s", response.status_code, response.text)
    response.raise_for_status()

    payload = response.json()
    choices = payload.get("choices") or []
    if not choices:
        raise ValueError("Vision API returned no choices")

    content = (choices[0].get("message") or {}).get("content", "")
    if isinstance(content, list):
        return "\n".join(part.get("text", "") for part in content if isinstance(part, dict) and part.get("text"))
    if isinstance(content, str):
        return content
    raise ValueError("Vision API returned unsupported content format")


@app.post("/api/v1/analyze-vision")
async def analyze_vision(request: AnalyzeVisionRequest) -> Dict[str, Any]:
    if not request.force_recheck:
        cached = _read_cache(request.path, request.focus_item)
        if cached is not None:
            return cached

    try:
        image_base64 = await _load_image_as_base64(request.path)
        llm_analysis = await _call_vision_api(image_base64, request.focus_item, request.model)
        structured = _build_structured_payload(llm_analysis)
        response = {
            "status": "success",
            "success": True,
            "file_path": request.path,
            "path": request.path,
            "model": request.model or VISION_MODEL,
            "cached": False,
            "cache_hit": False,
            "cache_key": _cache_key(request.path, request.focus_item),
            "focus_item": request.focus_item,
            **structured,
        }
        _write_cache(request.path, request.focus_item, response)
        return response
    except FileNotFoundError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
    except httpx.HTTPError as exc:
        raise HTTPException(status_code=502, detail=f"Vision API request failed: {exc}") from exc
    except Exception as exc:
        logger.exception("analyze_vision failed")
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@app.get("/api/v1/health")
async def health_check() -> Dict[str, str]:
    redis_status = "ok"
    try:
        _get_redis_client().ping()
    except Exception:
        redis_status = "unavailable"
    return {"status": "healthy", "redis": redis_status}


@app.get("/api/v1/models")
async def list_models() -> Dict[str, Any]:
    return {
        "models": [VISION_MODEL],
        "default": VISION_MODEL,
        "base_url": LLM_BASE_URL,
    }
