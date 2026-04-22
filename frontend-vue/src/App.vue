<template>
  <div id="app" class="app-container">
    <header v-if="showHeader" class="header">
      <div class="header-inner">
        <div></div>
        <h1>MedLabAgent - Medical AI System</h1>
        <div v-if="currentUser" class="header-user">
          <span>{{ currentUser.realName }}</span>
          <button @click="handleLogout">退出</button>
        </div>
      </div>
    </header>
    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "./stores/authStore";
import { useChatStore } from "./stores/chatStore";

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const chatStore = useChatStore();

const showHeader = computed(() => route.name !== "Login");
const currentUser = computed(() => authStore.user);

async function handleLogout() {
  await authStore.logout();
  chatStore.clearMessages();
  router.push("/login");
}
</script>

<style scoped>
.app-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

.header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 0 28px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
}

.header-inner {
  height: 76px;
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 18px;
}

.header h1 {
  font-size: 1.75rem;
  font-weight: 700;
  margin: 0;
  text-align: center;
}

.header-user {
  justify-self: end;
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 14px;
}

.header-user button {
  padding: 8px 14px;
  border: none;
  border-radius: 6px;
  color: white;
  background: rgba(255, 255, 255, 0.2);
  cursor: pointer;
}

.header-user button:hover {
  background: rgba(255, 255, 255, 0.3);
}

.main-content {
  flex: 1;
  display: flex;
  overflow: visible;
  min-height: 0;
  width: 100%;
}

@media (max-width: 760px) {
  .header-inner {
    grid-template-columns: 1fr;
    height: auto;
    padding: 14px 0;
  }

  .header-user {
    justify-self: center;
  }
}
</style>
