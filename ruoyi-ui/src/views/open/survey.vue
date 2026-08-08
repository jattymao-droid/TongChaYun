<template>
  <div class="open-survey" :style="rootStyle">
    <div class="hero" :style="heroStyle" v-if="showHero">
      <h1 v-if="showTitle" :style="titleStyle">{{ surveyName || '问卷填写' }}</h1>
      <p v-if="showDesc && surveyDesc">{{ surveyDesc }}</p>
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
      <template v-if="!submitted && (fillMode === 'pages' ? pages.length && pages[0].questions.length : visibleQuestions.length)">
        <div v-if="fillMode === 'step' || fillMode === 'pages'" class="step-progress">
          <div class="step-track"><i :style="{ width: stepProgress + '%' }" /></div>
          <span v-if="fillMode === 'pages'">{{ stepIndex + 1 }} / {{ pages.length }} 页</span>
          <span v-else>{{ stepIndex + 1 }} / {{ stepQuestionList.length }}</span>
        </div>
        <div v-if="fillMode === 'pages' && currentPageTitle" class="page-title">{{ currentPageTitle }}</div>
        <div v-for="q in displayQuestions" :key="q.questionId" class="q-block">
          <survey-question-field
            :question="q"
            v-model="form[q.questionId]"
            mode="open"
            :index-label="isDisplayOnly(q.qType) ? null : questionNo(q)"
            :upload-url="uploadUrl"
            :upload-data="uploadData"
            :file-list="fileListMap[q.questionId] || []"
            :bound-signatures="boundSignaturesOf(q)"
            :bound-form="form"
            bound-key-field="questionId"
            @change="() => onAnswerChange(q)"
            @bound-input="onBoundInput"
            @bound-change="(sq) => onAnswerChange(sq)"
            @file-success="(res, file) => onFileSuccess(q, res, file)"
            @file-remove="() => onFileRemove(q)"
            @file-error="onFileError"
          />
        </div>

        <div v-if="needCaptcha && (fillMode === 'all' || isLastStep)" class="captcha-field">
          <div class="captcha-label">验证码 <span class="req">*</span></div>
          <div class="captcha-box">
            <el-input
              v-model="captchaCode"
              class="captcha-input"
              placeholder="请输入计算结果"
              maxlength="6"
              clearable
              @keyup.enter.native="handleSubmit"
            />
            <button type="button" class="captcha-media" title="点击刷新验证码" @click="refreshCaptcha">
              <img v-if="captchaUrl" :src="captchaUrl" alt="验证码" />
              <span v-else class="captcha-loading">加载中</span>
              <i class="captcha-refresh" aria-hidden="true">↻</i>
            </button>
          </div>
          <p class="captcha-hint">看不清？点击右侧图片刷新</p>
        </div>
        <div class="actions step-actions" v-if="fillMode === 'step' || fillMode === 'pages'">
          <el-button :disabled="stepIndex <= 0" @click="prevStep">{{ fillMode === 'pages' ? '上一页' : '上一题' }}</el-button>
          <el-button v-if="!isLastStep" type="primary" class="theme-btn" @click="nextStep">{{ fillMode === 'pages' ? '下一页' : '下一题' }}</el-button>
          <el-button v-else type="primary" class="theme-btn" :loading="submitting" @click="handleSubmit">提交</el-button>
        </div>
        <div class="actions" v-else>
          <el-button type="primary" class="theme-btn" :loading="submitting" @click="handleSubmit">提交</el-button>
        </div>
      </template>

      <el-result
        v-else-if="submitted"
        icon="success"
        :title="successTitle"
        :sub-title="successSubTitle"
        class="survey-success"
      >
        <template slot="extra">
          <div v-if="voucherNo" class="voucher-slip">
            <div class="voucher-label">报名凭证</div>
            <div class="voucher-name">{{ surveyName || '问卷' }}</div>
            <div class="voucher-no">{{ voucherNo }}</div>
            <p class="voucher-hint">请妥善保存凭证号，必要时可打印本页</p>
          </div>
          <div class="success-extra no-print">
            <el-button v-if="voucherNo" @click="printVoucher">打印凭证</el-button>
            <el-button type="primary" class="theme-btn" @click="resetAndAgain" v-if="showAgainBtn">再填一份</el-button>
            <el-button v-if="successRedirectUrl" @click="goSuccessRedirect">立即前往</el-button>
          </div>
        </template>
      </el-result>

      <el-empty v-else-if="!metaLoading" description="问卷不可用或未发布" />
    </div>
  </div>
</template>

<script>
import { openSurveyMeta, openSurveySubmit, openSurveyUploadUrl, openSurveyDraft, saveOpenSurveyDraft, openSurveyEvent } from '@/api/biz/survey'
import { getCodeImg } from '@/api/login'
import SurveyQuestionField from '@/components/biz/SurveyQuestionField'
import {
  normalizeQuestion,
  defaultAnswerValue,
  resolveVisibleQuestions,
  validateSurveyAnswers,
  groupVisibleIntoPages,
  questionDisplayNo,
  isDisplayOnly,
  NUMERIC_TYPES,
  getBoundSignatures,
  withoutEmbeddedSignatures,
  expandWithBoundSignatures
} from '@/utils/bizSurveyQuestion'
import {
  getOrCreateClientToken,
  loadDraft,
  saveDraft,
  clearDraft,
  markSubmitted,
  isSubmittedLocally
} from '@/utils/bizSurveyDraft'
import {
  normalizeSurveyTheme,
  buildSurveyPageStyle,
  buildSurveyHeroStyle,
  buildSurveyTitleStyle
} from '@/utils/bizSurveyTheme'

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
      startLogged: false,
      stepIndex: 0,
      autoAdvanceTimer: null,
      draftTimer: null,
      redirectTimer: null,
      redirectCountdown: 0,
      answerId: null
    }
  },
  computed: {
    themeNorm() {
      return normalizeSurveyTheme(this.theme)
    },
    showTitle() {
      return this.themeNorm.showTitle
    },
    showDesc() {
      return this.themeNorm.showDesc
    },
    showHero() {
      return this.showTitle || (this.showDesc && !!this.surveyDesc)
    },
    successTitle() {
      return this.themeNorm.successTitle || '提交成功'
    },
    successMsg() {
      return this.themeNorm.successMsg || '感谢您的参与'
    },
    successRedirectUrl() {
      return this.themeNorm.successRedirectUrl || ''
    },
    successSubTitle() {
      const tip = this.successMsg
      if (this.redirectCountdown > 0) return tip + '（' + this.redirectCountdown + ' 秒后跳转）'
      return tip
    },
    voucherNo() {
      if (!this.answerId) return ''
      return 'TCY-' + String(this.code || '').toUpperCase() + '-' + this.answerId
    },
    showAgainBtn() {
      return this.allowAgain && this.themeNorm.showFillAgain !== false
    },
    heroStyle() {
      return buildSurveyHeroStyle(this.theme)
    },
    titleStyle() {
      return buildSurveyTitleStyle(this.theme)
    },
    rootStyle() {
      return buildSurveyPageStyle(this.theme, process.env.VUE_APP_BASE_API)
    },
    uploadUrl() { return openSurveyUploadUrl(this.code) },
    uploadData() { return this.accessPwd ? { accessPwd: this.accessPwd } : {} },
    fillMode() {
      const m = this.theme && this.theme.fillMode
      return m === 'step' || m === 'pages' ? m : 'all'
    },
    visibleQuestions() {
      void this.tick
      return resolveVisibleQuestions(this.questions, q => this.form[q.questionId])
    },
    pages() {
      return groupVisibleIntoPages(withoutEmbeddedSignatures(this.visibleQuestions, this.questions))
    },
    stepQuestionList() {
      return withoutEmbeddedSignatures(
        this.visibleQuestions.filter(q => q.qType !== 'page_break'),
        this.questions
      )
    },
    currentPageTitle() {
      if (this.fillMode !== 'pages') return ''
      const page = this.pages[this.stepIndex]
      return (page && page.title) || ''
    },
    displayQuestions() {
      if (this.fillMode === 'pages') {
        const page = this.pages[Math.min(Math.max(this.stepIndex, 0), Math.max(this.pages.length - 1, 0))]
        return (page && page.questions) || []
      }
      if (this.fillMode === 'step') {
        const list = this.stepQuestionList
        if (!list.length) return []
        const i = Math.min(Math.max(this.stepIndex, 0), list.length - 1)
        return [list[i]]
      }
      return withoutEmbeddedSignatures(
        this.visibleQuestions.filter(q => q.qType !== 'page_break'),
        this.questions
      )
    },
    stepProgress() {
      const total = this.fillMode === 'pages' ? this.pages.length : this.stepQuestionList.length
      if (!total) return 0
      return Math.round(((Math.min(this.stepIndex, total - 1) + 1) / total) * 100)
    },
    isLastStep() {
      const total = this.fillMode === 'pages' ? this.pages.length : this.stepQuestionList.length
      return this.stepIndex >= Math.max(0, total - 1)
    }
  },
  watch: {
    visibleQuestions() {
      const total = this.fillMode === 'pages' ? this.pages.length : this.stepQuestionList.length
      if (this.stepIndex >= total) this.stepIndex = Math.max(0, total - 1)
    }
  },
  created() {
    this.code = this.$route.params.code
    this.channel = (this.$route.query.channel || '').toString().trim()
    this.clientToken = getOrCreateClientToken(this.code)
    this.loadMeta()
  },
  beforeDestroy() {
    this.clearSuccessRedirect()
    if (this.draftTimer) clearTimeout(this.draftTimer)
    if (this.autoAdvanceTimer) clearTimeout(this.autoAdvanceTimer)
  },
  methods: {
    isDisplayOnly,
    boundSignaturesOf(q) {
      if (!q || q.qType !== 'agreement') return []
      return getBoundSignatures(q, this.visibleQuestions)
    },
    onBoundInput(sq, v) {
      if (!sq || sq.questionId == null) return
      this.$set(this.form, sq.questionId, v)
      this.onAnswerChange(sq)
    },
    onAnswerChange(q) {
      this.tick++
      this.trackStart()
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
    trackStart() {
      if (this.startLogged || !this.code || !this.unlocked) return
      this.startLogged = true
      openSurveyEvent(this.code, {
        action: 'start',
        channel: this.channel || undefined,
        accessPwd: this.accessPwd || undefined
      }).catch(() => { this.startLogged = false })
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
      openSurveyMeta(this.code, this.accessPwd || undefined, this.channel || undefined).then(res => {
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
        this.startLogged = false
        this.stepIndex = 0
        this.tick++
        this.needCaptcha = !!data.needCaptcha
        this.allowAgain = data.allowMulti !== '0'
        if (!this.allowAgain && isSubmittedLocally(this.code)) {
          this.submitted = true
          this.allowAgain = false
          this.restoreVoucher()
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
      return questionDisplayNo(this.visibleQuestions, q) || 1
    },
    prevStep() {
      if (this.stepIndex > 0) this.stepIndex--
    },
    nextStep() {
      const r = validateSurveyAnswers(
        expandWithBoundSignatures(this.displayQuestions, this.visibleQuestions),
        q => this.form[q.questionId]
      )
      if (!r.ok) {
        this.$message.warning(r.message)
        return
      }
      if (!this.isLastStep) this.stepIndex++
    },
    displayIndex(idx) {
      return this.visibleQuestions.slice(0, idx + 1).filter(x => !isDisplayOnly(x.qType)).length
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
      const answers = this.visibleQuestions.filter(q => !isDisplayOnly(q.qType)).map(q => ({
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
      }).then(res => {
        const aid = res && res.data && res.data.answerId
        this.answerId = aid != null ? aid : null
        if (this.answerId != null) {
          try {
            sessionStorage.setItem('tcy_voucher_' + this.code, String(this.answerId))
          } catch (e) { /* ignore */ }
        }
        this.submitted = true
        markSubmitted(this.code)
        if (!this.allowAgain) this.allowAgain = false
        this.scheduleSuccessRedirect()
      })
        .catch(() => { if (this.needCaptcha) this.refreshCaptcha() })
        .finally(() => { this.submitting = false })
    },
    restoreVoucher() {
      try {
        const raw = sessionStorage.getItem('tcy_voucher_' + this.code)
        if (raw) this.answerId = Number(raw) || raw
      } catch (e) { /* ignore */ }
    },
    printVoucher() {
      window.print()
    },
    clearSuccessRedirect() {
      if (this.redirectTimer) {
        clearInterval(this.redirectTimer)
        this.redirectTimer = null
      }
      this.redirectCountdown = 0
    },
    scheduleSuccessRedirect() {
      this.clearSuccessRedirect()
      const url = this.successRedirectUrl
      if (!url) return
      let sec = Number(this.themeNorm.successRedirectSec)
      if (!Number.isFinite(sec) || sec < 0) sec = 0
      if (sec === 0) {
        window.location.href = url
        return
      }
      this.redirectCountdown = Math.floor(sec)
      this.redirectTimer = setInterval(() => {
        this.redirectCountdown -= 1
        if (this.redirectCountdown <= 0) {
          this.clearSuccessRedirect()
          window.location.href = url
        }
      }, 1000)
    },
    goSuccessRedirect() {
      if (this.successRedirectUrl) window.location.href = this.successRedirectUrl
    },
    resetAndAgain() {
      if (!this.allowAgain) return
      this.clearSuccessRedirect()
      this.submitted = false
      this.answerId = null
      clearDraft(this.code)
      this.loadMeta()
    }
  }
}
</script>

<style scoped>
.open-survey {
  min-height: 100vh;
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
.captcha-field { margin: 8px 0 16px; }
.captcha-label { font-size: 14px; font-weight: 600; color: #1f2937; margin-bottom: 8px; }
.captcha-label .req { color: #ef4444; margin-left: 2px; }
.captcha-box { display: flex; align-items: stretch; gap: 10px; }
.captcha-input { flex: 1; min-width: 0; }
.captcha-input >>> .el-input__inner {
  height: 42px;
  line-height: 42px;
  border-radius: 10px;
}
.captcha-media {
  position: relative;
  flex: none;
  width: 118px;
  height: 42px;
  padding: 0;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #f8fafc;
  overflow: hidden;
  cursor: pointer;
}
.captcha-media img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  border: 0;
}
.captcha-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  font-size: 12px;
  color: #94a3b8;
}
.captcha-refresh {
  position: absolute;
  right: 4px;
  bottom: 4px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: rgba(15, 23, 42, 0.55);
  color: #fff;
  font-style: normal;
  font-size: 12px;
  line-height: 20px;
  text-align: center;
  pointer-events: none;
}
.captcha-hint { margin: 6px 0 0; font-size: 12px; color: #94a3b8; }
@media (max-width: 768px) { .hero h1 { font-size: 22px; } .captcha-media { width: 108px; } }

.step-progress { display: flex; align-items: center; gap: 10px; margin-bottom: 14px; font-size: 12px; color: #94a3b8; }
.step-track { flex: 1; height: 6px; border-radius: 999px; background: #e5e7eb; overflow: hidden; }
.step-track i { display: block; height: 100%; width: 0; background: var(--theme); border-radius: inherit; transition: width .25s ease; }
.step-actions { display: flex; gap: 10px; }
.step-actions .el-button { flex: 1; }
.page-title { font-size: 16px; font-weight: 650; color: #1f2937; margin: 0 0 14px; }
.voucher-slip {
  margin: 0 auto 16px;
  max-width: 360px;
  padding: 16px 18px;
  border: 1.5px dashed #94a3b8;
  border-radius: 12px;
  background: #f8fafc;
  text-align: center;
}
.voucher-label {
  font-size: 12px;
  font-weight: 650;
  letter-spacing: .12em;
  color: var(--theme);
  margin-bottom: 6px;
}
.voucher-name { font-size: 14px; color: #334155; margin-bottom: 10px; }
.voucher-no {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: .04em;
  color: #0f172a;
  word-break: break-all;
}
.voucher-hint { margin: 10px 0 0; font-size: 12px; color: #64748b; }
.success-extra { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; }
@media print {
  .no-print, .hero { display: none !important; }
  .open-survey { padding: 12mm !important; background: #fff !important; }
  .panel { box-shadow: none !important; }
  .voucher-slip { border: 2px solid #333 !important; background: #fff !important; max-width: none; }
}
</style>
