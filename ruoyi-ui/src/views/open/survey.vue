<template>
  <div class="open-survey" :style="rootStyle">
    <div class="hero">
      <h1>{{ surveyName || '问卷填写' }}</h1>
      <p v-if="surveyDesc">{{ surveyDesc }}</p>
    </div>

    <div class="panel" v-if="needPwd && !unlocked" v-loading="metaLoading">
      <el-form @submit.native.prevent="unlock" label-position="top" size="small">
        <el-form-item label="访问密码">
          <el-input v-model="accessPwd" show-password placeholder="请输入访问密码" @keyup.enter.native="unlock" />
        </el-form-item>
        <el-button type="primary" class="theme-btn" :loading="metaLoading" @click="unlock">进入问卷</el-button>
      </el-form>
    </div>

    <div class="panel" v-loading="metaLoading" v-else>
      <template v-if="!submitted && visibleQuestions.length">
        <div v-if="fillMode === 'step'" class="step-progress">
          <div class="step-track"><i :style="{ width: stepProgress + '%' }" /></div>
          <span>{{ stepIndex + 1 }} / {{ visibleQuestions.length }}</span>
        </div>
        <div v-for="q in displayQuestions" :key="q.questionId" class="q-block">
          <survey-question-field
            :question="q"
            v-model="form[q.questionId]"
            mode="open"
            :index-label="q.qType === 'section' ? null : questionNo(q)"
            :upload-url="uploadUrl"
            :upload-data="uploadData"
            :file-list="fileListMap[q.questionId] || []"
            @change="() => onAnswerChange(q)"
            @file-success="(res, file) => onFileSuccess(q, res, file)"
            @file-remove="() => onFileRemove(q)"
            @file-error="onFileError"
          />
        </div>

        <div v-if="needCaptcha && (fillMode !== 'step' || isLastStep)" class="captcha-row">
          <el-input v-model="captchaCode" placeholder="验证码" size="small" style="width:140px" @keyup.enter.native="handleSubmit" />
          <img v-if="captchaUrl" :src="captchaUrl" class="captcha-img" alt="captcha" @click="refreshCaptcha" />
          <el-button type="text" size="mini" @click="refreshCaptcha">换一张</el-button>
        </div>
        <div class="actions step-actions" v-if="fillMode === 'step'">
          <el-button :disabled="stepIndex <= 0" @click="prevStep">上一题</el-button>
          <el-button v-if="!isLastStep" type="primary" class="theme-btn" @click="nextStep">下一题</el-button>
          <el-button v-else type="primary" class="theme-btn" :loading="submitting" @click="handleSubmit">提交</el-button>
        </div>
        <div class="actions" v-else>
          <el-button type="primary" class="theme-btn" :loading="submitting" @click="handleSubmit">提交</el-button>
        </div>
      </template>

      <el-result v-else-if="submitted" icon="success" title="提交成功" sub-title="感谢您的参与">
        <template slot="extra">
          <el-button type="primary" class="theme-btn" @click="resetAndAgain" v-if="allowAgain">再填一份</el-button>
        </template>
      </el-result>

      <el-empty v-else-if="!metaLoading" description="问卷不可用或未发布" />
    </div>
  </div>
</template>

<script>
import { openSurveyMeta, openSurveySubmit, openSurveyUploadUrl, openSurveyDraft, saveOpenSurveyDraft } from '@/api/biz/survey'
import { getCodeImg } from '@/api/login'
import SurveyQuestionField from '@/components/biz/SurveyQuestionField'
import {
  normalizeQuestion,
  defaultAnswerValue,
  resolveVisibleQuestions,
  validateSurveyAnswers,
  NUMERIC_TYPES
} from '@/utils/bizSurveyQuestion'
import {
  getOrCreateClientToken,
  loadDraft,
  saveDraft,
  clearDraft,
  markSubmitted,
  isSubmittedLocally
} from '@/utils/bizSurveyDraft'

export default {
  name: 'OpenSurvey',
  components: { SurveyQuestionField },
  data() {
    return {
      code: '',
      metaLoading: false,
      submitting: false,
      submitted: false,
      surveyName: '',
      surveyDesc: '',
      theme: {},
      questions: [],
      form: {},
      fileListMap: {},
      startedAt: Date.now(),
      allowAgain: true,
      needPwd: false,
      unlocked: false,
      accessPwd: '',
      tick: 0,
      needCaptcha: false,
      captchaCode: '',
      captchaUuid: '',
      captchaUrl: '',
      channel: '',
      clientToken: '',
      stepIndex: 0,
      autoAdvanceTimer: null,
      draftTimer: null
    }
  },
  computed: {
    themeColor() { return (this.theme && this.theme.color) || '#1677ff' },
    rootStyle() {
      return {
        '--theme': this.themeColor,
        background: (this.theme && this.theme.bg) || undefined
      }
    },
    uploadUrl() { return openSurveyUploadUrl(this.code) },
    uploadData() { return this.accessPwd ? { accessPwd: this.accessPwd } : {} },
    fillMode() { return (this.theme && this.theme.fillMode) === 'step' ? 'step' : 'all' },
    visibleQuestions() {
      void this.tick
      return resolveVisibleQuestions(this.questions, q => this.form[q.questionId])
    },
    displayQuestions() {
      const list = this.visibleQuestions
      if (this.fillMode !== 'step') return list
      if (!list.length) return []
      const i = Math.min(Math.max(this.stepIndex, 0), list.length - 1)
      return [list[i]]
    },
    stepProgress() {
      const total = this.visibleQuestions.length
      if (!total) return 0
      return Math.round(((Math.min(this.stepIndex, total - 1) + 1) / total) * 100)
    },
    isLastStep() {
      return this.stepIndex >= Math.max(0, this.visibleQuestions.length - 1)
    }
  },
  watch: {
    visibleQuestions(list) {
      if (this.stepIndex >= list.length) this.stepIndex = Math.max(0, list.length - 1)
    }
  },
  created() {
    this.code = this.$route.params.code
    this.channel = (this.$route.query.channel || '').toString().trim()
    this.clientToken = getOrCreateClientToken(this.code)
    this.loadMeta()
  },
  beforeDestroy() {
    if (this.draftTimer) clearTimeout(this.draftTimer)
    if (this.autoAdvanceTimer) clearTimeout(this.autoAdvanceTimer)
  },
  methods: {
    onAnswerChange(q) {
      this.tick++
      const vis = new Set(this.visibleQuestions.map(x => String(x.questionId)))
      Object.keys(this.form).forEach(qid => {
        if (!vis.has(String(qid))) {
          const hit = this.questions.find(x => String(x.questionId) === String(qid))
          this.$set(this.form, qid, hit ? defaultAnswerValue(hit) : '')
          if (hit && hit.qType === 'file') this.$set(this.fileListMap, qid, [])
        }
      })
      this.scheduleDraftSave()
      const autoTypes = ['radio', 'yesno', 'image_radio', 'likert', 'select', 'nps']
      if (this.fillMode === 'step' && q && autoTypes.includes(q.qType) && !this.isLastStep) {
        if (this.autoAdvanceTimer) clearTimeout(this.autoAdvanceTimer)
        this.autoAdvanceTimer = setTimeout(() => {
          this.autoAdvanceTimer = null
          this.nextStep()
        }, 320)
      }
    },
    scheduleDraftSave() {
      if (this.draftTimer) clearTimeout(this.draftTimer)
      this.draftTimer = setTimeout(() => {
        const payload = {}
        Object.keys(this.form).forEach(k => { payload[k] = this.form[k] })
        saveDraft(this.code, payload)
        if (this.clientToken) {
          saveOpenSurveyDraft(this.code, {
            clientToken: this.clientToken,
            accessPwd: this.accessPwd || undefined,
            form: payload
          }).catch(() => {})
        }
      }, 400)
    },
    maybeRestoreDraft() {
      if (isSubmittedLocally(this.code) && !this.allowAgain) return
      const local = loadDraft(this.code)
      const apply = (form) => {
        if (!form) return
        const keys = Object.keys(form)
        if (!keys.length) return
        this.$confirm('检测到未提交草稿，是否恢复？', '提示', { type: 'info' }).then(() => {
          keys.forEach(k => {
            if (Object.prototype.hasOwnProperty.call(this.form, k) || this.questions.some(q => String(q.questionId) === String(k))) {
              this.$set(this.form, k, form[k])
            }
          })
          this.tick++
        }).catch(() => { clearDraft(this.code) })
      }
      const tryServer = () => {
        if (!this.clientToken) {
          if (local && local.form) apply(local.form)
          return
        }
        openSurveyDraft(this.code, { clientToken: this.clientToken, accessPwd: this.accessPwd || undefined }).then(res => {
          const remote = (res.data && res.data.form) || null
          const localAt = (local && local.savedAt) || 0
          const remoteAt = (res.data && res.data.savedAt) || 0
          if (remote && remoteAt >= localAt) apply(remote)
          else if (local && local.form) apply(local.form)
        }).catch(() => {
          if (local && local.form) apply(local.form)
        })
      }
      tryServer()
    },
    onFileSuccess(q, res, file) {
      if (!res || res.code !== 200) {
        this.$message.error((res && res.msg) || '上传失败')
        this.$set(this.fileListMap, q.questionId, [])
        this.$set(this.form, q.questionId, '')
        return
      }
      const payload = {
        fileName: res.fileName,
        url: res.url || res.fileName,
        originalFilename: res.originalFilename || file.name
      }
      this.$set(this.form, q.questionId, payload)
      this.$set(this.fileListMap, q.questionId, [{ name: payload.originalFilename, url: process.env.VUE_APP_BASE_API + payload.fileName }])
    },
    onFileRemove(q) {
      this.$set(this.form, q.questionId, '')
      this.$set(this.fileListMap, q.questionId, [])
    },
    onFileError() {
      this.$message.error('上传失败，请重试')
    },
    loadMeta() {
      this.metaLoading = true
      openSurveyMeta(this.code, this.accessPwd || undefined).then(res => {
        const data = res.data || {}
        this.surveyName = data.surveyName
        this.surveyDesc = data.surveyDesc
        this.needPwd = !!data.needPwd
        this.unlocked = !!data.unlocked || !data.needPwd
        document.title = this.surveyName || '问卷填写'
        if (!this.unlocked) return
        this.theme = data.theme || {}
        this.questions = (data.questions || []).map((q, i) => normalizeQuestion(q, i, { keyMode: 'open' }))
        const form = {}
        const files = {}
        this.questions.forEach(q => {
          form[q.questionId] = defaultAnswerValue(q)
          if (q.qType === 'file') files[q.questionId] = []
        })
        this.form = form
        this.fileListMap = files
        this.startedAt = Date.now()
        this.stepIndex = 0
        this.tick++
        this.needCaptcha = !!data.needCaptcha
        this.allowAgain = data.allowMulti !== '0'
        if (!this.allowAgain && isSubmittedLocally(this.code)) {
          this.submitted = true
          this.allowAgain = false
        } else {
          this.maybeRestoreDraft()
        }
        if (this.needCaptcha) this.refreshCaptcha()
      }).catch(err => {
        this.$message.error((err && err.msg) || '问卷不存在或未发布')
      }).finally(() => { this.metaLoading = false })
    },
    unlock() {
      if (!this.accessPwd) { this.$message.warning('请输入访问密码'); return }
      this.loadMeta()
    },
    questionNo(q) {
      let n = 0
      for (const item of this.visibleQuestions) {
        if (item.qType === 'section') continue
        n++
        if (item.questionId === q.questionId) return n
      }
      return n || 1
    },
    prevStep() {
      if (this.stepIndex > 0) this.stepIndex--
    },
    nextStep() {
      const r = validateSurveyAnswers(this.displayQuestions, q => this.form[q.questionId])
      if (!r.ok) {
        this.$message.warning(r.message)
        return
      }
      if (!this.isLastStep) this.stepIndex++
    },
    displayIndex(idx) {
      return this.visibleQuestions.slice(0, idx + 1).filter(x => x.qType !== 'section').length
    },
    validateClient() {
      const r = validateSurveyAnswers(this.visibleQuestions, q => this.form[q.questionId])
      if (!r.ok) {
        this.$message.warning(r.message)
        return false
      }
      return true
    },
    refreshCaptcha() {
      getCodeImg().then(res => {
        this.captchaUrl = 'data:image/gif;base64,' + res.img
        this.captchaUuid = res.uuid
        this.captchaCode = ''
      }).catch(() => {
        this.$message.error('验证码加载失败')
      })
    },
    handleSubmit() {
      if (!this.validateClient()) return
      if (this.needCaptcha && !this.captchaCode) {
        this.$message.warning('请输入验证码')
        return
      }
      const answers = this.visibleQuestions.filter(q => q.qType !== 'section').map(q => ({
        questionId: q.questionId,
        value: NUMERIC_TYPES.includes(q.qType)
          ? String(this.form[q.questionId] == null ? '' : this.form[q.questionId])
          : this.form[q.questionId]
      }))
      this.submitting = true
      openSurveySubmit(this.code, {
        costMs: Date.now() - this.startedAt,
        accessPwd: this.accessPwd || undefined,
        channel: this.channel || undefined,
        clientToken: this.clientToken || undefined,
        code: this.needCaptcha ? this.captchaCode : undefined,
        uuid: this.needCaptcha ? this.captchaUuid : undefined,
        answers
      }).then(() => {
        this.submitted = true
        markSubmitted(this.code)
        if (!this.allowAgain) this.allowAgain = false
      })
        .catch(() => { if (this.needCaptcha) this.refreshCaptcha() })
        .finally(() => { this.submitting = false })
    },
    resetAndAgain() {
      if (!this.allowAgain) return
      this.submitted = false
      clearDraft(this.code)
      this.loadMeta()
    }
  }
}
</script>

<style scoped>
.open-survey {
  min-height: 100vh;
  background: linear-gradient(180deg, #f5f8ff 0%, #f7f7f7 280px, #f7f7f7 100%);
  padding: 24px 16px 48px;
  box-sizing: border-box;
}
.hero { max-width: 720px; margin: 0 auto 16px; text-align: center; }
.hero h1 { margin: 0 0 8px; font-size: 26px; color: var(--theme); }
.hero p { margin: 0; color: #666; }
.panel {
  max-width: 720px; margin: 0 auto; background: #fff; border-radius: 12px;
  padding: 20px 16px; box-shadow: 0 6px 24px rgba(22, 119, 255, 0.06);
}
.q-block { margin-bottom: 22px; }
.actions { margin-top: 8px; text-align: center; }
.theme-btn { background: var(--theme); border-color: var(--theme); color: #fff; }
.theme-btn.is-plain { background: #fff; color: var(--theme); }
.captcha-row { display: flex; align-items: center; gap: 10px; justify-content: center; margin: 12px 0 4px; }
.captcha-img { height: 38px; cursor: pointer; border-radius: 4px; }
@media (max-width: 768px) { .hero h1 { font-size: 22px; } }

.step-progress { display: flex; align-items: center; gap: 10px; margin-bottom: 14px; font-size: 12px; color: #94a3b8; }
.step-track { flex: 1; height: 6px; border-radius: 999px; background: #e5e7eb; overflow: hidden; }
.step-track i { display: block; height: 100%; width: 0; background: var(--theme); border-radius: inherit; transition: width .25s ease; }
.step-actions { display: flex; gap: 10px; }
.step-actions .el-button { flex: 1; }
</style>
