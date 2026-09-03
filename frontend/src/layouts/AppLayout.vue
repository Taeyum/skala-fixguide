<script setup>
/**
 * 앱 공통 레이아웃: 좌측 GNB + Dark Chrome 헤더 + Light Workspace 본문
 * (buildver2 · 3. AppLayout / 목업 stitch_argus_ui)
 */
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'

const auth = useAuthStore()
const router = useRouter()

const engineerNav = [
  { to: '/home', label: '홈', icon: 'home' },
  { to: '/requests/new', label: '신규 요청', icon: 'add_circle' },
  { to: '/my/requests', label: '내 요청', icon: 'assignment' },
]
const safetyNav = [{ to: '/manage/requests', label: '요청 관리', icon: 'admin_panel_settings' }]

const nav = computed(() => (auth.role === 'safety' ? safetyNav : engineerNav))
const roleLabel = computed(() => (auth.role === 'safety' ? '안전관리자' : '엔지니어'))

function onLogout() {
  auth.logout()
  router.push({ name: 'login' })
}
</script>

<template>
  <div class="app-shell">
    <aside class="app-nav">
      <div class="app-nav__brand">
        <span class="material-symbols-outlined">precision_manufacturing</span>
        <span>ARGUS APM</span>
      </div>

      <nav class="app-nav__menu">
        <RouterLink v-for="item in nav" :key="item.to" :to="item.to" class="app-nav__item">
          <span class="material-symbols-outlined">{{ item.icon }}</span>
          {{ item.label }}
        </RouterLink>
      </nav>

      <div class="app-nav__user">
        <div class="app-nav__avatar">
          <span class="material-symbols-outlined">person</span>
        </div>
        <div class="app-nav__meta">
          <p class="app-nav__name">{{ auth.user?.name || auth.user?.email || '사용자' }}</p>
          <p class="app-nav__role">{{ roleLabel }}</p>
        </div>
        <button type="button" class="app-nav__logout" title="로그아웃" @click="onLogout">
          <span class="material-symbols-outlined">logout</span>
        </button>
      </div>
    </aside>

    <div class="app-main">
      <header class="app-chrome">
        <div class="app-chrome__title">
          <span class="material-symbols-outlined">shield</span>
          <span>Fab 안전 포털</span>
        </div>
      </header>

      <main class="app-workspace">
        <slot />
      </main>
    </div>
  </div>
</template>

<style scoped>
.app-shell {
  display: flex;
  min-height: 100vh;
}

.app-nav {
  position: fixed;
  inset: 0 auto 0 0;
  width: 240px;
  background: var(--surface-container-low);
  border-right: 1px solid var(--outline-variant);
  display: flex;
  flex-direction: column;
  padding: 20px 0;
  z-index: 40;
}

.app-nav__brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 20px 20px;
  font-size: 15px;
  font-weight: 600;
  color: var(--on-surface);
}

.app-nav__brand .material-symbols-outlined {
  color: var(--primary);
  font-variation-settings: 'FILL' 1;
}

.app-nav__menu {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px 12px;
}

.app-nav__item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 11px 14px;
  border-radius: var(--radius-md);
  color: var(--on-surface-variant);
  font-size: 14px;
  font-weight: 500;
  text-decoration: none;
}

.app-nav__item:hover {
  background: var(--surface-container-high);
  color: var(--on-surface);
  text-decoration: none;
}

.app-nav__item.router-link-active {
  background: var(--primary);
  color: var(--on-primary);
}

.app-nav__item .material-symbols-outlined {
  font-size: 20px;
}

.app-nav__user {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 12px;
  padding-top: 16px;
  border-top: 1px solid var(--outline-variant);
}

.app-nav__avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--primary);
  color: var(--on-primary);
  display: flex;
  align-items: center;
  justify-content: center;
}

.app-nav__avatar .material-symbols-outlined {
  font-size: 18px;
}

.app-nav__meta {
  flex: 1;
  min-width: 0;
}

.app-nav__name {
  font-size: 13px;
  font-weight: 600;
  color: var(--on-surface);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-nav__role {
  font-size: 11px;
  color: var(--on-surface-variant);
}

.app-nav__logout {
  border: none;
  background: transparent;
  color: var(--on-surface-variant);
  display: flex;
  padding: 6px;
  border-radius: var(--radius-sm);
}

.app-nav__logout:hover {
  background: var(--surface-container-high);
  color: var(--error);
}

.app-main {
  flex: 1;
  margin-left: 240px;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.app-chrome {
  position: sticky;
  top: 0;
  height: 56px;
  background: var(--chrome-bg);
  color: var(--chrome-fg);
  display: flex;
  align-items: center;
  padding: 0 24px;
  z-index: 30;
}

.app-chrome__title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 500;
  opacity: 0.9;
}

.app-chrome__title .material-symbols-outlined {
  font-size: 18px;
}

.app-workspace {
  flex: 1;
  padding: 32px;
  background: var(--surface);
}

@media (max-width: 720px) {
  .app-nav {
    width: 64px;
  }
  .app-nav__brand span:last-child,
  .app-nav__item,
  .app-nav__meta,
  .app-nav__logout {
    display: none;
  }
  .app-main {
    margin-left: 64px;
  }
  .app-workspace {
    padding: 20px;
  }
}
</style>
