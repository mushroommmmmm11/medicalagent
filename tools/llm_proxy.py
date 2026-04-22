#!/usr/bin/env python
# -*- coding: utf-8 -*-

import argparse
import http.server
import json
import os
import time

import httpx


def load_env(path):
    values = {}
    if not path or not os.path.exists(path):
        return values
    with open(path, "r", encoding="utf-8") as handle:
        for line in handle:
            stripped = line.strip()
            if not stripped or stripped.startswith("#") or "=" not in stripped:
                continue
            key, value = stripped.split("=", 1)
            values[key.strip()] = value.strip()
    return values


def normalize_base_url(url):
    normalized = (url or "").strip().rstrip("/")
    if normalized.endswith("/chat/completions"):
        return normalized[: -len("/chat/completions")]
    return normalized


class LlmProxyHandler(http.server.BaseHTTPRequestHandler):
    upstream_base_url = ""
    upstream_api_key = ""

    def do_GET(self):
        if self.path in {"/health", "/v1/health"}:
            self._send_json(200, {"status": "ok"})
            return
        self._send_json(404, {"error": "not found"})

    def do_POST(self):
        if self.path != "/v1/chat/completions":
            self._send_json(404, {"error": "not found"})
            return

        length = int(self.headers.get("Content-Length", "0"))
        body = self.rfile.read(length)
        target = f"{self.upstream_base_url}/chat/completions"
        headers = {
            "Content-Type": "application/json",
            "Authorization": f"Bearer {self.upstream_api_key}",
        }
        last_error = None
        for attempt in range(1, 4):
            try:
                with httpx.Client(timeout=180, trust_env=False) as client:
                    with client.stream("POST", target, content=body, headers=headers) as response:
                        self.send_response(response.status_code)
                        self.send_header("Content-Type", response.headers.get("Content-Type", "application/json"))
                        self.send_header("Cache-Control", "no-cache")
                        self.end_headers()
                        for chunk in response.iter_raw():
                            if chunk:
                                self.wfile.write(chunk)
                                self.wfile.flush()
                        return
            except Exception as exc:
                last_error = exc
                print(f"Upstream LLM request failed on attempt {attempt}: {exc}", flush=True)
                if attempt < 3:
                    time.sleep(attempt)

        self._send_json(502, {"error": str(last_error)})

    def _proxy_with_urllib(self, target, body, headers):
        """Kept out of the hot path; useful if a minimal image ever lacks httpx."""
        import urllib.error
        import urllib.request

        request = urllib.request.Request(target, data=body, headers=headers, method="POST")
        with urllib.request.urlopen(request, timeout=180) as response:
            response_body = response.read()
            self.send_response(response.status)
            self.send_header("Content-Type", response.headers.get("Content-Type", "application/json"))
            self.send_header("Content-Length", str(len(response_body)))
            self.end_headers()
            self.wfile.write(response_body)

    def log_message(self, fmt, *args):
        print("%s - %s" % (self.address_string(), fmt % args), flush=True)

    def _send_json(self, status, payload):
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--env-file", default=".env")
    parser.add_argument("--host", default="0.0.0.0")
    parser.add_argument("--port", type=int, default=18081)
    args = parser.parse_args()

    env = load_env(args.env_file)
    upstream_base = normalize_base_url(
        os.getenv("UPSTREAM_LLM_BASE_URL")
        or env.get("UPSTREAM_LLM_BASE_URL")
        or env.get("LLM_BASE_URL")
        or env.get("DASHSCOPE_BASE_URL")
    )
    upstream_key = (
        os.getenv("UPSTREAM_LLM_API_KEY")
        or env.get("UPSTREAM_LLM_API_KEY")
        or env.get("LLM_API_KEY")
        or env.get("DASHSCOPE_API_KEY")
    )
    if not upstream_base or not upstream_key:
        raise SystemExit("Missing upstream base URL or API key")

    LlmProxyHandler.upstream_base_url = upstream_base
    LlmProxyHandler.upstream_api_key = upstream_key

    server = http.server.ThreadingHTTPServer((args.host, args.port), LlmProxyHandler)
    print(f"LLM proxy listening on http://{args.host}:{args.port}/v1 -> {upstream_base}", flush=True)
    server.serve_forever()


if __name__ == "__main__":
    main()
