<template>
  <div class="chat-container">
    <section v-if="reportInsights && !showInterpretation" class="insights-page">
      <LabInsightsPanel
        :report="reportInsights"
        @interpret="openReportInterpretation"
      />
    </section>

    <div v-if="showInterpretation || !reportInsights" ref="chatWindowRef" class="chat-window" id="report-interpretation">
      <div class="interpretation-layout">
        <section class="interpret-chat">
          <div v-if="reportInsights" class="chat-mode-bar">
            <button class="back-dashboard-btn" type="button" @click="showInterpretation = false">
              返回异常趋势图
            </button>
            <span>检验报告解读</span>
          </div>

          <div class="messages-area" @mouseup="handleMessageSelection">
            <div v-if="messages.length === 0" class="empty-state">
              <h2>欢迎使用 MedLabAgent</h2>
              <p>上传化验单后，我会先提取结构化指标，再继续分析。</p>
              <p class="user-greeter">
                {{ currentUser ? `Hi, ${currentUser.realName}` : "" }}
              </p>
            </div>

            <div v-for="msg in messages" :key="msg.id">
              <ChatMessage :message="msg" />
              <div
                v-if="
                  msg.role === 'assistant' &&
                  msg.content &&
                  !isLoading &&
                  msg.isMedical &&
                  !msg.confirmed &&
                  !msg.rejected
                "
                class="confirm-bar"
              >
                <span class="confirm-hint">是否将这次分析记录到病历？</span>
                <button @click="showSaveDialog(msg)" class="confirm-btn">
                  记录到病历
                </button>
                <button @click="msg.rejected = true" class="reject-btn">
                  不记录
                </button>
              </div>
              <div v-if="msg.confirmed" class="confirm-bar confirmed">
                <span>已记录到病历</span>
              </div>
            </div>

            <div v-if="isLoading && !isStreaming" class="message assistant">
              <div class="message-content">
                <div class="spinner"></div>
              </div>
            </div>
          </div>

          <div
            v-if="selectionMenu.visible"
            class="selection-menu"
            :style="{ left: `${selectionMenu.x}px`, top: `${selectionMenu.y}px` }"
          >
            <button type="button" @mousedown.prevent="quoteSelection">引用</button>
            <span></span>
            <button type="button" @mousedown.prevent="memoSelection">摘录</button>
          </div>

          <div v-if="error" class="error-message">
            {{ error }}
          </div>

          <div class="input-area">
            <div v-if="activeQuote" class="quote-display">
              <span>引用</span>
              <strong>{{ activeQuote }}</strong>
              <button type="button" @click="clearQuote">×</button>
            </div>
            <div class="input-row">
              <input
                v-model="userInput"
                type="text"
                :placeholder="activeQuote ? '针对引用内容继续提问' : '输入问题，或上传化验单开始分析'"
                @keyup.enter="sendMessage"
                :disabled="isLoading"
              />
              <button @click="sendMessage" :disabled="isLoading || !userInput">
                {{ isLoading ? "发送中..." : "发送" }}
              </button>
              <button @click="uploadFile" :disabled="isLoading">上传</button>
            </div>
          </div>
        </section>

        <aside class="memo-section">
          <div class="memo-header">
            <span>我的摘录备忘</span>
            <small>共 {{ memos.length }} 条</small>
          </div>
          <div class="memo-list">
            <div v-if="memos.length === 0" class="memo-empty">
              在左侧 AI 解读中划词，选择“摘录”后会出现在这里。
            </div>
            <article v-for="memo in memos" :key="memo.id" class="memo-card">
              <div class="memo-card-top">
                <span>{{ memo.time }}</span>
                <button type="button" @click="removeMemo(memo.id)">×</button>
              </div>
              <blockquote>{{ memo.quote }}</blockquote>
              <textarea
                v-model="memo.note"
                rows="3"
                placeholder="在这里补充备注，例如：复查这个指标、问医生感染来源"
              ></textarea>
            </article>
          </div>
        </aside>
      </div>
    </div>

    <input
      ref="fileInput"
      type="file"
      style="display: none"
      @change="handleFileUpload"
      accept="image/*,.pdf"
    />

    <div v-if="showMedicalDialog" class="dialog-overlay">
      <div class="dialog-box">
        <h3>记录到病历</h3>
        <div class="dialog-field">
          <label>疾病或症状</label>
          <input
            v-model="medicalForm.disease"
            type="text"
            placeholder="例如：急性扁桃体炎、高热"
          />
        </div>
        <div class="dialog-field">
          <label>药物过敏</label>
          <input
            v-model="medicalForm.drugAllergy"
            type="text"
            placeholder="例如：青霉素；没有可留空"
          />
        </div>
        <div class="dialog-field">
          <label>当前状态</label>
          <select v-model="medicalForm.status">
            <option value="未康复">未康复</option>
            <option value="已康复">已康复</option>
            <option value="待观察">待观察</option>
          </select>
        </div>
        <div class="dialog-actions">
          <button @click="confirmSave" class="confirm-btn">确认保存</button>
          <button @click="cancelSave" class="reject-btn">取消</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, onBeforeUnmount, computed, nextTick } from "vue";
import ChatMessage from "./ChatMessage.vue";
import LabInsightsPanel from "./LabInsightsPanel.vue";
import { useChatStore } from "../stores/chatStore";
import { useAuthStore } from "../stores/authStore";
import ApiService from "../services/ApiService";
import { useRouter } from "vue-router";

export default {
  name: "ChatWindow",
  components: {
    ChatMessage,
    LabInsightsPanel,
  },
  setup() {
    const chatStore = useChatStore();
    const authStore = useAuthStore();
    const router = useRouter();
    const userInput = ref("");
    const fileInput = ref(null);
    const chatWindowRef = ref(null);
    const isLoading = ref(false);
    const isStreaming = ref(false);
    const error = ref(null);
    const reportInsights = ref(null);
    const showInterpretation = ref(false);
    const showMedicalDialog = ref(false);
    const currentSaveMsg = ref(null);
    const activeQuote = ref("");
    const selectedText = ref("");
    const memos = ref([]);
    const selectionMenu = ref({
      visible: false,
      x: 0,
      y: 0,
    });
    const medicalForm = ref({
      disease: "",
      drugAllergy: "",
      status: "未康复",
    });

    const messages = computed(() => chatStore.messages);
    const currentUser = computed(() => authStore.user);
    const hasReportInterpretation = computed(() =>
      chatStore.messages.some(
        (message) =>
          message.role === "assistant" &&
          typeof message.content === "string" &&
          message.content.trim().length > 0,
      ),
    );

    const scrollToBottom = async () => {
      await nextTick();
      const container = document.querySelector(".messages-area");
      if (container) {
        container.scrollTop = container.scrollHeight;
      }
    };

    function handlePageClose() {
      const token = localStorage.getItem("token");
      if (token) {
        navigator.sendBeacon(
          "/api/v1/auth/logout?token=" + encodeURIComponent(token),
        );
      }
    }

    onMounted(async () => {
      if (!authStore.isLoggedIn && localStorage.getItem("token")) {
        authStore.restoreAuth();
      }

      if (!authStore.isLoggedIn) {
        router.push("/login");
        return;
      }

      window.addEventListener("beforeunload", handlePageClose);
      document.addEventListener("mousedown", hideSelectionMenu);

      try {
        await ApiService.checkHealth();
      } catch (err) {
        error.value = "无法连接到后端服务，请检查服务是否运行。";
      }
    });

    onBeforeUnmount(() => {
      window.removeEventListener("beforeunload", handlePageClose);
      document.removeEventListener("mousedown", hideSelectionMenu);
    });

    async function sendMessage(options = {}) {
      if (!userInput.value.trim()) return;

      const rawInput = userInput.value;
      const userMessage = activeQuote.value
        ? `引用：“${activeQuote.value}”\n${rawInput}`
        : rawInput;
      chatStore.addMessage({
        role: "user",
        content: userMessage,
        id: Date.now(),
        timestamp: new Date(),
      });

      userInput.value = "";
      activeQuote.value = "";
      isLoading.value = true;
      isStreaming.value = false;
      error.value = null;
      scrollToBottom();

      chatStore.addMessage({
        role: "assistant",
        content: "",
        id: Date.now() + 1,
        timestamp: new Date(),
      });
      scrollToBottom();

      try {
        await ApiService.streamChat(
          userMessage,
          (chunk) => {
            isStreaming.value = true;
            const lastIndex = chatStore.messages.length - 1;
            const msg = chatStore.messages[lastIndex];
            if (msg && msg.role === "assistant") {
              msg.content += chunk;
              scrollToBottom();
            }
          },
          (streamErr) => {
            console.error("Stream error:", streamErr);
          },
          (metadata) => {
            const lastMsg = chatStore.messages[chatStore.messages.length - 1];
            if (lastMsg && lastMsg.role === "assistant" && metadata) {
              lastMsg.isMedical = metadata.isMedical || false;
              lastMsg.extractedDiseases = metadata.diseases || "";
              lastMsg.extractedDrugAllergy = metadata.drugAllergies || "";
              lastMsg.content = lastMsg.content
                .replace(/\n?\[META\|[^\]]*\]/g, "")
                .trimEnd();
            }
          },
          { ocrResult: options.ocrResult || reportInsights.value?.ocrResult || null },
        );
      } catch (err) {
        error.value = "通信失败：" + err.message;
        const lastIndex = chatStore.messages.length - 1;
        const msg = chatStore.messages[lastIndex];
        if (msg && msg.role === "assistant") {
          msg.content = "抱歉，连接服务时出错了。";
        }
      } finally {
        isLoading.value = false;
        isStreaming.value = false;
      }
    }

    async function openReportInterpretation() {
      showInterpretation.value = true;
      await nextTick();
      chatWindowRef.value?.scrollIntoView({ behavior: "auto", block: "start" });

      if (hasReportInterpretation.value || isLoading.value) {
        return;
      }

      userInput.value = "请根据这张化验单给出检验报告解读，重点说明异常指标、偏离程度、可能原因和下一步建议。";
      await nextTick();
      await sendMessage({
        ocrResult: reportInsights.value?.ocrResult || null,
      });
    }

    function handleMessageSelection(event) {
      const selection = window.getSelection();
      const text = selection?.toString().trim();
      if (!text) {
        return;
      }

      selectedText.value = text.length > 500 ? text.slice(0, 500) : text;
      selectionMenu.value = {
        visible: true,
        x: Math.min(event.clientX, window.innerWidth - 180),
        y: Math.max(event.clientY - 48, 12),
      };
    }

    function hideSelectionMenu(event) {
      if (event.target.closest?.(".selection-menu")) {
        return;
      }
      selectionMenu.value.visible = false;
    }

    function quoteSelection() {
      if (!selectedText.value) return;
      activeQuote.value = selectedText.value;
      selectionMenu.value.visible = false;
      window.getSelection()?.removeAllRanges();
      nextTick(() => document.querySelector(".input-area input")?.focus());
    }

    function memoSelection() {
      if (!selectedText.value) return;
      const now = new Date();
      memos.value.unshift({
        id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
        time: `摘录于 ${now.getHours()}:${String(now.getMinutes()).padStart(2, "0")}`,
        quote: selectedText.value,
        note: "",
      });
      selectionMenu.value.visible = false;
      window.getSelection()?.removeAllRanges();
    }

    function clearQuote() {
      activeQuote.value = "";
    }

    function removeMemo(id) {
      memos.value = memos.value.filter((memo) => memo.id !== id);
    }

    function uploadFile() {
      fileInput.value.click();
    }

    async function handleFileUpload(event) {
      const file = event.target.files[0];
      if (!file) return;

      isLoading.value = true;
      error.value = null;

      try {
        const response = await ApiService.uploadReport(file);
        if (response.status === "success") {
          chatStore.clearMessages();
          memos.value = [];
          activeQuote.value = "";
          reportInsights.value = await ApiService.getReportInsights(
            response.filePath,
          );
          showInterpretation.value = false;
          userInput.value = "";
        } else {
          error.value = response.message || "上传失败";
        }
      } catch (err) {
        error.value = "上传失败：" + err.message;
      } finally {
        isLoading.value = false;
        event.target.value = "";
      }
    }

    async function handleLogout() {
      await authStore.logout();
      chatStore.clearMessages();
      router.push("/login");
    }

    function showSaveDialog(msg) {
      currentSaveMsg.value = msg;
      medicalForm.value = {
        disease: msg.extractedDiseases || "",
        drugAllergy: msg.extractedDrugAllergy || "",
        status: "未康复",
      };
      showMedicalDialog.value = true;
    }

    async function confirmSave() {
      if (
        !medicalForm.value.disease.trim() &&
        !medicalForm.value.drugAllergy.trim()
      ) {
        return;
      }

      try {
        if (medicalForm.value.disease.trim()) {
          await ApiService.appendMedicalHistory(
            medicalForm.value.disease,
            medicalForm.value.status,
          );
        }
        if (medicalForm.value.drugAllergy.trim()) {
          await ApiService.updateDrugAllergy(medicalForm.value.drugAllergy);
        }
        if (currentSaveMsg.value) {
          currentSaveMsg.value.confirmed = true;
        }
      } catch (err) {
        error.value = "保存病历失败：" + err.message;
      } finally {
        showMedicalDialog.value = false;
        currentSaveMsg.value = null;
      }
    }

    function cancelSave() {
      showMedicalDialog.value = false;
      currentSaveMsg.value = null;
    }

    return {
      userInput,
      fileInput,
      chatWindowRef,
      isLoading,
      isStreaming,
      error,
      reportInsights,
      showInterpretation,
      messages,
      currentUser,
      showMedicalDialog,
      medicalForm,
      activeQuote,
      memos,
      selectionMenu,
      sendMessage,
      uploadFile,
      handleFileUpload,
      handleLogout,
      openReportInterpretation,
      handleMessageSelection,
      quoteSelection,
      memoSelection,
      clearQuote,
      removeMemo,
      showSaveDialog,
      confirmSave,
      cancelSave,
    };
  },
};
</script>

<style scoped>
.chat-container {
  display: flex;
  flex-direction: column;
  min-height: 100%;
  width: 100%;
  background: #f4f5f9;
  font-family:
    -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue",
    Arial, sans-serif;
}

.insights-page {
  min-height: calc(100vh - 76px);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 18px 24px 24px;
  box-sizing: border-box;
  background: #f4f5f9;
}

.insights-page :deep(.dashboard-shell) {
  width: min(100%, 1760px);
  padding: 0;
  border-bottom: none;
}

.insights-page :deep(.dashboard-container) {
  width: 100%;
  max-width: none;
  min-height: calc(100vh - 118px);
  display: flex;
  flex-direction: column;
}

.insights-page :deep(.main-layout) {
  flex: 1;
  height: auto;
  min-height: 620px;
  grid-template-columns: minmax(340px, 1.05fr) minmax(620px, 1.65fr) minmax(300px, 0.9fr);
}

.chat-window {
  flex: 1 1 auto;
  display: flex;
  flex-direction: column;
  width: min(100%, 1760px);
  margin: 0 auto;
  padding: 18px 24px 24px;
  box-sizing: border-box;
  height: calc(100vh - 76px);
  min-height: 0;
  overflow: hidden;
}

.interpretation-layout {
  display: grid;
  grid-template-columns: minmax(0, 3fr) minmax(280px, 1fr);
  gap: 18px;
  flex: 1;
  height: 100%;
  min-height: 0;
}

.interpret-chat,
.memo-section {
  min-height: 0;
  background: #fff;
  border: 1px solid #d8e5dd;
  border-radius: 8px;
  overflow: hidden;
}

.interpret-chat {
  display: flex;
  flex-direction: column;
}

.chat-mode-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-bottom: 1px solid #d8e5dd;
  background: #fff;
  color: #173d32;
  font-weight: 700;
}

.back-dashboard-btn {
  border: 1px solid #176b62;
  border-radius: 8px;
  background: #fff;
  color: #176b62;
  padding: 8px 12px;
  cursor: pointer;
}

.back-dashboard-btn:hover {
  background: #eef7f2;
}

.messages-area {
  flex: 1;
  overflow-y: auto;
  padding: 24px 30px;
  background: #f8f9fa;
  min-height: 0;
}

.empty-state {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 100%;
  color: #6c757d;
  text-align: center;
}

.empty-state h2 {
  margin: 0 0 10px 0;
  color: #495057;
  font-size: 32px;
}

.user-greeter {
  margin-top: 10px;
  font-size: 18px;
  color: #176b62;
}

.message {
  margin-bottom: 16px;
  display: flex;
}

.message.assistant {
  justify-content: flex-start;
}

.message-content {
  display: inline-block;
  max-width: 80%;
  padding: 12px 16px;
  border-radius: 8px;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.selection-menu {
  position: fixed;
  z-index: 2000;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.14);
}

.selection-menu button {
  border: none;
  background: transparent;
  cursor: pointer;
  color: #1a1a1a;
  font-size: 14px;
}

.selection-menu span {
  width: 1px;
  height: 16px;
  background: #d9d9d9;
}

.spinner {
  width: 20px;
  height: 20px;
  border: 3px solid #e9ecef;
  border-top: 3px solid #176b62;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.error-message {
  margin: 12px 16px;
  padding: 12px 16px;
  background: #fff5f5;
  color: #c53030;
  border: 1px solid #feb2b2;
  border-radius: 8px;
}

.input-area {
  padding: 14px 20px 18px;
  border-top: 1px solid #e8e8e8;
  background: #fff;
  width: 100%;
  box-sizing: border-box;
  flex: 0 0 auto;
}

.quote-display {
  display: flex;
  align-items: center;
  gap: 8px;
  width: fit-content;
  max-width: 100%;
  margin-bottom: 10px;
  padding: 7px 10px;
  border-radius: 8px;
  background: #f5f5f5;
  color: #595959;
  font-size: 13px;
}

.quote-display strong {
  max-width: 360px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
}

.quote-display button {
  border: none;
  background: transparent;
  color: #8c8c8c;
  cursor: pointer;
  font-size: 16px;
}

.input-row {
  display: flex;
  align-items: stretch;
  gap: 12px;
  width: 100%;
}

.input-row input {
  flex: 1 1 auto;
  min-width: 0;
  width: 100%;
  min-height: 52px;
  padding: 14px 16px;
  border: 1px solid #dee2e6;
  border-radius: 8px;
  font-size: 16px;
  outline: none;
}

.input-row input:focus {
  border-color: #176b62;
  box-shadow: 0 0 0 3px rgba(23, 107, 98, 0.12);
}

.input-row button {
  flex: 0 0 64px;
  min-height: 52px;
  padding: 0 18px;
  border: none;
  border-radius: 8px;
  background: #176b62;
  color: white;
  cursor: pointer;
  transition: opacity 0.2s;
}

.input-row button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.memo-section {
  display: flex;
  flex-direction: column;
  background: #f9fafb;
}

.memo-header {
  padding: 16px 18px;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  font-weight: 700;
}

.memo-header small {
  color: #8c8c8c;
  font-weight: 400;
}

.memo-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.memo-empty {
  padding: 14px;
  border: 1px dashed #d9d9d9;
  border-radius: 8px;
  color: #8c8c8c;
  font-size: 14px;
  line-height: 1.6;
  background: #fff;
}

.memo-card {
  margin-bottom: 16px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.03);
  overflow: hidden;
}

.memo-card-top {
  padding: 10px 12px;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  color: #8c8c8c;
  font-size: 12px;
}

.memo-card-top button {
  border: none;
  background: transparent;
  color: #bfbfbf;
  cursor: pointer;
  font-size: 16px;
}

.memo-card blockquote {
  margin: 0;
  padding: 13px 14px;
  border-left: 4px solid #faad14;
  background: #fffbe6;
  color: #595959;
  font-size: 14px;
  line-height: 1.6;
}

.memo-card textarea {
  width: 100%;
  border: none;
  resize: vertical;
  min-height: 76px;
  padding: 12px 14px;
  box-sizing: border-box;
  color: #262626;
  font: inherit;
  font-size: 13px;
  line-height: 1.6;
  outline: none;
}

.memo-card textarea:focus {
  background: #fffcf5;
}

.confirm-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 8px 0 16px 44px;
  font-size: 14px;
}

.confirm-bar.confirmed {
  color: #2f855a;
}

.confirm-hint {
  color: #4a5568;
}

.confirm-btn,
.reject-btn {
  padding: 8px 12px;
  border-radius: 8px;
  border: none;
  cursor: pointer;
}

.confirm-btn {
  background: #48bb78;
  color: white;
}

.reject-btn {
  background: #edf2f7;
  color: #2d3748;
}

.dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  z-index: 3000;
}

.dialog-box {
  width: min(480px, 100%);
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.18);
}

.dialog-box h3 {
  margin-top: 0;
}

.dialog-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 14px;
}

.dialog-field input,
.dialog-field select {
  padding: 10px 12px;
  border: 1px solid #d2d6dc;
  border-radius: 8px;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
}

@media (max-width: 980px) {
  .interpretation-layout {
    grid-template-columns: 1fr;
  }

  .memo-section {
    min-height: 320px;
  }
}

@media (max-width: 768px) {
  .chat-window,
  .insights-page {
    padding: 12px;
  }

  .messages-area {
    padding: 12px;
  }

  .input-row {
    flex-wrap: wrap;
  }

  .input-row button {
    height: 44px;
  }

  .confirm-bar {
    margin-left: 0;
    flex-wrap: wrap;
  }
}
</style>
