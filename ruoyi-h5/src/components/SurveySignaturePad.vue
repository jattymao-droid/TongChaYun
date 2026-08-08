<template>
  <div class="h5-sign-pad">
    <div v-if="previewUrl" class="sign-done">
      <img :src="previewUrl" alt="signature" />
      <button type="button" class="btn link" @click="clearSigned">{{ labels.resign }}</button>
    </div>
    <template v-else>
      <div class="pad-wrap" :style="{ height: padHeight + 'px' }">
        <canvas
          ref="canvasRef"
          class="pad-canvas"
          @mousedown="start"
          @mousemove="move"
          @mouseup="end"
          @mouseleave="end"
          @touchstart.prevent="startTouch"
          @touchmove.prevent="moveTouch"
          @touchend.prevent="end"
        />
      </div>
      <div class="sign-actions">
        <button type="button" class="btn" @click="clearPad">{{ labels.clear }}</button>
        <button type="button" class="btn primary" :disabled="!hasInk || uploading" @click="confirm">
          {{ uploading ? labels.uploading : labels.confirm }}
        </button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'

const labels = {
  clear: '\u6e05\u9664',
  confirm: '\u786e\u8ba4\u7b7e\u540d',
  resign: '\u91cd\u7b7e',
  uploading: '\u4e0a\u4f20\u4e2d\u2026',
  uploadFail: '\u4e0a\u4f20\u5931\u8d25'
}

const props = defineProps({
  modelValue: { default: '' },
  penColor: { type: String, default: '#111111' },
  padHeight: { type: Number, default: 160 },
  uploadUrl: { type: String, default: '' },
  uploadData: { type: Object, default: () => ({}) },
  apiBase: { type: String, default: '' }
})
const emit = defineEmits(['update:modelValue', 'change', 'error'])

const canvasRef = ref(null)
const drawing = ref(false)
const hasInk = ref(false)
const uploading = ref(false)
const localPreview = ref('')

const previewUrl = computed(() => {
  if (localPreview.value) return localPreview.value
  const v = props.modelValue
  if (!v) return ''
  if (typeof v === 'string' && v.startsWith('data:')) return v
  if (typeof v === 'object' && v.fileName) {
    const path = v.url || v.fileName
    if (String(path).startsWith('data:')) return path
    return (props.apiBase || '') + path
  }
  return ''
})

function resizeCanvas() {
  const canvas = canvasRef.value
  if (!canvas) return
  const parent = canvas.parentElement
  const w = parent ? parent.clientWidth : 320
  const h = props.padHeight || 160
  const ratio = window.devicePixelRatio || 1
  canvas.width = Math.floor(w * ratio)
  canvas.height = Math.floor(h * ratio)
  canvas.style.width = w + 'px'
  canvas.style.height = h + 'px'
  const ctx = canvas.getContext('2d')
  ctx.setTransform(ratio, 0, 0, ratio, 0, 0)
  ctx.lineCap = 'round'
  ctx.lineJoin = 'round'
  ctx.strokeStyle = props.penColor
  ctx.lineWidth = 2.2
  hasInk.value = false
}

function pos(e) {
  const canvas = canvasRef.value
  const rect = canvas.getBoundingClientRect()
  return { x: e.clientX - rect.left, y: e.clientY - rect.top }
}
function start(e) {
  const canvas = canvasRef.value
  if (!canvas) return
  drawing.value = true
  const ctx = canvas.getContext('2d')
  const p = pos(e)
  ctx.beginPath()
  ctx.moveTo(p.x, p.y)
}
function move(e) {
  if (!drawing.value) return
  const canvas = canvasRef.value
  const ctx = canvas.getContext('2d')
  const p = pos(e)
  ctx.lineTo(p.x, p.y)
  ctx.stroke()
  hasInk.value = true
}
function end() { drawing.value = false }
function startTouch(e) {
  const t = e.touches[0]
  if (t) start({ clientX: t.clientX, clientY: t.clientY })
}
function moveTouch(e) {
  const t = e.touches[0]
  if (t) move({ clientX: t.clientX, clientY: t.clientY })
}
function clearPad() { resizeCanvas() }
function clearSigned() {
  localPreview.value = ''
  emit('update:modelValue', '')
  emit('change', '')
  nextTick(() => resizeCanvas())
}

function canvasToBlob() {
  return new Promise((resolve, reject) => {
    const canvas = canvasRef.value
    if (!canvas) return reject(new Error('no canvas'))
    canvas.toBlob(b => (b ? resolve(b) : reject(new Error('empty'))), 'image/png')
  })
}

async function confirm() {
  if (!hasInk.value) return
  if (!props.uploadUrl) {
    const dataUrl = canvasRef.value.toDataURL('image/png')
    const payload = { fileName: dataUrl, url: dataUrl, originalFilename: 'signature.png' }
    localPreview.value = dataUrl
    emit('update:modelValue', payload)
    emit('change', payload)
    return
  }
  uploading.value = true
  try {
    const blob = await canvasToBlob()
    const formData = new FormData()
    formData.append('file', blob, 'signature.png')
    Object.keys(props.uploadData || {}).forEach(k => {
      if (props.uploadData[k] != null && props.uploadData[k] !== '') formData.append(k, props.uploadData[k])
    })
    const res = await fetch(props.uploadUrl, { method: 'POST', body: formData })
    const json = await res.json()
    if (!json || json.code !== 200) throw new Error((json && json.msg) || labels.uploadFail)
    const payload = {
      fileName: json.fileName,
      url: json.url || json.fileName,
      originalFilename: json.originalFilename || 'signature.png'
    }
    localPreview.value = (props.apiBase || '') + payload.fileName
    emit('update:modelValue', payload)
    emit('change', payload)
  } catch (e) {
    emit('error', e)
  } finally {
    uploading.value = false
  }
}

onMounted(() => {
  nextTick(() => resizeCanvas())
  window.addEventListener('resize', resizeCanvas)
})
onUnmounted(() => window.removeEventListener('resize', resizeCanvas))
watch(() => props.padHeight, () => nextTick(() => resizeCanvas()))
</script>

<style scoped>
.h5-sign-pad { width: 100%; }
.pad-wrap {
  border: 1px dashed var(--border, #94a3b8);
  border-radius: 12px;
  background: #fff;
  overflow: hidden;
  touch-action: none;
}
.pad-canvas { display: block; width: 100%; height: 100%; cursor: crosshair; }
.sign-done img {
  display: block; width: 100%; max-height: 180px; object-fit: contain;
  border: 1px solid var(--border, #e5e7eb); border-radius: 12px; background: #fff;
}
.sign-actions { margin-top: 8px; display: flex; gap: 8px; }
.sign-actions .btn { flex: 1; }
.btn.link { border: 0; background: transparent; color: var(--theme); margin-top: 8px; }
</style>