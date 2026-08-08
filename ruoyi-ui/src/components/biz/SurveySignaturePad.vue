<template>
  <div class="survey-sign-pad">
    <div v-if="previewUrl" class="sign-done">
      <img :src="previewUrl" alt="signature" />
      <div class="sign-actions">
        <el-button size="mini" type="text" @click="clearSigned">{{ labels.resign }}</el-button>
      </div>
    </div>
    <template v-else>
      <div class="pad-wrap" :style="{ height: padHeight + 'px' }">
        <canvas
          ref="canvas"
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
        <el-button size="mini" @click="clearPad">{{ labels.clear }}</el-button>
        <el-button size="mini" type="primary" :loading="uploading" :disabled="!hasInk" @click="confirm">{{ labels.confirm }}</el-button>
      </div>
      <p v-if="hint" class="hint">{{ hint }}</p>
    </template>
  </div>
</template>

<script>
export default {
  name: 'SurveySignaturePad',
  props: {
    value: { default: '' },
    penColor: { type: String, default: '#111111' },
    padHeight: { type: Number, default: 160 },
    /** When set, confirm uploads PNG via multipart to this URL */
    uploadUrl: { type: String, default: '' },
    uploadData: { type: Object, default: () => ({}) },
    /** preview: keep dataURL without upload */
    mode: { type: String, default: 'open' }
  },
  data() {
    return {
      drawing: false,
      hasInk: false,
      uploading: false,
      localPreview: '',
      labels: {
        clear: '\u6e05\u9664',
        confirm: '\u786e\u8ba4\u7b7e\u540d',
        resign: '\u91cd\u7b7e',
        uploadFail: '\u4e0a\u4f20\u5931\u8d25',
        signUploadFail: '\u7b7e\u540d\u4e0a\u4f20\u5931\u8d25'
      }
    }
  },
  computed: {
    previewUrl() {
      if (this.localPreview) return this.localPreview
      const v = this.value
      if (!v) return ''
      if (typeof v === 'string' && v.startsWith('data:')) return v
      if (typeof v === 'object' && v.fileName) {
        const path = v.url || v.fileName
        if (String(path).startsWith('data:')) return path
        return process.env.VUE_APP_BASE_API + path
      }
      return ''
    }
  },
  mounted() {
    this.$nextTick(() => this.resizeCanvas())
    window.addEventListener('resize', this.resizeCanvas)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.resizeCanvas)
  },
  methods: {
    resizeCanvas() {
      const canvas = this.$refs.canvas
      if (!canvas) return
      const parent = canvas.parentElement
      const w = parent ? parent.clientWidth : 320
      const h = this.padHeight || 160
      const ratio = window.devicePixelRatio || 1
      canvas.width = Math.floor(w * ratio)
      canvas.height = Math.floor(h * ratio)
      canvas.style.width = w + 'px'
      canvas.style.height = h + 'px'
      const ctx = canvas.getContext('2d')
      ctx.setTransform(ratio, 0, 0, ratio, 0, 0)
      ctx.lineCap = 'round'
      ctx.lineJoin = 'round'
      ctx.strokeStyle = this.penColor
      ctx.lineWidth = 2.2
      this.hasInk = false
    },
    pos(e) {
      const canvas = this.$refs.canvas
      const rect = canvas.getBoundingClientRect()
      return { x: e.clientX - rect.left, y: e.clientY - rect.top }
    },
    start(e) {
      const canvas = this.$refs.canvas
      if (!canvas) return
      this.drawing = true
      const ctx = canvas.getContext('2d')
      const p = this.pos(e)
      ctx.beginPath()
      ctx.moveTo(p.x, p.y)
    },
    move(e) {
      if (!this.drawing) return
      const canvas = this.$refs.canvas
      const ctx = canvas.getContext('2d')
      const p = this.pos(e)
      ctx.lineTo(p.x, p.y)
      ctx.stroke()
      this.hasInk = true
    },
    end() {
      this.drawing = false
    },
    startTouch(e) {
      const t = e.touches[0]
      if (!t) return
      this.start({ clientX: t.clientX, clientY: t.clientY })
    },
    moveTouch(e) {
      const t = e.touches[0]
      if (!t) return
      this.move({ clientX: t.clientX, clientY: t.clientY })
    },
    clearPad() {
      this.resizeCanvas()
    },
    clearSigned() {
      this.localPreview = ''
      this.$emit('input', '')
      this.$emit('change', '')
      this.$nextTick(() => this.resizeCanvas())
    },
    canvasToBlob() {
      return new Promise((resolve, reject) => {
        const canvas = this.$refs.canvas
        if (!canvas) return reject(new Error('no canvas'))
        if (canvas.toBlob) {
          canvas.toBlob(b => (b ? resolve(b) : reject(new Error('empty'))), 'image/png')
        } else {
          const data = canvas.toDataURL('image/png')
          const arr = data.split(',')
          const mime = arr[0].match(/:(.*?);/)[1]
          const bstr = atob(arr[1])
          let n = bstr.length
          const u8 = new Uint8Array(n)
          while (n--) u8[n] = bstr.charCodeAt(n)
          resolve(new Blob([u8], { type: mime }))
        }
      })
    },
    async confirm() {
      if (!this.hasInk) return
      if (this.mode === 'preview' || !this.uploadUrl) {
        const dataUrl = this.$refs.canvas.toDataURL('image/png')
        const payload = { fileName: dataUrl, url: dataUrl, originalFilename: 'signature.png' }
        this.localPreview = dataUrl
        this.$emit('input', payload)
        this.$emit('change', payload)
        return
      }
      this.uploading = true
      try {
        const blob = await this.canvasToBlob()
        const form = new FormData()
        form.append('file', blob, 'signature.png')
        Object.keys(this.uploadData || {}).forEach(k => {
          if (this.uploadData[k] != null && this.uploadData[k] !== '') form.append(k, this.uploadData[k])
        })
        const res = await fetch(this.uploadUrl, { method: 'POST', body: form })
        const json = await res.json()
        if (!json || json.code !== 200) {
          throw new Error((json && json.msg) || this.labels.uploadFail)
        }
        const payload = {
          fileName: json.fileName || json.data && json.data.fileName,
          url: (json.url || (json.data && json.data.url) || json.fileName),
          originalFilename: json.originalFilename || 'signature.png'
        }
        if (json.data && json.data.fileName) {
          payload.fileName = json.data.fileName
          payload.url = json.data.url || json.data.fileName
          payload.originalFilename = json.data.originalFilename || 'signature.png'
        }
        this.localPreview = process.env.VUE_APP_BASE_API + payload.fileName
        this.$emit('input', payload)
        this.$emit('change', payload)
      } catch (e) {
        this.$message && this.$message.error(e.message || this.labels.signUploadFail)
        this.$emit('error', e)
      } finally {
        this.uploading = false
      }
    }
  }
}
</script>

<style scoped>
.survey-sign-pad { width: 100%; }
.pad-wrap {
  border: 1px dashed #94a3b8;
  border-radius: 10px;
  background: #fff;
  overflow: hidden;
  touch-action: none;
}
.pad-canvas { display: block; width: 100%; height: 100%; cursor: crosshair; }
.sign-done img {
  display: block; width: 100%; max-height: 200px; object-fit: contain;
  border: 1px solid #e5e7eb; border-radius: 10px; background: #fff;
}
.sign-actions { margin-top: 8px; display: flex; gap: 8px; align-items: center; }
.hint { margin: 6px 0 0; font-size: 12px; color: #94a3b8; }
</style>