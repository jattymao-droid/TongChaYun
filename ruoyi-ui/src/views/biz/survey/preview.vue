<template>
  <div :class="embedded ? 'preview-embed' : 'app-container'" v-loading="loading">
    <el-page-header v-if="!embedded" @back="goBack" :content="'预览 - ' + (survey.surveyName || '')" class="mb16" />
    <el-alert
      title="预览模式：不会真正提交答卷。若刚改过题目，请先保存再预览。"
      type="warning"
      :closable="false"
      class="mb16"
    />

    <div class="preview-wrap">
      <div class="hero">
        <h1>{{ survey.surveyName || '问卷预览' }}</h1>
        <p v-if="survey.surveyDesc">{{ survey.surveyDesc }}</p>
      </div>
      <div class="panel">
        <div v-if="(fillMode === 'step' || fillMode === 'pages') && displayQuestions.length" class="step-progress">
          <div class="step-track"><i :style="{ width: stepProgress + '%' }" /></div>
          <span v-if="fillMode === 'pages'">{{ stepIndex + 1 }} / {{ pages.length }} 页</span>
          <span v-else>{{ stepIndex + 1 }} / {{ stepQuestionList.length }}</span>
        </div>
        <div v-if="fillMode === 'pages' && currentPageTitle" class="page-title">{{ currentPageTitle }}</div>
        <div v-for="q in displayQuestions" :key="q._key" class="q-block">
          <survey-question-field
            :question="q"
            v-model="form[q._key]"
            mode="preview"
            :show-type-tag="true"
            :index-label="isDisplayOnly(q.qType) ? null : questionNo(q)"
            :bound-signatures="boundSignaturesOf(q)"
            :bound-form="form"
            bound-key-field="_key"
            @change="onChange"
            @bound-input="onBoundInput"
            @bound-change="onChange"
          />
        </div>

        <el-empty v-if="!loading && !questions.length" description="请先保存题目后再预览" />
        <el-empty v-else-if="!loading && questions.length && !visibleQuestions.length" description="当前跳题规则下无可显示题目" />
        <div class="actions" v-if="displayQuestions.length || (fillMode === 'all' && visibleQuestions.length)">
          <el-button @click="reload">刷新预览</el-button>
          <template v-if="fillMode === 'step' || fillMode === 'pages'">
            <el-button :disabled="stepIndex <= 0" @click="prevStep">{{ fillMode === 'pages' ? '上一页' : '上一题' }}</el-button>
            <el-button v-if="!isLastStep" type="primary" @click="nextStep">{{ fillMode === 'pages' ? '下一页' : '下一题' }}</el-button>
            <el-button v-else type="primary" @click="mockSubmit">模拟提交</el-button>
          </template>
          <el-button v-else type="primary" @click="mockSubmit">模拟提交</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getSurvey } from '@/api/biz/survey'
import SurveyQuestionField from '@/components/biz/SurveyQuestionField'
import {
  normalizeQuestion,
  defaultAnswerValue,
  resolveVisibleQuestions,
  validateSurveyAnswers,
  groupVisibleIntoPages,
  questionDisplayNo,
  isDisplayOnly,
  getBoundSignatures,
  withoutEmbeddedSignatures,
  expandWithBoundSignatures
} from '@/utils/bizSurveyQuestion'

export default {
  name: 'BizSurveyPreview',
  components: { SurveyQuestionField },
  props: {
    embedded: { type: Boolean, default: false },
    surveyIdProp: { type: [String, Number], default: null }
  },
  data() {
    return {
      loading: false,
      surveyId: null,
      survey: {},
      questions: [],
      form: {},
      tick: 0,
      stepIndex: 0,
      fillMode: 'all'
    }
  },
  computed: {
    visibleQuestions() {
      void this.tick
      return resolveVisibleQuestions(this.questions, q => this.form[q._key])
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
    },
    surveyIdProp: {
      immediate: false,
      handler(v) {
        if (v != null && String(v) !== String(this.surveyId)) {
          this.surveyId = v
          this.load()
        }
      }
    }
  },
  created() {
    this.surveyId = this.surveyIdProp != null ? this.surveyIdProp : this.$route.params.surveyId
    this.load()
  },
  activated() {
    if (this.surveyId) this.load()
  },
  methods: {
    isDisplayOnly,
    boundSignaturesOf(q) {
      if (!q || q.qType !== 'agreement') return []
      return getBoundSignatures(q, this.visibleQuestions)
    },
    onBoundInput(sq, v) {
      if (!sq || sq._key == null) return
      this.$set(this.form, sq._key, v)
      this.onChange()
    },
    answerNo(vIdx) {
      return this.visibleQuestions.slice(0, vIdx + 1).filter(q => !isDisplayOnly(q.qType)).length
    },
    questionNo(q) {
      return questionDisplayNo(this.visibleQuestions, q) || 1
    },
    prevStep() { if (this.stepIndex > 0) this.stepIndex-- },
    nextStep() {
      const r = validateSurveyAnswers(
        expandWithBoundSignatures(this.displayQuestions, this.visibleQuestions),
        q => this.form[q._key]
      )
      if (!r.ok) {
        this.$modal.msgWarning(r.message)
        return
      }
      if (!this.isLastStep) this.stepIndex++
    },
    goBack() {
      if (this.embedded) return
      this.$router.push('/biz/survey-design/index/' + this.surveyId)
    },
    reload() { this.load() },
    onChange() { this.tick++ },
    mockSubmit() {
      const r = validateSurveyAnswers(this.visibleQuestions, q => this.form[q._key])
      if (!r.ok) {
        this.$modal.msgWarning(r.message)
        return
      }
      this.$modal.msgSuccess('预览模式，不会提交')
    },
    load() {
      if (!this.surveyId) return
      this.loading = true
      getSurvey(this.surveyId).then(res => {
        const data = res.data || {}
        this.survey = data.survey || {}
        let theme = {}
        try { theme = this.survey.themeJson ? JSON.parse(this.survey.themeJson) : {} } catch (e) { theme = {} }
        this.fillMode = theme.fillMode === 'step' || theme.fillMode === 'pages' ? theme.fillMode : 'all'
        this.questions = (data.questions || []).map((q, i) => normalizeQuestion(q, i, { keyMode: 'preview' }))
        const form = {}
        this.questions.forEach(q => {
          form[q._key] = defaultAnswerValue(q)
        })
        this.form = form
        this.stepIndex = 0
        this.tick++
      }).finally(() => { this.loading = false })
    }
  }
}
</script>

<style scoped>
.mb16 { margin-bottom: 16px; }
.preview-wrap { max-width: 720px; margin: 0 auto; }
.hero { text-align: center; margin-bottom: 16px; }
.hero h1 { margin: 0 0 8px; color: #1677ff; }
.panel { background: #fff; border-radius: 12px; padding: 20px 16px; border: 1px solid #ebeef5; }
.q-block { margin-bottom: 22px; }
.actions { text-align: center; margin-top: 8px; display: flex; justify-content: center; gap: 10px; }

.step-progress { display: flex; align-items: center; gap: 10px; margin-bottom: 14px; font-size: 12px; color: #94a3b8; }
.step-track { flex: 1; height: 6px; border-radius: 999px; background: #e5e7eb; overflow: hidden; }
.step-track i { display: block; height: 100%; background: #2b6de5; border-radius: inherit; transition: width .25s ease; }
.page-title { font-size: 16px; font-weight: 650; color: #1f2937; margin: 0 0 14px; }
</style>
