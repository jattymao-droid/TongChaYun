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
        <div v-if="fillMode === 'step' && visibleQuestions.length" class="step-progress">
          <div class="step-track"><i :style="{ width: stepProgress + '%' }" /></div>
          <span>{{ stepIndex + 1 }} / {{ visibleQuestions.length }}</span>
        </div>
        <div v-for="(q, vIdx) in displayQuestions" :key="q._key" class="q-block">
          <survey-question-field
            :question="q"
            v-model="form[q._key]"
            mode="preview"
            :show-type-tag="true"
            :index-label="q.qType === 'section' ? null : questionNo(q)"
            @change="onChange"
          />
        </div>

        <el-empty v-if="!loading && !questions.length" description="请先保存题目后再预览" />
        <el-empty v-else-if="!loading && questions.length && !visibleQuestions.length" description="当前跳题规则下无可显示题目" />
        <div class="actions" v-if="visibleQuestions.length">
          <el-button @click="reload">刷新预览</el-button>
          <template v-if="fillMode === 'step'">
            <el-button :disabled="stepIndex <= 0" @click="prevStep">上一题</el-button>
            <el-button v-if="!isLastStep" type="primary" @click="nextStep">下一题</el-button>
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
  validateSurveyAnswers
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
  watch: {
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
    answerNo(vIdx) {
      return this.visibleQuestions.slice(0, vIdx + 1).filter(q => q.qType !== 'section').length
    },
    questionNo(q) {
      let n = 0
      for (const item of this.visibleQuestions) {
        if (item.qType === 'section') continue
        n++
        if (item._key === q._key) return n
      }
      return n || 1
    },
    prevStep() { if (this.stepIndex > 0) this.stepIndex-- },
    nextStep() {
      const r = validateSurveyAnswers(this.displayQuestions, q => this.form[q._key])
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
        this.fillMode = theme.fillMode === 'step' ? 'step' : 'all'
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
</style>
