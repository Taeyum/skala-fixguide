<script setup>
/** C_00 / C_01 공통: 중앙 정렬 단일 카드 레이아웃 */
</script>

<template>
  <main class="auth-layout">
    <div class="auth-layout__glow" aria-hidden="true" />
    <div class="auth-layout__content">
      <slot />
    </div>
  </main>
</template>

<style scoped>
.auth-layout {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--gutter-lg);
  overflow: hidden;
}

.auth-layout__glow {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(60% 50% at 15% 0%, var(--primary-fixed) 0%, transparent 60%),
    radial-gradient(50% 50% at 100% 100%, var(--secondary-fixed) 0%, transparent 55%),
    var(--surface);
  opacity: 0.7;
  pointer-events: none;
}

.auth-layout__content {
  position: relative;
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--gutter-lg);
}
</style>
