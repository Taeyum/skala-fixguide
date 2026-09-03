<script setup>
/** WRA_C_00 · 로그인 (/login) */
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import BaseButton from '@/components/common/BaseButton.vue'
import InlineError from '@/components/common/InlineError.vue'
import { apiErrorMessage } from '@/api/errorMessage'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const form = reactive({ email: '', password: '' })
const loading = ref(false)
const errorMessage = ref('')

/** 데모용 역할 프리필 (실제 역할은 로그인 응답에서 결정) */
function prefill(role) {
  form.email = role === 'safety' ? 'safety.lee@argus.fab' : 'engineer.kim@argus.fab'
}

async function onSubmit() {
  errorMessage.value = ''
  loading.value = true
  try {
    const user = await auth.login({ email: form.email, password: form.password })
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : null
    router.push(redirect ?? auth.homeRoute(user.role))
  } catch (err) {
    errorMessage.value =
      err.response?.status === 401
        ? '이메일 또는 비밀번호가 올바르지 않습니다.'
        : apiErrorMessage(err, '로그인 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-card">
    <div class="auth-card__brand">
      <div class="auth-card__logo">
        <span class="material-symbols-outlined">precision_manufacturing</span>
      </div>
      <span class="auth-card__eyebrow">Argus Semiconductor Fab</span>
      <h1 class="auth-card__title">부품 교체 요청·승인 시스템</h1>
      <p class="auth-card__subtitle">안전하고 신속한 팹 장비 유지보수 워크플로우 포털</p>
    </div>

    <form class="auth-form" novalidate @submit.prevent="onSubmit">
      <InlineError :message="errorMessage" />

      <div class="field">
        <label for="email">사내 이메일 / ID</label>
        <div class="field__control">
          <span class="material-symbols-outlined">badge</span>
          <input
            id="email"
            v-model.trim="form.email"
            type="text"
            autocomplete="username"
            placeholder="engineer.kim@argus.fab"
            :class="{ 'is-invalid': errorMessage }"
            required
          />
        </div>
      </div>

      <div class="field">
        <label for="password">비밀번호</label>
        <div class="field__control">
          <span class="material-symbols-outlined">lock</span>
          <input
            id="password"
            v-model="form.password"
            type="password"
            autocomplete="current-password"
            placeholder="••••••••"
            :class="{ 'is-invalid': errorMessage }"
            required
          />
        </div>
      </div>

      <div class="field">
        <label>로그인 역할 선택 (데모용)</label>
        <div class="quick-role">
          <button type="button" @click="prefill('engineer')">
            <span class="material-symbols-outlined">engineering</span> 엔지니어
          </button>
          <button type="button" @click="prefill('safety')">
            <span class="material-symbols-outlined">verified_user</span> 안전관리자
          </button>
        </div>
      </div>

      <BaseButton type="submit" variant="primary" block :loading="loading">
        <span>시스템 접속</span>
        <span class="material-symbols-outlined">arrow_forward</span>
      </BaseButton>
    </form>

    <div class="auth-card__divider" />

    <p class="auth-card__foot">
      계정이 없으신가요?
      <RouterLink to="/signup">신규 사원 등록 요청</RouterLink>
    </p>
  </div>
</template>

<style scoped>
.quick-role {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.quick-role button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px;
  border: 1px solid var(--outline-variant);
  border-radius: var(--radius-sm);
  background: var(--surface-container-low);
  color: var(--on-surface-variant);
  font-size: 13px;
}

.quick-role button:hover {
  background: var(--surface-container);
}

.quick-role .material-symbols-outlined {
  font-size: 18px;
}
</style>
