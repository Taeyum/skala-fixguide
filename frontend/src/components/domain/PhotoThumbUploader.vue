<script setup>
/**
 * 제품 사진 썸네일 업로더 (buildver2 · PhotoThumbUploader)
 * - 선택된 파일은 부모가 관리(요청 초안 생성 후 업로드)
 * - v-model:files = File[]
 */
import { computed } from 'vue'

const props = defineProps({
  files: { type: Array, default: () => [] },
  max: { type: Number, default: 5 },
  uploaded: { type: Array, default: () => [] }, // 서버 저장된 { id, url }
})
const emit = defineEmits(['update:files'])

const previews = computed(() => props.files.map((f) => ({ name: f.name, url: URL.createObjectURL(f) })))

function onPick(e) {
  const picked = Array.from(e.target.files ?? [])
  const next = [...props.files, ...picked].slice(0, props.max)
  emit('update:files', next)
  e.target.value = ''
}

function removeAt(i) {
  const next = [...props.files]
  next.splice(i, 1)
  emit('update:files', next)
}
</script>

<template>
  <div class="uploader">
    <label class="uploader__drop">
      <input type="file" accept="image/*" multiple hidden @change="onPick" />
      <span class="material-symbols-outlined">add_a_photo</span>
      <span class="uploader__hint">클릭하여 사진 첨부 (PNG, JPG · 최대 {{ max }}장)</span>
    </label>

    <div v-if="uploaded.length || previews.length" class="uploader__grid">
      <div v-for="p in uploaded" :key="p.id" class="uploader__thumb">
        <img :src="p.url" alt="업로드된 사진" />
      </div>
      <div v-for="(p, i) in previews" :key="p.url" class="uploader__thumb">
        <img :src="p.url" :alt="p.name" />
        <button type="button" class="uploader__remove" @click="removeAt(i)">
          <span class="material-symbols-outlined">close</span>
        </button>
      </div>
    </div>

    <p class="uploader__count">{{ uploaded.length + files.length }}장 선택됨</p>
  </div>
</template>

<style scoped>
.uploader {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.uploader__drop {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 28px;
  border: 2px dashed var(--outline-variant);
  border-radius: var(--radius-md);
  background: var(--surface-container-lowest);
  cursor: pointer;
  text-align: center;
}

.uploader__drop:hover {
  border-color: var(--primary);
}

.uploader__drop .material-symbols-outlined {
  font-size: 26px;
  color: var(--primary);
}

.uploader__hint {
  font-size: 12px;
  color: var(--on-surface-variant);
}

.uploader__grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.uploader__thumb {
  position: relative;
  width: 84px;
  height: 84px;
  border-radius: var(--radius-sm);
  overflow: hidden;
  background: var(--surface-container);
}

.uploader__thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.uploader__remove {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 20px;
  height: 20px;
  border: none;
  border-radius: 50%;
  background: rgba(3, 20, 39, 0.7);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.uploader__remove .material-symbols-outlined {
  font-size: 14px;
}

.uploader__count {
  font-size: 12px;
  color: var(--primary);
  font-weight: 500;
}
</style>
