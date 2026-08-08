<template>
  <div class="poster-bg-form">
    <el-form-item :label="typeLabel">
      <el-radio-group :value="bgType" @input="set(typeKey, $event)">
        <el-radio label="theme">主题渐变</el-radio>
        <el-radio label="color">纯色</el-radio>
        <el-radio label="image">自定义图</el-radio>
      </el-radio-group>
    </el-form-item>
    <el-form-item v-if="bgType === 'color'" label="背景色">
      <div class="theme-row">
        <span
          v-for="c in colorPresets"
          :key="c"
          class="swatch"
          :style="{ background: c }"
          :class="{ active: bgColor === c }"
          @click="set(colorKey, c)"
        />
        <el-color-picker
          :value="bgColor || '#eef2ff'"
          @input="set(colorKey, $event)"
        />
      </div>
    </el-form-item>
    <template v-if="bgType === 'image'">
      <el-form-item label="背景图">
        <image-upload
          :value="bgImage || ''"
          :limit="1"
          :file-size="3"
          @input="set(imageKey, $event)"
        />
      </el-form-item>
      <el-form-item label="遮罩强度">
        <el-slider
          :value="overlayValue"
          :min="0"
          :max="90"
          :step="5"
          show-input
          @input="set(overlayKey, $event)"
        />
        <p class="tip">数值越高文字越清晰，建议 30–50</p>
      </el-form-item>
    </template>
    <p v-if="hint" class="tip hint-line">{{ hint }}</p>
  </div>
</template>

<script>
export default {
  name: 'PosterBgForm',
  props: {
    model: {
      type: Object,
      required: true
    },
    /** Field prefix: posterBg → posterBgType; pageBg → pageBgType */
    prefix: {
      type: String,
      default: 'posterBg'
    },
    typeLabel: {
      type: String,
      default: '海报背景'
    },
    hint: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      colorPresets: ['#eef2ff', '#ecfdf5', '#fff7ed', '#f8fafc', '#e0e7ff', '#fce7f3']
    }
  },
  computed: {
    typeKey() { return this.prefix + 'Type' },
    colorKey() { return this.prefix + 'Color' },
    imageKey() { return this.prefix + 'Image' },
    overlayKey() { return this.prefix + 'Overlay' },
    bgType() {
      return this.model[this.typeKey] || 'theme'
    },
    bgColor() {
      return this.model[this.colorKey]
    },
    bgImage() {
      return this.model[this.imageKey]
    },
    overlayValue() {
      const n = Number(this.model[this.overlayKey])
      return Number.isNaN(n) ? 40 : n
    }
  },
  methods: {
    set(key, value) {
      this.$set(this.model, key, value)
      this.$emit('change', { ...this.model })
    }
  }
}
</script>

<style scoped>
.theme-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.swatch {
  width: 22px;
  height: 22px;
  border-radius: 4px;
  border: 1px solid rgba(15, 23, 42, 0.12);
  cursor: pointer;
  box-sizing: border-box;
}
.swatch.active {
  outline: 2px solid #1677ff;
  outline-offset: 1px;
}
.tip {
  margin: 4px 0 0;
  color: #909399;
  font-size: 12px;
  line-height: 1.4;
}
.hint-line {
  margin: 0 0 8px;
}
</style>
