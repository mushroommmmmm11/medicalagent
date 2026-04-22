<template>
  <div class="chat-message" :class="message.role">
    <div class="avatar" v-if="message.role === 'assistant'">🤖</div>
    <div class="message-body">
      <div class="message-content" v-html="renderedContent"></div>
      <div class="timestamp">{{ formatTime(message.timestamp) }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed } from "vue";
import MarkdownIt from "markdown-it";
import DOMPurify from "dompurify";

const props = defineProps({
  message: {
    type: Object,
    required: true,
  },
});

const md = new MarkdownIt({
  html: false,
  linkify: true,
  typographer: true,
  breaks: true, // 允许换行
});

const renderedContent = computed(() => {
  const raw = props.message.content || "";
  if (!raw.trim()) return "";

  const html = md.render(raw);

  // 配置 DOMPurify 允许markdown相关的HTML标签
  const config = {
    ALLOWED_TAGS: [
      "h1",
      "h2",
      "h3",
      "h4",
      "h5",
      "h6",
      "p",
      "br",
      "strong",
      "b",
      "em",
      "i",
      "ul",
      "ol",
      "li",
      "blockquote",
      "code",
      "pre",
      "a",
      "table",
      "thead",
      "tbody",
      "tr",
      "th",
      "td",
      "span",
      "div",
    ],
    ALLOWED_ATTR: ["href", "title", "class"],
  };

  const sanitized = DOMPurify.sanitize(html, config);

  // 调试：检查是否有HTML标签在输出中
  if (raw.includes("#") && !sanitized.includes("<h")) {
    console.warn("⚠️ Markdown heading render may have issues");
  }

  return sanitized;
});

function formatTime(date) {
  if (!date) return "";
  try {
    return new Date(date).toLocaleTimeString("zh-CN", {
      hour: "2-digit",
      minute: "2-digit",
    });
  } catch (e) {
    return "";
  }
}
</script>

<style scoped>
.chat-message {
  display: flex;
  margin-bottom: 1rem;
  align-items: flex-start;
  width: 100%;
}

.chat-message.user {
  justify-content: flex-end;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 0.5rem;
  font-size: 1.5rem;
  flex: 0 0 32px;
  margin-top: 2px;
}

.message-body {
  display: flex;
  flex-direction: column;
  min-width: 0;
  max-width: min(860px, 82%);
}

.chat-message.user .message-body {
  max-width: min(720px, 72%);
}

.message-content {
  width: fit-content;
  max-width: 100%;
  padding: 0.9rem 1.1rem;
  border-radius: 8px;
  overflow-wrap: anywhere;
  word-break: break-word;
  white-space: normal;
  line-height: 1.7;
  font-size: 15px;
  box-sizing: border-box;
}

/* Markdown 样式支持 */
.message-content :deep(h1),
.message-content :deep(h2),
.message-content :deep(h3),
.message-content :deep(h4),
.message-content :deep(h5),
.message-content :deep(h6) {
  margin: 0.35rem 0 0.7rem;
  font-weight: bold;
  line-height: 1.3;
}

.message-content :deep(h1) {
  font-size: 1.5rem;
  border-bottom: 2px solid;
  padding-bottom: 0.3rem;
}

.message-content :deep(h2) {
  font-size: 1.3rem;
}

.message-content :deep(h3) {
  font-size: 1.1rem;
}

.message-content :deep(p) {
  margin: 0 0 0.65rem;
}

.message-content :deep(p:last-child),
.message-content :deep(ul:last-child),
.message-content :deep(ol:last-child) {
  margin-bottom: 0;
}

.message-content :deep(code) {
  background-color: rgba(0, 0, 0, 0.05);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: "Courier New", monospace;
  font-size: 0.9em;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.message-content :deep(pre) {
  background-color: rgba(0, 0, 0, 0.08);
  padding: 1rem;
  border-radius: 6px;
  max-width: 100%;
  overflow-x: auto;
  border-left: 3px solid;
  font-family: "Courier New", monospace;
  font-size: 0.85em;
  line-height: 1.4;
  box-sizing: border-box;
}

.message-content :deep(pre code) {
  background: none;
  padding: 0;
  border-radius: 0;
  white-space: pre;
  overflow-wrap: normal;
}

.message-content :deep(ul),
.message-content :deep(ol) {
  margin: 0.45rem 0 0.7rem;
  padding-left: 1.25rem;
}

.message-content :deep(li) {
  margin: 0.25rem 0;
  padding-left: 0.15rem;
}

.message-content :deep(blockquote) {
  border-left: 4px solid;
  margin: 0.5rem 0;
  padding: 0 0.5rem 0 1rem;
  opacity: 0.8;
  font-style: italic;
}

.message-content :deep(table) {
  border-collapse: collapse;
  margin: 0.5rem 0;
  font-size: 0.9em;
  display: block;
  max-width: 100%;
  overflow-x: auto;
}

.message-content :deep(table th),
.message-content :deep(table td) {
  border: 1px solid rgba(0, 0, 0, 0.1);
  padding: 0.5rem;
  text-align: left;
}

.message-content :deep(table th) {
  background-color: rgba(0, 0, 0, 0.05);
  font-weight: bold;
}

.message-content :deep(a) {
  text-decoration: underline;
  cursor: pointer;
  color: inherit;
  opacity: 0.9;
}

.chat-message.user .message-content {
  background-color: #667eea;
  color: white;
}

.chat-message.assistant .message-content {
  background-color: #f2f3f5;
  color: #252a31;
  box-shadow: 0 4px 14px rgba(20, 27, 40, 0.06);
}

.timestamp {
  font-size: 0.75rem;
  color: #999;
  margin-top: 0.3rem;
  padding: 0 0.8rem;
}
</style>
