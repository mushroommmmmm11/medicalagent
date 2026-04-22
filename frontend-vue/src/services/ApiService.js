import axios from "axios";

const apiClient = axios.create({
  baseURL: "/api",
  headers: {
    "Content-Type": "application/json",
  },
});

apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error),
);

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem("token");
      localStorage.removeItem("user");
      window.location.href = "/login";
    }
    return Promise.reject(error.response?.data || error);
  },
);

export default {
  setAuthToken(token) {
    if (token) {
      apiClient.defaults.headers.common["Authorization"] = `Bearer ${token}`;
    } else {
      delete apiClient.defaults.headers.common["Authorization"];
    }
  },

  post(url, data) {
    return apiClient.post(url, data);
  },

  get(url, config) {
    return apiClient.get(url, config);
  },

  put(url, data) {
    return apiClient.put(url, data);
  },

  delete(url) {
    return apiClient.delete(url);
  },

  async checkHealth() {
    try {
      const response = await apiClient.get("/v1/health");
      return response.data;
    } catch (error) {
      console.error("Health check failed:", error);
      throw error;
    }
  },

  async analyzeReport(reportContent) {
    try {
      const response = await apiClient.post("/v1/agent/analyze-report", null, {
        params: { reportContent },
      });
      return response.data;
    } catch (error) {
      console.error("Analyze report failed:", error);
      throw error;
    }
  },

  async chat(userQuery) {
    try {
      const response = await apiClient.post("/v1/agent/chat", null, {
        params: { userQuery },
      });
      return response.data;
    } catch (error) {
      console.error("Chat request failed:", error);
      throw error;
    }
  },

  async uploadReport(file) {
    try {
      const formData = new FormData();
      formData.append("file", file);
      const response = await apiClient.post("/v1/agent/upload-report", formData, {
        headers: {
          "Content-Type": "multipart/form-data",
        },
      });
      return response.data;
    } catch (error) {
      console.error("Upload report failed:", error);
      throw error;
    }
  },

  async getReportInsights(filePath) {
    try {
      const response = await apiClient.get("/v1/agent/report-insights", {
        params: { filePath },
      });
      return response.data;
    } catch (error) {
      console.error("Get report insights failed:", error);
      throw error;
    }
  },

  async searchKnowledge(keyword) {
    try {
      const response = await apiClient.get("/v1/knowledge/search", {
        params: { keyword },
      });
      return response.data;
    } catch (error) {
      console.error("Search knowledge failed:", error);
      throw error;
    }
  },

  async appendMedicalHistory(disease, status) {
    try {
      const response = await apiClient.post("/v1/user/medical-history/append", null, {
        params: { disease, status },
      });
      return response.data;
    } catch (error) {
      console.error("Append medical history failed:", error);
      throw error;
    }
  },

  async updateDrugAllergy(drugAllergy) {
    try {
      const response = await apiClient.post("/v1/user/drug-allergy/update", null, {
        params: { drugAllergy },
      });
      return response.data;
    } catch (error) {
      console.error("Update drug allergy failed:", error);
      throw error;
    }
  },

  async getMedicalHistory() {
    try {
      const response = await apiClient.get("/v1/user/medical-history");
      return response.data;
    } catch (error) {
      console.error("Get medical history failed:", error);
      throw error;
    }
  },

  async logout() {
    try {
      await apiClient.post("/v1/auth/logout");
    } catch (error) {
      console.error("Logout request failed:", error);
    }
  },

  async extractKeywords(text) {
    try {
      const response = await apiClient.post("/v1/agent/extract-keywords", {
        text,
      });
      return {
        isMedical: response.data.isMedical || false,
        diseases: response.data.diseases || "",
        drugAllergies: response.data.drugAllergies || "",
      };
    } catch (error) {
      console.error("Extract keywords failed:", error);
      return { isMedical: false, diseases: "", drugAllergies: "" };
    }
  },

  async streamChat(userQuery, onMessage, onError, onDone, options = {}) {
    const token = localStorage.getItem("token");
    const response = await fetch(`/api/v1/agent/chat/stream`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
        Accept: "text/event-stream",
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        query: userQuery,
        ocrResult: options.ocrResult || undefined,
      }),
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder("utf-8");
    let buffer = "";
    let streamDone = false;
    let streamMeta = null;

    function processEvent(eventText) {
      const dataLines = [];
      for (const line of eventText.split(/\r?\n/)) {
        const trimmed = line.trim();
        if (trimmed.startsWith("data:")) {
          dataLines.push(trimmed.replace(/^data:\s*/, ""));
        }
      }

      if (dataLines.length === 0) return;
      const fullData = dataLines.join("\n").trim();
      if (!fullData || fullData === "[DONE]") {
        streamDone = true;
        return;
      }

      if (fullData.startsWith("[META:") && fullData.endsWith("]")) {
        try {
          const metaJson = fullData.substring(6, fullData.length - 1);
          streamMeta = JSON.parse(metaJson);
        } catch (e) {
          console.error("META parse error:", e);
        }
        return;
      }

      try {
        const parsed = JSON.parse(fullData);
        if (parsed.content !== undefined) {
          onMessage(parsed.content);
          return;
        }
      } catch (e) {
        // 不是 JSON，按普通文本处理
      }

      onMessage(fullData);
    }

    try {
      while (true) {
        const { value, done } = await reader.read();
        if (done || streamDone) {
          if (buffer.trim()) {
            processEvent(buffer);
          }
          break;
        }

        buffer += decoder.decode(value, { stream: true });
        const events = buffer.split(/\n\n/);
        buffer = events.pop() || "";

        for (const event of events) {
          processEvent(event);
          if (streamDone) break;
        }
        if (streamDone) break;
      }
    } catch (error) {
      if (onError) onError(error);
      throw error;
    } finally {
      try {
        reader.cancel();
      } catch (e) {
        /* ignore */
      }
      if (onDone) onDone(streamMeta);
    }
  },
};
