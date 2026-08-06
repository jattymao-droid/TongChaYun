<template>
  <div class="survey-q-field">
    <div v-if="question.qType === 'section'" class="section-box">
      <h4>{{ question.title }}</h4>
      <p>{{ question._content || '' }}</p>
    </div>
    <template v-else>
      <div class="q-label" v-if="showLabel">
        <span class="idx" v-if="indexLabel != null">{{ indexLabel }}.</span>
        <span>{{ question.title }}</span>
        <el-tag v-if="showTypeTag" size="mini" type="info" class="type-tag">{{ typeLabel(question.qType) }}</el-tag>
        <span class="req" v-if="question.required === '1'">*</span>
      </div>

      <el-radio-group
        v-if="question.qType === 'radio' || question.qType === 'yesno'"
        class="opt-cards"
        :value="value"
        @input="emitValue"
        @change="emitChange"
      >
        <label
          v-for="opt in question._options"
          :key="String(opt.value)"
          class="opt-card"
          :class="{ on: value === opt.value }"
        >
          <el-radio :label="opt.value">{{ opt.label }}</el-radio>
        </label>
      </el-radio-group>

      <div v-else-if="question.qType === 'likert'" class="likert-box">
        <div class="likert-labels" v-if="question._options && question._options.length">
          <span>{{ question._options[0].label }}</span>
          <span>{{ question._options[question._options.length - 1].label }}</span>
        </div>
        <div class="likert-row">
          <button
            v-for="opt in question._options"
            :key="String(opt.value)"
            type="button"
            class="likert-btn"
            :class="{ on: String(value) === String(opt.value) }"
            :title="opt.label"
            @click="pick(opt.value)"
          >
            <em>{{ opt.value }}</em>
            <span>{{ opt.label }}</span>
          </button>
        </div>
      </div>

      <div v-else-if="question.qType === 'image_radio'" class="img-opts">
        <label
          v-for="opt in question._options"
          :key="String(opt.value)"
          class="img-opt"
          :class="{ on: value === opt.value }"
          @click.prevent="pick(opt.value)"
        >
          <img v-if="opt.imageUrl" :src="opt.imageUrl" alt="" />
          <div v-else class="img-ph">无图</div>
          <span>{{ opt.label }}</span>
        </label>
      </div>

      <div v-else-if="question.qType === 'image_checkbox'" class="img-opts">
        <label
          v-for="opt in question._options"
          :key="String(opt.value)"
          class="img-opt"
          :class="{ on: isMultiOn(opt.value) }"
          @click.prevent="toggleMulti(opt.value)"
        >
          <img v-if="opt.imageUrl" :src="opt.imageUrl" alt="" />
          <div v-else class="img-ph">无图</div>
          <span>{{ opt.label }}</span>
        </label>
      </div>

      <div v-else-if="question.qType === 'matrix_radio'" class="matrix-wrap">
        <table class="matrix-table">
          <thead>
            <tr>
              <th class="row-h"></th>
              <th v-for="opt in question._options" :key="String(opt.value)">{{ opt.label }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in (question._rows || [])" :key="String(row.value || row.label)">
              <td class="row-h">{{ row.label }}</td>
              <td v-for="opt in question._options" :key="String(opt.value)" class="cell">
                <el-radio
                  :value="matrixVal(row)"
                  :label="opt.value"
                  @input="setMatrix(row, opt.value)"
                >&nbsp;</el-radio>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <el-checkbox-group
        v-else-if="question.qType === 'checkbox'"
        class="opt-cards"
        :value="value || []"
        @input="emitValue"
        @change="emitChange"
      >
        <label
          v-for="opt in question._options"
          :key="String(opt.value)"
          class="opt-card"
          :class="{ on: Array.isArray(value) && value.indexOf(opt.value) > -1 }"
        >
          <el-checkbox :label="opt.value">{{ opt.label }}</el-checkbox>
        </label>
      </el-checkbox-group>

      <el-select v-else-if="question.qType === 'select'" :value="value" clearable style="width:100%" @input="emitValue" @change="emitChange">
        <el-option v-for="opt in question._options" :key="String(opt.value)" :label="opt.label" :value="opt.value" />
      </el-select>

      <el-cascader
        v-else-if="question.qType === 'cascade_select'"
        :value="value || []"
        :options="question._options || []"
        :props="{ value: 'value', label: 'label', children: 'children', emitPath: true }"
        clearable
        filterable
        style="width:100%"
        @input="emitValue"
        @change="emitChange"
      />

      <el-rate v-else-if="question.qType === 'rate'" :value="value" :max="question._max || 5" @input="emitValue" @change="emitChange" />

      <div v-else-if="question.qType === 'nps'" class="nps-box">
        <div class="nps-labels">
          <span>{{ question._leftLabel || '不可能' }}</span>
          <span>{{ question._rightLabel || '非常可能' }}</span>
        </div>
        <div class="nps-row">
          <button
            v-for="n in 11"
            :key="n - 1"
            type="button"
            class="nps-btn"
            :class="{ on: value === (n - 1) }"
            @click="pick(n - 1)"
          >{{ n - 1 }}</button>
        </div>
      </div>

      <div v-else-if="question.qType === 'slider'" class="slider-wrap">
        <el-slider
          :value="value"
          :min="num(question._min, 0)"
          :max="num(question._max, 100)"
          :step="num(question._step, 1)"
          show-input
          @input="emitValue"
          @change="emitChange"
        />
      </div>

      <el-input-number
        v-else-if="question.qType === 'number'"
        :value="value"
        :min="question._min"
        :max="question._max"
        :step="num(question._step, 1)"
        controls-position="right"
        style="width:100%"
        @input="emitValue"
        @change="emitChange"
      />

      <el-date-picker
        v-else-if="question.qType === 'date'"
        :value="value"
        type="date"
        value-format="yyyy-MM-dd"
        style="width:100%"
        @input="emitValue"
      />
      <el-time-picker
        v-else-if="question.qType === 'time'"
        :value="value"
        value-format="HH:mm"
        format="HH:mm"
        style="width:100%"
        @input="emitValue"
      />
      <el-date-picker
        v-else-if="question.qType === 'datetime'"
        :value="value"
        type="datetime"
        value-format="yyyy-MM-dd HH:mm:ss"
        style="width:100%"
        @input="emitValue"
      />

      <el-alert
        v-else-if="question.qType === 'file' && mode === 'preview'"
        type="info"
        :closable="false"
        title="附件题：公开页可上传，预览模式不实际上传"
      />

      <div v-else-if="question.qType === 'file' && mode === 'open'" class="file-box">
        <el-upload
          :action="uploadUrl"
          :data="uploadData"
          :limit="1"
          :file-list="fileList || []"
          :before-upload="onBeforeUpload"
          :on-success="onFileSuccess"
          :on-remove="onFileRemove"
          :on-error="onFileError"
        >
          <el-button size="mini" type="primary" plain>选择文件</el-button>
          <div slot="tip" class="el-upload__tip">{{ question._placeholder || ('支持图片/PDF/Office，最大 ' + uploadMaxMb + 'MB') }}</div>
        </el-upload>
      </div>

      <el-input
        v-else-if="question.qType === 'textarea'"
        type="textarea"
        :rows="3"
        :value="value"
        :placeholder="placeholderOf(question)"
        :maxlength="question._maxLength || undefined"
        show-word-limit
        @input="emitValue"
      />

      <el-input
        v-else
        :value="value"
        :placeholder="placeholderOf(question)"
        :maxlength="inputMaxLength"
        @input="emitValue"
      />
    </template>
  </div>
</template>

<script>
import { typeLabel, placeholderOf, effectiveUploadMaxMb } from '@/utils/bizSurveyQuestion'

export default {
  name: 'SurveyQuestionField',
  props: {
    question: { type: Object, required: true },
    value: { default: undefined },
    mode: { type: String, default: 'open' },
    showLabel: { type: Boolean, default: true },
    showTypeTag: { type: Boolean, default: false },
    indexLabel: { type: [Number, String], default: null },
    uploadUrl: { type: String, default: '' },
    uploadData: { type: Object, default: () => ({}) },
    fileList: { type: Array, default: () => [] }
  },
  computed: {
    uploadMaxMb() {
      return effectiveUploadMaxMb(this.question)
    },
    inputMaxLength() {
      if (this.question.qType === 'phone') return 11
      if (this.question.qType === 'idcard') return 18
      return this.question._maxLength || undefined
    }
  },
  methods: {
    typeLabel,
    placeholderOf,
    num(v, d) {
      const n = Number(v)
      return Number.isFinite(n) ? n : d
    },
    emitValue(v) {
      this.$emit('input', v)
    },
    emitChange() {
      this.$emit('change')
    },
    pick(v) {
      this.emitValue(v)
      this.emitChange()
    },
    isMultiOn(val) {
      return Array.isArray(this.value) && this.value.map(String).indexOf(String(val)) > -1
    },
    toggleMulti(val) {
      const cur = Array.isArray(this.value) ? this.value.slice() : []
      const idx = cur.map(String).indexOf(String(val))
      if (idx > -1) cur.splice(idx, 1)
      else cur.push(val)
      this.emitValue(cur)
      this.emitChange()
    },
    matrixVal(row) {
      const map = this.value && typeof this.value === 'object' ? this.value : {}
      const rk = row.value || row.label
      return map[rk]
    },
    setMatrix(row, optVal) {
      const rk = row.value || row.label
      const map = Object.assign({}, (this.value && typeof this.value === 'object') ? this.value : {})
      map[rk] = optVal
      this.emitValue(map)
      this.emitChange()
    },
    onBeforeUpload(file) {
      const maxMb = this.uploadMaxMb
      if (file.size > maxMb * 1024 * 1024) {
        this.$message.error('附件不能超过 ' + maxMb + 'MB')
        return false
      }
      return true
    },
    onFileSuccess(res, file) {
      this.$emit('file-success', res, file)
      this.emitChange()
    },
    onFileRemove() {
      this.$emit('file-remove')
      this.emitChange()
    },
    onFileError() {
      this.$emit('file-error')
    }
  }
}
</script>

<style scoped>
.q-label { font-weight: 600; margin-bottom: 10px; display: flex; align-items: center; flex-wrap: wrap; gap: 6px; color: #303133; }
.q-label .idx { margin-right: 2px; }
.type-tag { font-weight: 500; }
.req { color: #f56c6c; }
.opt-cards {
  display: grid;
  gap: 8px;
}
.opt-card {
  display: block;
  margin: 0;
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
  cursor: pointer;
  transition: border-color .15s, box-shadow .15s, background .15s;
}
.opt-card:hover { border-color: #93c5fd; }
.opt-card.on {
  border-color: #1677ff;
  background: rgba(22, 119, 255, 0.06);
  box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.12);
}
.opt-card .el-radio,
.opt-card .el-checkbox {
  display: flex;
  align-items: center;
  margin: 0;
  width: 100%;
}
.section-box { background: #f8fafc; border: 1px dashed #dbe3ef; border-radius: 10px; padding: 14px; }
.section-box h4 { margin: 0 0 6px; color: #0f172a; }
.section-box p { margin: 0; color: #64748b; white-space: pre-wrap; line-height: 1.6; }
.nps-labels { display: flex; justify-content: space-between; font-size: 12px; color: #94a3b8; margin-bottom: 8px; }
.nps-row { display: flex; flex-wrap: wrap; gap: 6px; }
.nps-btn {
  width: 34px; height: 34px; border-radius: 8px; border: 1px solid #e5e7eb; background: #fff; cursor: pointer;
}
.nps-btn.on { background: #1677ff; color: #fff; border-color: transparent; }
.likert-box { margin-top: 4px; }
.likert-labels {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #94a3b8;
  margin-bottom: 8px;
}
.likert-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(72px, 1fr));
  gap: 8px;
}
.likert-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  min-height: 64px;
  padding: 8px 6px;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  background: #fff;
  cursor: pointer;
  color: #334155;
}
.likert-btn em {
  font-style: normal;
  font-weight: 700;
  font-size: 15px;
}
.likert-btn span {
  font-size: 11px;
  line-height: 1.3;
  color: #64748b;
}
.likert-btn.on {
  border-color: #1d4ed8;
  background: #eff6ff;
  box-shadow: 0 0 0 2px rgba(29, 78, 216, 0.12);
}
.likert-btn.on em,
.likert-btn.on span { color: #1d4ed8; }
.slider-wrap { padding: 0 8px; }
.img-opts { display: grid; grid-template-columns: repeat(auto-fill, minmax(120px, 1fr)); gap: 10px; }
.img-opt { border: 1px solid #e5e7eb; border-radius: 10px; padding: 8px; cursor: pointer; text-align: center; background: #fff; }
.img-opt.on { border-color: #1677ff; box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.15); }
.img-opt img, .img-ph {
  width: 100%; height: 80px; object-fit: cover; border-radius: 6px; margin-bottom: 6px; display: flex;
  align-items: center; justify-content: center; background: #f1f5f9; color: #94a3b8; font-size: 12px;
}
.file-box .el-upload__tip { color: #909399; }
.matrix-wrap { overflow-x: auto; }
.matrix-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.matrix-table th, .matrix-table td { border: 1px solid #e5e7eb; padding: 8px 6px; text-align: center; }
.matrix-table .row-h { text-align: left; min-width: 100px; background: #f8fafc; }
.matrix-table .cell .el-radio { margin: 0; }
</style>
