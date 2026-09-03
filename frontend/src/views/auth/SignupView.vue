<script setup>
/** WRA_C_01 · 회원가입 (/signup) */
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import BaseButton from '@/components/common/BaseButton.vue'
import InlineError from '@/components/common/InlineError.vue'
import SegmentedToggle from '@/components/common/SegmentedToggle.vue'

const router = useRouter()
const auth = useAuthStore()

const roleOptions = [
  { value: 'engineer', label: '엔지니어', icon: 'engineering' },
  { value: 'safety', label: '안전관리자', icon: 'verified_user' },
]

const form = reactive({
  name: '',
  email: '',
  password: '',
  passwordConfirm: '',
  role: null,
  agreed: false,
})

const loading = ref(false)
const errorMessage = ref('')
const submitted = ref(false)

const passwordMismatch = computed(
  () => !!form.passwordConfirm && form.password !== form.passwordConfirm,
)

const canSubmit = computed(
  () =>
    form.name &&
    form.email &&
    form.password.length >= 8 &&
    !passwordMismatch.value &&
    form.role &&
    form.agreed,
)

async function onSubmit() {
  submitted.value = true
  errorMessage.value = ''
  if (!canSubmit.value) {
    if (passwordMismatch.value) {
      errorMessage.value = '비밀번호와 비밀번호 확인이 일치하지 않습니다.'
    } else {
      errorMessage.value = '필수 항목과 담당 역할을 모두 입력해 주세요.'
    }
    return
  }

  loading.value = true
  try {
    await auth.signup({
      name: form.name,
      email: form.email,
      password: form.password,
      passwordConfirm: form.passwordConfirm,
      role: form.role,
    })
    router.push({ path: '/login', query: { registered: '1' } })
  } catch (err) {
    if (err.response?.status === 409) {
      errorMessage.value = '이미 가입된 이메일입니다. 로그인해 주세요.'
    } else {
      errorMessage.value = '회원가입 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.'
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-card">
    <div class="auth-card__brand">
      <div class="auth-card__logo">
        <span class="material-symbols-outlined">shield_person</span>
      </div>
      <span class="auth-card__eyebrow">Argus Fab Portal</span>
      <h1 class="auth-card__title">계정 생성</h1>
      <p class="auth-card__subtitle">반도체 제조 공정 접근 권한을 위한 신규 등록</p>
    </div>

    <form class="auth-form" novalidate @submit.prevent="onSubmit">
      <InlineError :message="errorMessage" />

      <div class="field">
        <label>담당 역할 선택</label>
        <SegmentedToggle v-model="form.role" :options="roleOptions" />
        <span v-if="submitted && !form.role" class="field__hint field__hint--error">
          담당 역할을 선택해 주세요.
        </span>
      </div>

      <div class="field">
        <label for="name">성명</label>
        <div class="field__control">
          <span class="material-symbols-outlined">badge</span>
          <input
            id="name"
            v-model.trim="form.name"
            type="text"
            autocomplete="name"
            placeholder="홍길동"
            :class="{ 'is-invalid': submitted && !form.name }"
            required
          />
        </div>
      </div>

      <div class="field">
        <label for="email">사내 이메일</label>
        <div class="field__control">
          <span class="material-symbols-outlined">mail</span>
          <input
            id="email"
            v-model.trim="form.email"
            type="email"
            autocomplete="email"
            placeholder="employee@argus-fab.com"
            :class="{ 'is-invalid': submitted && !form.email }"
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
            autocomplete="new-password"
            placeholder="영문·숫자·특수문자 조합 8자 이상"
            :class="{ 'is-invalid': submitted && form.password.length < 8 }"
            required
          />
        </div>
        <span v-if="submitted && form.password.length < 8" class="field__hint field__hint--error">
          비밀번호는 8자 이상이어야 합니다.
        </span>
      </div>

      <div class="field">
        <label for="passwordConfirm">비밀번호 확인</label>
        <div class="field__control">
          <span class="material-symbols-outlined">lock_reset</span>
          <input
            id="passwordConfirm"
            v-model="form.passwordConfirm"
            type="password"
            autocomplete="new-password"
            placeholder="비밀번호 재입력"
            :class="{ 'is-invalid': passwordMismatch }"
            required
          />
        </div>
        <span v-if="passwordMismatch" class="field__hint field__hint--error">
          비밀번호가 일치하지 않습니다.
        </span>
      </div>

      <label class="terms">
        <input v-model="form.agreed" type="checkbox" />
        <span><b>보안 서약서</b> 및 <b>개인정보 처리방침</b>에 동의합니다.</span>
      </label>

      <BaseButton type="submit" variant="primary" block :loading="loading">
        <span>가입하기</span>
        <span class="material-symbols-outlined">arrow_forward</span>
      </BaseButton>
    </form>

    <div class="auth-card__divider" />

    <p class="auth-card__foot">
      이미 계정이 있으신가요?
      <RouterLink to="/login">로그인하기</RouterLink>
    </p>
  </div>
</template>

<style scoped>
.terms {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 12px;
  color: var(--on-surface-variant);
}

.terms input {
  margin-top: 1px;
  accent-color: var(--primary);
}

.terms b {
  color: var(--primary);
  font-weight: 600;
}
</style>
