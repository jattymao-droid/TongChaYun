<template>
  <div class="page survey-fill" :style="pageStyle">
    <div class="hero" :style="heroStyle" v-if="showHero">
      <h1 v-if="showTitle" :style="titleStyle">{{ surveyName || '问卷填写' }}</h1>
      <p v-if="showDesc && surveyDesc">{{ surveyDesc }}</p>
    </div>

    <div class="card card-pwd" v-if="needPwd && !unlocked">
      <div class="panel-tag">访问验证</div>
      <div class="field">
        <label>访问密码</label>
        <input v-model="accessPwd" type="password" placeholder="请输入访问密码" @keyup.enter="loadMeta" />
      </div>
      <div class="actions">
        <button class="btn primary" :disabled="metaLoading" @click="loadMeta">进入问卷</button>
      </div>
      <p class="msg" v-if="error">{{ error }}</p>
    </div>

    <div class="card card-success" v-else-if="submitted">
      <div class="success">
        <div class="success-mark no-print" aria-hidden="true">✓</div>
        <h2>{{ successTitle }}</h2>
        <p class="tip">{{ successMsg }}</p>
        <div v-if="voucherNo" class="voucher-slip">
          <div class="voucher-label">报名凭证</div>
          <div class="voucher-name">{{ surveyName || '问卷' }}</div>
          <div class="voucher-no">{{ voucherNo }}</div>
          <p class="voucher-hint">请妥善保存凭证号，必要时可打印本页</p>
        </div>
        <p v-if="redirectHint" class="tip redirect-hint no-print">{{ redirectHint }}</p>
        <div class="success-actions no-print">
          <button v-if="voucherNo" class="btn ghost" @click="printVoucher">打印凭证</button>
          <button v-if="showAgainBtn" class="btn primary" @click="again">再填一份</button>
          <a v-if="successRedirectUrl" class="btn ghost" :href="successRedirectUrl">立即前往</a>
        </div>
      </div>
    </div>

    <div class="card card-fill" v-else>
      <div class="loading-box" v-if="metaLoading">
        <span class="loading-dot" /><span class="loading-dot" /><span class="loading-dot" />
        <p>加载中…</p>
      </div>
      <template v-else-if="fillMode === 'pages' ? pages.length && pages[0].questions.length : stepQuestionList.length">
        <div v-if="fillMode === 'step' || fillMode === 'pages'" class="step-progress">
          <div class="step-track"><i :style="{ width: stepProgress + '%' }" /></div>
          <span v-if="fillMode === 'pages'">{{ stepIndex + 1 }} / {{ pages.length }} 页</span>
          <span v-else>{{ stepIndex + 1 }} / {{ stepQuestionList.length }}</span>
        </div>
        <div v-if="fillMode === 'pages' && currentPageTitle" class="page-title">{{ currentPageTitle }}</div>
        <Transition :name="(fillMode === 'step' || fillMode === 'pages') ? stepAnim : ''" mode="out-in">
          <div class="step-stage" :key="stepStageKey">
            <div class="field step-field" v-for="(q, qi) in displayQuestions" :key="q.questionId" :style="{ animationDelay: (qi * 0.04) + 's' }">
          <div v-if="q.qType === 'section'" class="section-box">
            <h3>{{ q.title }}</h3>
            <p>{{ q._content || '' }}</p>
          </div>
          <div v-else-if="q.qType === 'agreement'" class="agreement-box">
            <div class="q-head">
              <span class="q-no">{{ questionNo(q) }}</span>
              <label class="q-title">
                {{ q.title }}
                <span class="req" v-if="q.required === '1'">*</span>
              </label>
            </div>
            <div class="agree-body" v-html="q._content || ''" />
            <label class="opt agree-check" :class="{ on: form[q.questionId] === '1' }">
              <input type="checkbox" :checked="form[q.questionId] === '1'" @change="onAgree(q, $event)" />
              {{ q._agreeLabel || '我已阅读并同意' }}
            </label>
            <div
              v-for="sq in boundSignaturesOf(q)"
              :key="sq.questionId"
              class="agree-sign"
            >
              <label class="agree-sign-label">
                {{ sq.title || '手写签名' }}
                <span class="req" v-if="sq.required === '1'">*</span>
              </label>
              <SurveySignaturePad
                v-model="form[sq.questionId]"
                :pen-color="sq._penColor || '#111111'"
                :pad-height="sq._padHeight || 160"
                :upload-url="surveyUploadUrl(code)"
                :upload-data="accessPwd ? { accessPwd } : {}"
                :api-base="apiBase"
                @change="onChange"
                @error="onSignError"
              />
            </div>
          </div>
          <div v-else-if="q.qType === 'signature'" class="signature-box">
            <div class="q-head">
              <span class="q-no">{{ questionNo(q) }}</span>
              <label class="q-title">
                {{ q.title }}
                <span class="req" v-if="q.required === '1'">*</span>
              </label>
            </div>
            <SurveySignaturePad
              v-model="form[q.questionId]"
              :pen-color="q._penColor || '#111111'"
              :pad-height="q._padHeight || 160"
              :upload-url="surveyUploadUrl(code)"
              :upload-data="accessPwd ? { accessPwd } : {}"
              :api-base="apiBase"
              @change="onChange"
              @error="onSignError"
            />
          </div>
          <template v-else>
            <div class="q-head">
              <span class="q-no">{{ questionNo(q) }}</span>
              <label class="q-title">
                {{ q.title }}
                <span class="req" v-if="q.required === '1'">*</span>
              </label>
            </div>

            <template v-if="q.qType === 'radio' || q.qType === 'yesno'">
              <div class="opt-list">
                <label
                  class="opt"
                  v-for="opt in q._options"
                  :key="opt.value"
                  :class="{ on: form[q.questionId] === opt.value }"
                >
                  <input type="radio" :name="'q'+q.questionId" :value="opt.value" v-model="form[q.questionId]" @change="onSinglePick(q)" />
                  {{ opt.label }}
                </label>
              </div>
            </template>

            <div v-else-if="q.qType === 'likert'" class="likert-box">
              <div class="likert-row">
                <button
                  type="button"
                  v-for="opt in q._options"
                  :key="opt.value"
                  class="likert-btn"
                  :class="{ on: String(form[q.questionId]) === String(opt.value) }"
                  @click="form[q.questionId]=opt.value; onSinglePick(q)"
                >
                  <em>{{ opt.value }}</em>
                  <span>{{ opt.label }}</span>
                </button>
              </div>
            </div>

            <div v-else-if="q.qType === 'image_radio'" class="img-opts">
              <label v-for="opt in q._options" :key="opt.value" class="img-opt" :class="{ on: form[q.questionId] === opt.value }">
                <input type="radio" :name="'q'+q.questionId" :value="opt.value" v-model="form[q.questionId]" @change="onSinglePick(q)" />
                <img v-if="opt.imageUrl" :src="opt.imageUrl" alt="" />
                <span>{{ opt.label }}</span>
              </label>
            </div>

            <div v-else-if="q.qType === 'image_checkbox'" class="img-opts">
              <label
                v-for="opt in q._options"
                :key="opt.value"
                class="img-opt"
                :class="{ on: Array.isArray(form[q.questionId]) && form[q.questionId].includes(opt.value) }"
              >
                <input type="checkbox" :value="opt.value" v-model="form[q.questionId]" @change="onChange" />
                <img v-if="opt.imageUrl" :src="opt.imageUrl" alt="" />
                <span>{{ opt.label }}</span>
              </label>
            </div>

            <div v-else-if="q.qType === 'matrix_radio'" class="matrix-wrap">
              <table class="matrix-table">
                <thead>
                  <tr>
                    <th></th>
                    <th v-for="opt in q._options" :key="opt.value">{{ opt.label }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="row in (q._rows || [])" :key="row.value || row.label">
                    <td class="matrix-row-label">{{ row.label }}</td>
                    <td v-for="opt in q._options" :key="opt.value">
                      <label class="matrix-cell" :class="{ on: form[q.questionId] && form[q.questionId][row.value||row.label] === opt.value }">
                        <input
                          type="radio"
                          :name="'m'+q.questionId+'-'+(row.value||row.label)"
                          :value="opt.value"
                          :checked="form[q.questionId] && form[q.questionId][row.value||row.label] === opt.value"
                          @change="setMatrix(q, row, opt.value)"
                        />
                        <span class="matrix-dot" aria-hidden="true" />
                      </label>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            <template v-else-if="q.qType === 'checkbox'">
              <div class="opt-list">
                <label
                  class="opt"
                  v-for="opt in q._options"
                  :key="opt.value"
                  :class="{ on: Array.isArray(form[q.questionId]) && form[q.questionId].includes(opt.value) }"
                >
                  <input type="checkbox" :value="opt.value" v-model="form[q.questionId]" @change="onChange" />
                  {{ opt.label }}
                </label>
              </div>
            </template>

            <select v-else-if="q.qType === 'select'" class="ctrl" v-model="form[q.questionId]" @change="onSinglePick(q)">
              <option value="">请选择</option>
              <option v-for="opt in q._options" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
            </select>

            <div v-else-if="q.qType === 'cascade_select'" class="cascade-wrap">
              <select class="ctrl" :value="cascadeLevel(q, 0)" @change="onCascadeChange(q, 0, $event.target.value)">
                <option value="">请选择</option>
                <option v-for="opt in (q._options || [])" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
              </select>
              <select class="ctrl" v-if="cascadeChildren(q).length" :value="cascadeLevel(q, 1)" @change="onCascadeChange(q, 1, $event.target.value)">
                <option value="">请选择</option>
                <option v-for="opt in cascadeChildren(q)" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
              </select>
            </div>

            <div v-else-if="q.qType === 'rate'" class="rate-box">
              <button
                type="button"
                v-for="n in (q._max || 5)"
                :key="n"
                class="rate-star"
                :class="{ on: Number(form[q.questionId]) >= n }"
                @click="form[q.questionId]=n; onChange()"
              >★</button>
              <span class="rate-val" v-if="form[q.questionId]">{{ form[q.questionId] }} / {{ q._max || 5 }}</span>
            </div>
            <div v-else-if="q.qType === 'nps'" class="nps-box">
              <div class="nps-labels"><span>{{ q._leftLabel || '不可能' }}</span><span>{{ q._rightLabel || '非常可能' }}</span></div>
              <div class="nps-row">
                <button type="button" v-for="n in 11" :key="n-1" class="nps-btn" :class="{ on: form[q.questionId] === (n-1) }" @click="form[q.questionId]=n-1; onSinglePick(q)">{{ n-1 }}</button>
              </div>
            </div>
            <div v-else-if="q.qType === 'slider'" class="slider-box">
              <div class="slider-meta">
                <span>{{ q._min != null ? q._min : 0 }}</span>
                <strong>{{ form[q.questionId] == null || form[q.questionId] === '' ? '—' : form[q.questionId] }}</strong>
                <span>{{ q._max != null ? q._max : 100 }}</span>
              </div>
              <input
                class="slider-range"
                type="range"
                :min="q._min != null ? q._min : 0"
                :max="q._max != null ? q._max : 100"
                :step="q._step || 1"
                :value="form[q.questionId] == null || form[q.questionId] === '' ? (q._min != null ? q._min : 0) : form[q.questionId]"
                @input="form[q.questionId] = Number($event.target.value); onChange()"
              />
            </div>
            <div v-else-if="q.qType === 'number'" class="number-box">
              <input
                class="ctrl"
                type="number"
                :min="q._min"
                :max="q._max"
                :step="q._step || 1"
                v-model.number="form[q.questionId]"
                @change="onChange"
              />
              <p v-if="q._min != null || q._max != null" class="tip">
                范围 {{ q._min != null ? q._min : '不限' }} ~ {{ q._max != null ? q._max : '不限' }}
              </p>
            </div>
            <div v-else-if="q.qType === 'date' || q.qType === 'time' || q.qType === 'datetime'" class="date-wrap">
              <input
                class="ctrl date-ctrl"
                :type="q.qType === 'datetime' ? 'datetime-local' : q.qType"
                v-model="form[q.questionId]"
              />
            </div>
            <textarea v-else-if="q.qType === 'textarea'" class="ctrl" rows="4" v-model="form[q.questionId]" :placeholder="q._placeholder || '请输入'" :maxlength="q._maxLength || undefined" />

            <div v-else-if="q.qType === 'file'" class="file-box">
              <label class="file-pick">
                <input type="file" @change="onFile($event, q)" />
                <span>{{ (form[q.questionId] && form[q.questionId].originalFilename) || '选择文件' }}</span>
              </label>
              <p class="tip">{{ q._placeholder || ('最大 ' + (q._maxSizeMb || 5) + 'MB（上限 10MB）') }}</p>
            </div>

            <input
              v-else
              class="ctrl"
              :type="h5InputType(q)"
              v-model="form[q.questionId]"
              :placeholder="h5Placeholder(q)"
              :maxlength="h5MaxLength(q)"
            />
          </template>
        </div>
          </div>
        </Transition>
        <div class="fill-foot">
          <div v-if="needCaptcha && (fillMode === 'all' || isLastStep)" class="captcha-field">
            <div class="captcha-head">
              <label>验证码 <span class="req">*</span></label>
              <button type="button" class="captcha-refresh-btn" :disabled="captchaLoading" @click="refreshCaptcha">
                <span class="captcha-refresh-ico" :class="{ spin: captchaLoading }" aria-hidden="true">↻</span>
                换一张
              </button>
            </div>
            <div class="captcha-row">
              <input
                v-model="captchaCode"
                class="captcha-input"
                placeholder="请输入计算结果"
                maxlength="6"
                inputmode="numeric"
                autocomplete="off"
                @keyup.enter="submit"
              />
              <button
                type="button"
                class="captcha-media"
                :class="{ loading: captchaLoading || !captchaUrl }"
                title="点击刷新验证码"
                @click="refreshCaptcha"
              >
                <img v-if="captchaUrl" :src="captchaUrl" alt="验证码" draggable="false" />
                <span v-else class="captcha-loading">加载中</span>
              </button>
            </div>
            <p class="captcha-hint">看不清可点击图片或「换一张」刷新</p>
          </div>
          <div class="actions" :class="{ step: fillMode === 'step' || fillMode === 'pages' }">
            <button v-if="fillMode === 'step' || fillMode === 'pages'" class="btn" type="button" :disabled="stepIndex <= 0" @click="prevStep">{{ fillMode === 'pages' ? '上一页' : '上一题' }}</button>
            <button
              v-if="(fillMode === 'step' || fillMode === 'pages') && !isLastStep"
              class="btn primary"
              type="button"
              @click="nextStep"
            >{{ fillMode === 'pages' ? '下一页' : '下一题' }}</button>
            <button
              v-else
              class="btn primary"
              type="button"
              :disabled="submitting"
              @click="submit"
            >{{ submitting ? '提交中…' : '提交' }}</button>
          </div>
          <p class="msg" v-if="error">{{ error }}</p>
        </div>
      </template>
      <p class="empty" v-else>{{ error || '问卷不可用或未发布' }}</p>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { surveyMeta, surveySubmit, surveyUploadUrl, getCaptchaImage, surveyDraft, saveSurveyDraft, surveyEvent } from '@/api/open'
import SurveySignaturePad from '@/components/SurveySignaturePad.vue'
import {
  normalizeQuestion,
  defaultAnswerValue,
  resolveVisibleQuestions,
  validateSurveyAnswers,
  groupVisibleIntoPages,
  questionDisplayNo,
  isDisplayOnly,
  NUMERIC_TYPES,
  effectiveUploadMaxMb,
  placeholderOf,
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

const route = useRoute()
const code = computed(() => route.params.code)
const apiBase = import.meta.env.VITE_APP_BASE_API || ''
const metaLoading = ref(false)
const submitting = ref(false)
const submitted = ref(false)
const answerId = ref(null)
const voucherNo = computed(() => {
  if (!answerId.value) return ''
  return 'TCY-' + String(code.value || '').toUpperCase() + '-' + answerId.value
})
const needPwd = ref(false)
const unlocked = ref(false)
const accessPwd = ref('')
const surveyName = ref('')
const surveyDesc = ref('')
const theme = ref({})
const questions = ref([])
const form = reactive({})
const tick = ref(0)
const error = ref('')
const startedAt = ref(Date.now())
const needCaptcha = ref(false)
const captchaCode = ref('')
const captchaUuid = ref('')
const captchaUrl = ref('')
const captchaLoading = ref(false)
const channel = computed(() => String(route.query.channel || '').trim())
const allowAgain = ref(true)
const clientToken = ref('')
const startLogged = ref(false)
const stepIndex = ref(0)
const stepDir = ref(1)
let draftTimer = null
let autoAdvanceTimer = null

const pageStyle = computed(() => buildSurveyPageStyle(theme.value, apiBase))
const heroStyle = computed(() => buildSurveyHeroStyle(theme.value))
const titleStyle = computed(() => buildSurveyTitleStyle(theme.value))
const themeNorm = computed(() => normalizeSurveyTheme(theme.value))
const showTitle = computed(() => themeNorm.value.showTitle)
const showDesc = computed(() => themeNorm.value.showDesc)
const showHero = computed(() => showTitle.value || (showDesc.value && !!surveyDesc.value))
const successTitle = computed(() => themeNorm.value.successTitle || '提交成功')
const successMsg = computed(() => themeNorm.value.successMsg || '感谢您的参与')
const successRedirectUrl = computed(() => themeNorm.value.successRedirectUrl || '')
const showAgainBtn = computed(() => allowAgain.value && themeNorm.value.showFillAgain !== false)
const redirectHint = ref('')
let redirectTimer = null
let redirectCountdown = 0

const fillMode = computed(() => {
  const m = theme.value && theme.value.fillMode
  return m === 'step' || m === 'pages' ? m : 'all'
})

const visibleQuestions = computed(() => {
  void tick.value
  return resolveVisibleQuestions(questions.value, q => form[q.questionId])
})

const pages = computed(() => groupVisibleIntoPages(withoutEmbeddedSignatures(visibleQuestions.value, questions.value)))

const stepQuestionList = computed(() => withoutEmbeddedSignatures(
  visibleQuestions.value.filter(q => q.qType !== 'page_break'),
  questions.value
))

const currentPageTitle = computed(() => {
  if (fillMode.value !== 'pages') return ''
  const page = pages.value[stepIndex.value]
  return (page && page.title) || ''
})

const displayQuestions = computed(() => {
  if (fillMode.value === 'pages') {
    const page = pages.value[Math.min(Math.max(stepIndex.value, 0), Math.max(pages.value.length - 1, 0))]
    return (page && page.questions) || []
  }
  if (fillMode.value === 'step') {
    const list = stepQuestionList.value
    if (!list.length) return []
    const i = Math.min(Math.max(stepIndex.value, 0), list.length - 1)
    return [list[i]]
  }
  return stepQuestionList.value
})

const stepProgress = computed(() => {
  const total = fillMode.value === 'pages' ? pages.value.length : stepQuestionList.value.length
  if (!total) return 0
  return Math.round(((Math.min(stepIndex.value, total - 1) + 1) / total) * 100)
})

const isLastStep = computed(() => {
  const total = fillMode.value === 'pages' ? pages.value.length : stepQuestionList.value.length
  return stepIndex.value >= Math.max(0, total - 1)
})
const stepAnim = computed(() => (stepDir.value < 0 ? 'step-prev' : 'step-next'))
const stepStageKey = computed(() => {
  if (fillMode.value === 'pages') return 'page-' + stepIndex.value
  if (fillMode.value !== 'step') return 'all'
  const q = displayQuestions.value[0]
  return q ? q.questionId : 'empty'
})
const AUTO_ADVANCE_TYPES = ['radio', 'yesno', 'image_radio', 'likert', 'select', 'nps']

function h5InputType(q) {
  if (q.qType === 'phone') return 'tel'
  if (q.qType === 'email') return 'email'
  if (q.qType === 'url') return 'url'
  return 'text'
}
function h5Placeholder(q) {
  return placeholderOf(q)
}
function h5MaxLength(q) {
  if (q.qType === 'phone') return 11
  if (q.qType === 'idcard') return 18
  return q._maxLength || undefined
}

watch(visibleQuestions, () => {
  const total = fillMode.value === 'pages' ? pages.value.length : stepQuestionList.value.length
  if (stepIndex.value >= total) stepIndex.value = Math.max(0, total - 1)
})

function setMatrix(q, row, val) {
  const rk = row.value || row.label
  const cur = form[q.questionId] && typeof form[q.questionId] === 'object' ? { ...form[q.questionId] } : {}
  cur[rk] = val
  form[q.questionId] = cur
  onChange()
}

function cascadeLevel(q, idx) {
  const path = Array.isArray(form[q.questionId]) ? form[q.questionId] : []
  return path[idx] || ''
}

function cascadeChildren(q) {
  const path = Array.isArray(form[q.questionId]) ? form[q.questionId] : []
  const p0 = path[0]
  if (!p0) return []
  const parent = (q._options || []).find(o => String(o.value) === String(p0))
  return (parent && parent.children) || []
}

function onCascadeChange(q, level, val) {
  const path = Array.isArray(form[q.questionId]) ? [...form[q.questionId]] : []
  if (!val) {
    form[q.questionId] = path.slice(0, level)
  } else {
    path[level] = val
    form[q.questionId] = path.slice(0, level + 1)
  }
  onChange()
}

function onChange() {
  tick.value++
  trackStart()
  const vis = new Set(visibleQuestions.value.map(q => String(q.questionId)))
  Object.keys(form).forEach(qid => {
    if (!vis.has(String(qid))) {
      const q = questions.value.find(x => String(x.questionId) === String(qid))
      form[qid] = q ? defaultAnswerValue(q) : ''
    }
  })
  scheduleDraftSave()
}

function trackStart() {
  if (startLogged.value || !code.value || !unlocked.value) return
  startLogged.value = true
  surveyEvent(code.value, {
    action: 'start',
    channel: channel.value || undefined,
    accessPwd: accessPwd.value || undefined
  }).catch(() => { startLogged.value = false })
}

function onAgree(q, ev) {
  form[q.questionId] = ev.target.checked ? '1' : ''
  onChange()
}

function onSignError(e) {
  error.value = (e && e.message) || '签名上传失败'
}

function scheduleDraftSave() {
  if (draftTimer) clearTimeout(draftTimer)
  draftTimer = setTimeout(() => {
    const payload = {}
    Object.keys(form).forEach(k => { payload[k] = form[k] })
    saveDraft(code.value, payload)
    if (clientToken.value) {
      saveSurveyDraft(code.value, {
        clientToken: clientToken.value,
        accessPwd: accessPwd.value || undefined,
        form: payload
      }).catch(() => {})
    }
  }, 400)
}

function maybeRestoreDraft() {
  if (isSubmittedLocally(code.value) && !allowAgain.value) return
  const local = loadDraft(code.value)
  const apply = (data) => {
    if (!data) return
    const keys = Object.keys(data)
    if (!keys.length) return
    if (window.confirm('检测到未提交草稿，是否恢复？')) {
      keys.forEach(k => { form[k] = data[k] })
      tick.value++
    } else {
      clearDraft(code.value)
    }
  }
  if (!clientToken.value) {
    if (local && local.form) apply(local.form)
    return
  }
  surveyDraft(code.value, { clientToken: clientToken.value, accessPwd: accessPwd.value || undefined }).then(res => {
    const remote = (res.data && res.data.form) || null
    const localAt = (local && local.savedAt) || 0
    const remoteAt = (res.data && res.data.savedAt) || 0
    if (remote && remoteAt >= localAt) apply(remote)
    else if (local && local.form) apply(local.form)
  }).catch(() => {
    if (local && local.form) apply(local.form)
  })
}

async function onFile(ev, q) {
  const file = ev.target.files && ev.target.files[0]
  if (!file) return
  const maxMb = effectiveUploadMaxMb(q)
  if (file.size / 1024 / 1024 > maxMb) {
    error.value = '文件不能超过 ' + maxMb + 'MB'
    ev.target.value = ''
    return
  }
  error.value = ''
  const fd = new FormData()
  fd.append('file', file)
  if (accessPwd.value) fd.append('accessPwd', accessPwd.value)
  try {
    const res = await fetch(surveyUploadUrl(code.value), { method: 'POST', body: fd })
    const data = await res.json()
    if (data.code !== 200) throw new Error(data.msg || '上传失败')
    form[q.questionId] = {
      fileName: data.fileName,
      url: data.url || data.fileName,
      originalFilename: data.originalFilename || file.name
    }
  } catch (e) {
    error.value = e.message || '上传失败'
    form[q.questionId] = ''
  }
}

async function loadMeta() {
  error.value = ''
  metaLoading.value = true
  submitted.value = false
  answerId.value = null
  try {
    const res = await surveyMeta(code.value, accessPwd.value || undefined, channel.value || undefined)
    const data = res.data || {}
    surveyName.value = data.surveyName || ''
    surveyDesc.value = data.surveyDesc || ''
    needPwd.value = !!data.needPwd
    unlocked.value = !!data.unlocked || !data.needPwd
    document.title = surveyName.value || '问卷填写'
    if (!unlocked.value) return
    theme.value = data.theme || {}
    questions.value = (data.questions || []).map((q, i) => normalizeQuestion(q, i, { keyMode: 'open' }))
    Object.keys(form).forEach(k => delete form[k])
    questions.value.forEach(q => {
      form[q.questionId] = defaultAnswerValue(q)
    })
    startedAt.value = Date.now()
    startLogged.value = false
    stepIndex.value = 0
    tick.value++
    needCaptcha.value = !!data.needCaptcha
    allowAgain.value = data.allowMulti !== '0'
    clientToken.value = getOrCreateClientToken(code.value)
    if (!allowAgain.value && isSubmittedLocally(code.value)) {
      submitted.value = true
      restoreVoucher()
    } else {
      maybeRestoreDraft()
    }
    if (needCaptcha.value) await refreshCaptcha()
  } catch (e) {
    error.value = e.message || '加载失败'
  } finally {
    metaLoading.value = false
  }
}

async function refreshCaptcha() {
  if (captchaLoading.value) return
  captchaLoading.value = true
  try {
    const res = await getCaptchaImage()
    captchaUrl.value = 'data:image/gif;base64,' + res.img
    captchaUuid.value = res.uuid
    captchaCode.value = ''
  } catch (e) {
    error.value = e.message || '验证码加载失败'
  } finally {
    captchaLoading.value = false
  }
}

function questionNo(q) {
  return questionDisplayNo(visibleQuestions.value, q) || 1
}

function boundSignaturesOf(q) {
  if (!q || q.qType !== 'agreement') return []
  return getBoundSignatures(q, visibleQuestions.value)
}

function prevStep() {
  error.value = ''
  if (autoAdvanceTimer) { clearTimeout(autoAdvanceTimer); autoAdvanceTimer = null }
  if (stepIndex.value > 0) {
    stepDir.value = -1
    stepIndex.value--
  }
}

function nextStep() {
  error.value = ''
  const r = validateSurveyAnswers(
    expandWithBoundSignatures(displayQuestions.value, visibleQuestions.value),
    q => form[q.questionId]
  )
  if (!r.ok) {
    error.value = r.message
    return false
  }
  if (!isLastStep.value) {
    stepDir.value = 1
    stepIndex.value++
    return true
  }
  return false
}

function onSinglePick(q) {
  onChange()
  if (fillMode.value !== 'step' || !q || !AUTO_ADVANCE_TYPES.includes(q.qType)) return
  if (isLastStep.value) return
  if (autoAdvanceTimer) clearTimeout(autoAdvanceTimer)
  autoAdvanceTimer = setTimeout(() => {
    autoAdvanceTimer = null
    nextStep()
  }, 320)
}

function validate() {
  const r = validateSurveyAnswers(visibleQuestions.value, q => form[q.questionId])
  if (!r.ok) {
    error.value = r.message
    return false
  }
  return true
}

async function submit() {
  error.value = ''
  if (!validate()) return
  if (needCaptcha.value && !captchaCode.value) {
    error.value = '请输入验证码'
    return
  }
  submitting.value = true
  try {
    const answers = visibleQuestions.value.filter(q => !isDisplayOnly(q.qType)).map(q => ({
      questionId: q.questionId,
      value: NUMERIC_TYPES.includes(q.qType)
        ? String(form[q.questionId] == null ? '' : form[q.questionId])
        : form[q.questionId]
    }))
    const res = await surveySubmit(code.value, {
      costMs: Date.now() - startedAt.value,
      accessPwd: accessPwd.value || undefined,
      channel: channel.value || undefined,
      clientToken: clientToken.value || undefined,
      code: needCaptcha.value ? captchaCode.value : undefined,
      uuid: needCaptcha.value ? captchaUuid.value : undefined,
      answers
    })
    const aid = res && res.data && res.data.answerId
    answerId.value = aid != null ? aid : null
    if (answerId.value != null) {
      try {
        sessionStorage.setItem('tcy_voucher_' + code.value, String(answerId.value))
      } catch (_) { /* ignore */ }
    }
    submitted.value = true
    markSubmitted(code.value)
    scheduleSuccessRedirect()
  } catch (e) {
    error.value = e.message || '提交失败'
    if (needCaptcha.value) await refreshCaptcha()
  } finally {
    submitting.value = false
  }
}

function printVoucher() {
  window.print()
}

function clearSuccessRedirect() {
  if (redirectTimer) {
    clearInterval(redirectTimer)
    redirectTimer = null
  }
  redirectHint.value = ''
  redirectCountdown = 0
}

function scheduleSuccessRedirect() {
  clearSuccessRedirect()
  const url = successRedirectUrl.value
  if (!url) return
  let sec = Number(themeNorm.value.successRedirectSec)
  if (!Number.isFinite(sec) || sec < 0) sec = 0
  if (sec === 0) {
    window.location.href = url
    return
  }
  redirectCountdown = Math.floor(sec)
  redirectHint.value = redirectCountdown + ' 秒后自动跳转…'
  redirectTimer = setInterval(() => {
    redirectCountdown -= 1
    if (redirectCountdown <= 0) {
      clearSuccessRedirect()
      window.location.href = url
    } else {
      redirectHint.value = redirectCountdown + ' 秒后自动跳转…'
    }
  }, 1000)
}

function again() {
  if (!allowAgain.value) return
  clearSuccessRedirect()
  submitted.value = false
  answerId.value = null
  clearDraft(code.value)
  loadMeta()
}

function restoreVoucher() {
  try {
    const raw = sessionStorage.getItem('tcy_voucher_' + code.value)
    if (raw) answerId.value = Number(raw) || raw
  } catch (_) { /* ignore */ }
}

onMounted(loadMeta)
onUnmounted(() => {
  if (draftTimer) clearTimeout(draftTimer)
  if (autoAdvanceTimer) clearTimeout(autoAdvanceTimer)
  clearSuccessRedirect()
})
</script>

<style scoped>
.panel-tag {
  display: inline-flex;
  align-items: center;
  margin-bottom: 14px;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 650;
  color: var(--theme);
  background: color-mix(in srgb, var(--theme) 12%, #fff);
}
.q-head {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 12px;
}
.q-no {
  flex: none;
  min-width: 26px;
  height: 26px;
  padding: 0 6px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: var(--theme);
  background: color-mix(in srgb, var(--theme) 12%, #fff);
  line-height: 1;
  margin-top: 1px;
}
.q-title {
  flex: 1;
  margin: 0 !important;
  font-weight: 650;
  font-size: 15px;
  line-height: 1.45;
  color: var(--text);
}
.opt-list { display: grid; gap: 0; }
.step-field {
  padding-bottom: 4px;
  margin-bottom: 18px;
  border-bottom: 1px solid color-mix(in srgb, var(--border) 70%, transparent);
  animation: fieldIn .4s cubic-bezier(.22, 1, .36, 1) both;
}
.step-field:last-child { border-bottom: 0; margin-bottom: 8px; }
@keyframes fieldIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.rate-box {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
}
.rate-star {
  border: 0;
  background: transparent;
  color: #d1d5db;
  font-size: 28px;
  line-height: 1;
  min-width: 44px;
  min-height: 44px;
  padding: 8px 2px;
  cursor: pointer;
  transition: color .12s, transform .12s;
}
.rate-star.on { color: #f59e0b; }
.rate-star:active { transform: scale(1.08); }
.rate-val {
  margin-left: 8px;
  font-size: 13px;
  color: var(--muted);
  font-weight: 600;
}

.loading-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 36px 8px;
  color: var(--muted);
  font-size: 13px;
}
.loading-box p { margin: 0; }
.loading-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  margin: 0 3px;
  border-radius: 50%;
  background: var(--theme);
  animation: bounce 1s ease infinite;
}
.loading-dot:nth-child(2) { animation-delay: .15s; }
.loading-dot:nth-child(3) { animation-delay: .3s; }
@keyframes bounce {
  0%, 80%, 100% { transform: translateY(0); opacity: .4; }
  40% { transform: translateY(-6px); opacity: 1; }
}

.fill-foot {
  margin-top: 8px;
  padding-top: 4px;
}
.card-fill {
  padding-bottom: 18px;
}
.success-mark {
  width: 56px;
  height: 56px;
  margin: 0 auto 14px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  background: var(--theme);
  box-shadow: 0 10px 24px color-mix(in srgb, var(--theme) 35%, transparent);
  animation: popIn .45s cubic-bezier(.22, 1, .36, 1) both;
}
@keyframes popIn {
  from { opacity: 0; transform: scale(.7); }
  to { opacity: 1; transform: scale(1); }
}
.success-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
  margin-top: 16px;
}
.success-actions .btn { min-width: 120px; }
.voucher-slip {
  margin: 18px auto 8px;
  max-width: 320px;
  padding: 16px 18px;
  border: 1.5px dashed color-mix(in srgb, var(--theme) 55%, #94a3b8);
  border-radius: 12px;
  background: color-mix(in srgb, var(--theme) 6%, #fff);
  text-align: center;
}
.voucher-label {
  font-size: 12px;
  font-weight: 650;
  letter-spacing: .12em;
  color: var(--theme);
  margin-bottom: 6px;
}
.voucher-name {
  font-size: 14px;
  color: #334155;
  margin-bottom: 10px;
}
.voucher-no {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: .04em;
  color: #0f172a;
  word-break: break-all;
}
.voucher-hint {
  margin: 10px 0 0;
  font-size: 12px;
  color: #64748b;
}

@media print {
  .no-print, .hero, .site-footer, .ambient { display: none !important; }
  .page { background: #fff !important; padding: 16mm !important; min-height: auto !important; }
  .card-success { box-shadow: none !important; border: none !important; }
  .voucher-slip {
    border: 2px solid #333 !important;
    background: #fff !important;
    max-width: none;
    padding: 24px;
  }
  .voucher-no { font-size: 22px; }
}

.date-wrap {
  width: 100%;
}
.date-ctrl {
  width: 100% !important;
  max-width: 100% !important;
  min-width: 0;
}
.file-box { width: 100%; }
.file-pick {
  position: relative;
  display: flex;
  align-items: center;
  min-height: 48px;
  padding: 12px 14px;
  border: 1.5px dashed color-mix(in srgb, var(--theme) 35%, var(--border));
  border-radius: 12px;
  background: color-mix(in srgb, var(--theme) 5%, #fff);
  color: var(--theme);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
}
.file-pick input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}
.file-pick span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.step-progress {
  display: flex; align-items: center; gap: 10px; margin-bottom: 18px;
  font-size: 12px; color: var(--muted);
}
.step-track {
  flex: 1; height: 6px; border-radius: 999px; background: #e5e7eb; overflow: hidden;
}
.step-track i {
  display: block; height: 100%; background: var(--theme); border-radius: 999px;
  transition: width .25s ease;
}
.actions.step { display: flex; gap: 10px; width: 100%; }
.actions.step .btn { flex: 1; }
.page-title { font-size: 16px; font-weight: 650; color: var(--text); margin: 0 0 14px; }

.section-box {
  padding: 12px 14px;
  border-radius: 12px;
  background: color-mix(in srgb, var(--theme) 7%, #fff);
  border: 1px solid color-mix(in srgb, var(--theme) 14%, transparent);
  margin-bottom: 4px;
}
.section-box h3 { margin: 0 0 4px; font-size: 15px; color: var(--theme); }
.section-box p { margin: 0; color: var(--muted); font-size: 13px; line-height: 1.5; }
.agreement-box {
  border: 1px solid var(--border); border-radius: 14px; padding: 14px; background: #fafafa;
}
.agree-body {
  max-height: 200px; overflow: auto; margin: 0 0 12px; padding: 12px;
  background: #fff; border-radius: 10px; border: 1px solid #eef2f7;
  font-size: 13px; line-height: 1.55; color: var(--text);
  overflow-wrap: anywhere;
}
.agree-body :deep(p) { margin: 0 0 8px; }
.agree-body :deep(img),
.agree-body :deep(table),
.agree-body :deep(video) {
  max-width: 100%;
}
.agree-check { margin-top: 4px; }
.agree-sign {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed var(--border, #e5e7eb);
}
.agree-sign-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 8px;
}
.signature-box { margin-top: 4px; }

.img-opts { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
.img-opt {
  position: relative;
  display: flex; flex-direction: column; gap: 6px; align-items: center;
  border: 1.5px solid var(--border); border-radius: 14px; padding: 10px 8px;
  cursor: pointer; background: #fafbfc; text-align: center; font-size: 13px;
  transition: border-color .15s, box-shadow .15s, background .15s;
}
.img-opt input { position: absolute; opacity: 0; pointer-events: none; }
.img-opt img {
  width: 100%;
  aspect-ratio: 4 / 3;
  max-height: 140px;
  object-fit: contain;
  border-radius: 8px;
  background: #f1f5f9;
}
.img-opt span {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-word;
  line-height: 1.35;
}
.img-opt.on {
  border-color: var(--theme);
  background: color-mix(in srgb, var(--theme) 8%, #fff);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--theme) 14%, transparent);
}

.matrix-wrap { overflow-x: auto; -webkit-overflow-scrolling: touch; border-radius: 12px; border: 1px solid var(--border); }
.matrix-table { width: 100%; min-width: 0; border-collapse: collapse; font-size: 13px; }
.matrix-table th, .matrix-table td {
  border-bottom: 1px solid var(--border); border-right: 1px solid var(--border);
  padding: 0; text-align: center; white-space: nowrap;
}
.matrix-table th { padding: 10px 8px; }
.matrix-table th:last-child, .matrix-table td:last-child { border-right: 0; }
.matrix-table tr:last-child td { border-bottom: 0; }
.matrix-table th:first-child,
.matrix-table td.matrix-row-label {
  text-align: left; white-space: normal; min-width: 72px; max-width: 120px; padding: 10px 12px;
}
.matrix-table thead th { background: color-mix(in srgb, var(--theme) 8%, #fff); color: var(--muted); font-weight: 600; }
.matrix-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 48px;
  min-height: 48px;
  margin: 0;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}
.matrix-cell input {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
  pointer-events: none;
}
.matrix-dot {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2px solid #cbd5e1;
  background: #fff;
  box-shadow: inset 0 0 0 0 var(--theme);
  transition: border-color .15s, box-shadow .15s;
}
.matrix-cell.on .matrix-dot {
  border-color: var(--theme);
  box-shadow: inset 0 0 0 5px var(--theme);
}

.cascade-wrap { display: grid; gap: 8px; }

.slider-box { margin-top: 2px; }
.slider-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 12px;
  color: var(--muted);
}
.slider-meta strong {
  font-size: 18px;
  color: var(--theme);
  font-weight: 700;
  min-width: 2.5em;
  text-align: center;
}
.slider-range {
  display: block;
  width: 100%;
  height: 44px;
  margin: 0;
  accent-color: var(--theme);
  cursor: pointer;
}
.number-box .tip { margin-top: 6px; }

.nps-box { margin-top: 4px; }
.nps-labels { display: flex; justify-content: space-between; color: var(--muted); font-size: 12px; margin-bottom: 8px; }
.nps-row {
  display: grid;
  grid-template-columns: repeat(11, minmax(0, 1fr));
  gap: 4px;
}
.nps-btn {
  min-width: 0;
  width: 100%;
  min-height: 40px;
  height: 40px;
  padding: 0;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: #fff;
  cursor: pointer;
  font-size: 13px;
  color: var(--text);
  transition: background .12s, border-color .12s, color .12s, transform .12s;
}
.nps-btn:active { transform: scale(.96); }
.nps-btn.on { background: var(--theme); color: #fff; border-color: var(--theme); }
.likert-box { margin-top: 4px; }
.likert-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(56px, 1fr));
  gap: 6px;
}
.likert-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  min-height: 58px;
  padding: 8px 4px;
  border-radius: 12px;
  border: 1.5px solid #e5e7eb;
  background: #fafbfc;
  color: #334155;
  transition: border-color .15s, background .15s;
}
.likert-btn em { font-style: normal; font-weight: 700; font-size: 14px; }
.likert-btn span {
  font-size: 11px;
  line-height: 1.25;
  color: #64748b;
  text-align: center;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-word;
}
.likert-btn.on { border-color: var(--theme); background: color-mix(in srgb, var(--theme) 12%, #fff); }
.likert-btn.on em,
.likert-btn.on span { color: var(--theme); }

.captcha-field {
  margin: 8px 0 14px;
  padding: 14px;
  border-radius: 14px;
  border: 1px solid color-mix(in srgb, var(--theme) 18%, var(--border));
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--theme) 6%, #fff) 0%, #fff 100%);
}
.captcha-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}
.captcha-field .captcha-head > label {
  display: block;
  font-weight: 650;
  margin: 0;
  font-size: 15px;
}
.captcha-refresh-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: 0;
  background: transparent;
  color: var(--theme);
  font-size: 13px;
  font-weight: 600;
  padding: 4px 2px;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}
.captcha-refresh-btn:disabled {
  opacity: .55;
  cursor: not-allowed;
}
.captcha-refresh-ico {
  display: inline-flex;
  width: 16px;
  height: 16px;
  align-items: center;
  justify-content: center;
  font-style: normal;
  font-size: 14px;
  line-height: 1;
}
.captcha-refresh-ico.spin {
  animation: captchaSpin .7s linear infinite;
}
@keyframes captchaSpin {
  to { transform: rotate(360deg); }
}
.captcha-row {
  display: flex;
  align-items: stretch;
  gap: 10px;
}
.captcha-input {
  flex: 1;
  min-width: 0;
  height: 48px;
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 0 14px;
  font-size: 16px;
  letter-spacing: 0.06em;
  background: #fff;
  outline: none;
}
.captcha-input:focus {
  border-color: var(--theme);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--theme) 16%, transparent);
}
.captcha-media {
  position: relative;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 132px;
  height: 48px;
  padding: 4px 8px;
  border: 1px dashed color-mix(in srgb, var(--theme) 28%, var(--border));
  border-radius: 12px;
  background:
    repeating-linear-gradient(
      -12deg,
      #f8fafc,
      #f8fafc 8px,
      #f1f5f9 8px,
      #f1f5f9 16px
    );
  overflow: hidden;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
  transition: border-color .15s, box-shadow .15s;
}
.captcha-media:active {
  box-shadow: inset 0 0 0 2px color-mix(in srgb, var(--theme) 18%, transparent);
}
.captcha-media.loading {
  opacity: .85;
}
.captcha-media img {
  display: block;
  max-width: 100%;
  max-height: 100%;
  width: auto;
  height: auto;
  object-fit: contain;
  border: 0;
  image-rendering: auto;
  border-radius: 4px;
  background: #fff;
}
.captcha-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #94a3b8;
}
.captcha-hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: #94a3b8;
  line-height: 1.4;
}

@media (max-width: 480px) {
  .img-opts { grid-template-columns: 1fr; }
  .nps-btn { min-height: 36px; height: 36px; font-size: 12px; }
  .captcha-media { width: 118px; }
  .captcha-refresh-btn { min-height: 36px; padding: 6px 4px; }
}

.step-stage { position: relative; min-height: 120px; }
.step-next-enter-active, .step-next-leave-active,
.step-prev-enter-active, .step-prev-leave-active {
  transition: opacity .22s ease, transform .22s ease;
}
.step-next-enter-from { opacity: 0; transform: translateX(18px); }
.step-next-leave-to { opacity: 0; transform: translateX(-14px); }
.step-prev-enter-from { opacity: 0; transform: translateX(-18px); }
.step-prev-leave-to { opacity: 0; transform: translateX(14px); }
.btn.primary { transition: transform .12s ease, box-shadow .12s ease; }
.btn.primary:active:not(:disabled) { transform: scale(.98); }
.success { text-align: center; padding: 12px 0 4px; }
.success h2 { margin: 0 0 8px; color: var(--theme); font-size: 22px; }
.success .btn { display: inline-block; margin: 0; text-decoration: none; }
.btn.ghost {
  background: #fff;
  color: var(--theme);
  border: 1px solid color-mix(in srgb, var(--theme) 35%, #cbd5e1);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.redirect-hint { color: #64748b; font-size: 13px; }

@media (prefers-reduced-motion: reduce) {
  .step-field, .success-mark, .loading-dot {
    animation: none !important;
  }
}
</style>
