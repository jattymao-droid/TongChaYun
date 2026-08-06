<template>
  <div class="page" :style="pageStyle">
    <div class="hero">
      <h1>{{ surveyName || '问卷填写' }}</h1>
      <p v-if="surveyDesc">{{ surveyDesc }}</p>
    </div>

    <div class="card" v-if="needPwd && !unlocked">
      <div class="field">
        <label>访问密码</label>
        <input v-model="accessPwd" type="password" placeholder="请输入访问密码" @keyup.enter="loadMeta" />
      </div>
      <div class="actions">
        <button class="btn primary" :disabled="metaLoading" @click="loadMeta">进入问卷</button>
      </div>
      <p class="msg" v-if="error">{{ error }}</p>
    </div>

    <div class="card" v-else-if="submitted">
      <div class="success">
        <h2>提交成功</h2>
        <p class="tip">感谢您的参与</p>
        <button v-if="allowAgain" class="btn primary" @click="again">再填一份</button>
      </div>
    </div>

    <div class="card" v-else>
      <p class="empty" v-if="metaLoading">加载中…</p>
      <template v-else-if="visibleQuestions.length">
        <div v-if="fillMode === 'step'" class="step-progress">
          <div class="step-track"><i :style="{ width: stepProgress + '%' }" /></div>
          <span>{{ stepIndex + 1 }} / {{ visibleQuestions.length }}</span>
        </div>
        <Transition :name="fillMode === 'step' ? stepAnim : ''" mode="out-in">
          <div class="step-stage" :key="stepStageKey">
            <div class="field step-field" v-for="(q, idx) in displayQuestions" :key="q.questionId">
          <div v-if="q.qType === 'section'" class="section-box">
            <h3>{{ q.title }}</h3>
            <p>{{ q._content || '' }}</p>
          </div>
          <template v-else>
            <label>
              {{ questionNo(q) }}. {{ q.title }}
              <span class="req" v-if="q.required === '1'">*</span>
            </label>

            <template v-if="q.qType === 'radio' || q.qType === 'yesno'">
              <label
                class="opt"
                v-for="opt in q._options"
                :key="opt.value"
                :class="{ on: form[q.questionId] === opt.value }"
              >
                <input type="radio" :name="'q'+q.questionId" :value="opt.value" v-model="form[q.questionId]" @change="onSinglePick(q)" />
                {{ opt.label }}
              </label>
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
                    <td>{{ row.label }}</td>
                    <td v-for="opt in q._options" :key="opt.value">
                      <input
                        type="radio"
                        :name="'m'+q.questionId+'-'+(row.value||row.label)"
                        :value="opt.value"
                        :checked="form[q.questionId] && form[q.questionId][row.value||row.label] === opt.value"
                        @change="setMatrix(q, row, opt.value)"
                      />
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            <template v-else-if="q.qType === 'checkbox'">
              <label
                class="opt"
                v-for="opt in q._options"
                :key="opt.value"
                :class="{ on: Array.isArray(form[q.questionId]) && form[q.questionId].includes(opt.value) }"
              >
                <input type="checkbox" :value="opt.value" v-model="form[q.questionId]" @change="onChange" />
                {{ opt.label }}
              </label>
            </template>

            <select v-else-if="q.qType === 'select'" v-model="form[q.questionId]" @change="onSinglePick(q)">
              <option value="">请选择</option>
              <option v-for="opt in q._options" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
            </select>

            <div v-else-if="q.qType === 'cascade_select'" class="cascade-wrap">
              <select :value="cascadeLevel(q, 0)" @change="onCascadeChange(q, 0, $event.target.value)">
                <option value="">请选择</option>
                <option v-for="opt in (q._options || [])" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
              </select>
              <select v-if="cascadeChildren(q).length" :value="cascadeLevel(q, 1)" @change="onCascadeChange(q, 1, $event.target.value)">
                <option value="">请选择</option>
                <option v-for="opt in cascadeChildren(q)" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
              </select>
            </div>

            <input v-else-if="q.qType === 'rate'" type="number" min="1" :max="q._max || 5" v-model.number="form[q.questionId]" @change="onChange" />
            <div v-else-if="q.qType === 'nps'" class="nps-box">
              <div class="nps-labels"><span>{{ q._leftLabel || '不可能' }}</span><span>{{ q._rightLabel || '非常可能' }}</span></div>
              <div class="nps-row">
                <button type="button" v-for="n in 11" :key="n-1" class="nps-btn" :class="{ on: form[q.questionId] === (n-1) }" @click="form[q.questionId]=n-1; onSinglePick(q)">{{ n-1 }}</button>
              </div>
            </div>
            <input v-else-if="q.qType === 'slider' || q.qType === 'number'" type="number" :min="q._min" :max="q._max" :step="q._step || 1" v-model.number="form[q.questionId]" @change="onChange" />
            <input v-else-if="q.qType === 'date'" type="date" v-model="form[q.questionId]" />
            <input v-else-if="q.qType === 'time'" type="time" v-model="form[q.questionId]" />
            <input v-else-if="q.qType === 'datetime'" type="datetime-local" v-model="form[q.questionId]" />
            <textarea v-else-if="q.qType === 'textarea'" rows="3" v-model="form[q.questionId]" :placeholder="q._placeholder || '请输入'" :maxlength="q._maxLength || undefined" />

            <div v-else-if="q.qType === 'file'">
              <input type="file" @change="onFile($event, q)" />
              <p class="tip" v-if="form[q.questionId] && form[q.questionId].originalFilename">已选：{{ form[q.questionId].originalFilename }}</p>
              <p class="tip">{{ q._placeholder || ('最大 ' + (q._maxSizeMb || 5) + 'MB（上限 10MB）') }}</p>
            </div>

            <input
              v-else
              :type="h5InputType(q)"
              v-model="form[q.questionId]"
              :placeholder="h5Placeholder(q)"
              :maxlength="h5MaxLength(q)"
            />
          </template>
        </div>
          </div>
        </Transition>
        <div v-if="needCaptcha && (fillMode !== 'step' || isLastStep)" class="captcha-row">
          <input v-model="captchaCode" placeholder="验证码" maxlength="6" />
          <img v-if="captchaUrl" :src="captchaUrl" alt="captcha" @click="refreshCaptcha" />
          <button type="button" class="btn link" @click="refreshCaptcha">换一张</button>
        </div>
        <div class="actions" :class="{ step: fillMode === 'step' }">
          <button v-if="fillMode === 'step'" class="btn" type="button" :disabled="stepIndex <= 0" @click="prevStep">上一题</button>
          <button
            v-if="fillMode === 'step' && !isLastStep"
            class="btn primary"
            type="button"
            @click="nextStep"
          >下一题</button>
          <button
            v-else
            class="btn primary"
            type="button"
            :disabled="submitting"
            @click="submit"
          >提交</button>
        </div>
        <p class="msg" v-if="error">{{ error }}</p>
      </template>
      <p class="empty" v-else>{{ error || '问卷不可用或未发布' }}</p>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { surveyMeta, surveySubmit, surveyUploadUrl, getCaptchaImage, surveyDraft, saveSurveyDraft } from '@/api/open'
import {
  normalizeQuestion,
  defaultAnswerValue,
  resolveVisibleQuestions,
  validateSurveyAnswers,
  NUMERIC_TYPES,
  effectiveUploadMaxMb,
  placeholderOf
} from '@/utils/bizSurveyQuestion'
import {
  getOrCreateClientToken,
  loadDraft,
  saveDraft,
  clearDraft,
  markSubmitted,
  isSubmittedLocally
} from '@/utils/bizSurveyDraft'

const route = useRoute()
const code = computed(() => route.params.code)
const metaLoading = ref(false)
const submitting = ref(false)
const submitted = ref(false)
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
const channel = computed(() => String(route.query.channel || '').trim())
const allowAgain = ref(true)
const clientToken = ref('')
const stepIndex = ref(0)
const stepDir = ref(1)
let draftTimer = null
let autoAdvanceTimer = null

const pageStyle = computed(() => ({
  '--theme': (theme.value && theme.value.color) || '#1677ff',
  background: (theme.value && theme.value.bg) || undefined
}))

const fillMode = computed(() => ((theme.value && theme.value.fillMode) === 'step' ? 'step' : 'all'))

const visibleQuestions = computed(() => {
  void tick.value
  return resolveVisibleQuestions(questions.value, q => form[q.questionId])
})

const displayQuestions = computed(() => {
  const list = visibleQuestions.value
  if (fillMode.value !== 'step') return list
  if (!list.length) return []
  const i = Math.min(Math.max(stepIndex.value, 0), list.length - 1)
  return [list[i]]
})

const stepProgress = computed(() => {
  const total = visibleQuestions.value.length
  if (!total) return 0
  return Math.round(((Math.min(stepIndex.value, total - 1) + 1) / total) * 100)
})

const isLastStep = computed(() => stepIndex.value >= Math.max(0, visibleQuestions.value.length - 1))
const stepAnim = computed(() => (stepDir.value < 0 ? 'step-prev' : 'step-next'))
const stepStageKey = computed(() => {
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

watch(visibleQuestions, (list) => {
  if (stepIndex.value >= list.length) stepIndex.value = Math.max(0, list.length - 1)
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
  const vis = new Set(visibleQuestions.value.map(q => String(q.questionId)))
  Object.keys(form).forEach(qid => {
    if (!vis.has(String(qid))) {
      const q = questions.value.find(x => String(x.questionId) === String(qid))
      form[qid] = q ? defaultAnswerValue(q) : ''
    }
  })
  scheduleDraftSave()
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
  try {
    const res = await surveyMeta(code.value, accessPwd.value || undefined)
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
    stepIndex.value = 0
    tick.value++
    needCaptcha.value = !!data.needCaptcha
    allowAgain.value = data.allowMulti !== '0'
    clientToken.value = getOrCreateClientToken(code.value)
    if (!allowAgain.value && isSubmittedLocally(code.value)) {
      submitted.value = true
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
  try {
    const res = await getCaptchaImage()
    captchaUrl.value = 'data:image/gif;base64,' + res.img
    captchaUuid.value = res.uuid
    captchaCode.value = ''
  } catch (e) {
    error.value = e.message || '验证码加载失败'
  }
}

function questionNo(q) {
  const list = visibleQuestions.value
  let n = 0
  for (const item of list) {
    if (item.qType === 'section') continue
    n++
    if (item.questionId === q.questionId) return n
  }
  return n || 1
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
  const r = validateSurveyAnswers(displayQuestions.value, q => form[q.questionId])
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
    const answers = visibleQuestions.value.filter(q => q.qType !== 'section').map(q => ({
      questionId: q.questionId,
      value: NUMERIC_TYPES.includes(q.qType)
        ? String(form[q.questionId] == null ? '' : form[q.questionId])
        : form[q.questionId]
    }))
    await surveySubmit(code.value, {
      costMs: Date.now() - startedAt.value,
      accessPwd: accessPwd.value || undefined,
      channel: channel.value || undefined,
      clientToken: clientToken.value || undefined,
      code: needCaptcha.value ? captchaCode.value : undefined,
      uuid: needCaptcha.value ? captchaUuid.value : undefined,
      answers
    })
    submitted.value = true
    markSubmitted(code.value)
  } catch (e) {
    error.value = e.message || '提交失败'
    if (needCaptcha.value) await refreshCaptcha()
  } finally {
    submitting.value = false
  }
}

function again() {
  if (!allowAgain.value) return
  submitted.value = false
  clearDraft(code.value)
  loadMeta()
}

onMounted(loadMeta)
onUnmounted(() => { if (draftTimer) clearTimeout(draftTimer); if (autoAdvanceTimer) clearTimeout(autoAdvanceTimer) })
</script>

<style scoped>
.step-progress {
  display: flex; align-items: center; gap: 10px; margin-bottom: 16px;
  font-size: 12px; color: var(--muted);
}
.step-track {
  flex: 1; height: 6px; border-radius: 999px; background: #e5e7eb; overflow: hidden;
}
.step-track i {
  display: block; height: 100%; background: var(--theme); border-radius: 999px;
  transition: width .25s ease;
}
.actions.step { display: flex; gap: 10px; }
.actions.step .btn { flex: 1; }

.section-box { padding: 8px 0 4px; border-bottom: 1px dashed var(--border); margin-bottom: 8px; }
.section-box h3 { margin: 0 0 4px; font-size: 16px; color: var(--theme); }
.section-box p { margin: 0; color: var(--muted); font-size: 13px; line-height: 1.5; }

.img-opts { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
.img-opt {
  display: flex; flex-direction: column; gap: 6px; align-items: center;
  border: 1px solid var(--border); border-radius: 12px; padding: 10px 8px;
  cursor: pointer; background: #fff; text-align: center; font-size: 13px;
}
.img-opt input { position: absolute; opacity: 0; pointer-events: none; }
.img-opt img { width: 100%; max-height: 120px; object-fit: cover; border-radius: 8px; }
.img-opt.on { border-color: var(--theme); box-shadow: 0 0 0 2px color-mix(in srgb, var(--theme) 22%, transparent); }

.matrix-wrap { overflow-x: auto; -webkit-overflow-scrolling: touch; }
.matrix-table { width: 100%; min-width: 420px; border-collapse: collapse; font-size: 13px; }
.matrix-table th, .matrix-table td {
  border: 1px solid var(--border); padding: 8px 6px; text-align: center; white-space: nowrap;
}
.matrix-table th:first-child, .matrix-table td:first-child { text-align: left; white-space: normal; min-width: 88px; }
.matrix-table thead th { background: color-mix(in srgb, var(--theme) 8%, #fff); color: var(--muted); font-weight: 600; }

.cascade-wrap { display: grid; gap: 8px; }

.nps-box { margin-top: 4px; }
.nps-labels { display: flex; justify-content: space-between; color: var(--muted); font-size: 12px; margin-bottom: 8px; }
.nps-row { display: flex; flex-wrap: wrap; gap: 6px; }
.nps-btn {
  width: 36px; height: 36px; border-radius: 10px; border: 1px solid var(--border);
  background: #fff; cursor: pointer; font-size: 14px; color: var(--text);
}
.nps-btn.on { background: var(--theme); color: #fff; border-color: var(--theme); }
.likert-box { margin-top: 4px; }
.likert-row {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 6px;
}
.likert-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  min-height: 58px;
  padding: 8px 4px;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  background: #fff;
  color: #334155;
}
.likert-btn em { font-style: normal; font-weight: 700; font-size: 14px; }
.likert-btn span { font-size: 10px; line-height: 1.25; color: #64748b; text-align: center; }
.likert-btn.on { border-color: var(--theme); background: color-mix(in srgb, var(--theme) 12%, #fff); }
.likert-btn.on em,
.likert-btn.on span { color: var(--theme); }

.captcha-row { display: flex; gap: 10px; align-items: center; }
.captcha-row input { flex: 1; }
.captcha-row img { height: 40px; border-radius: 8px; cursor: pointer; border: 1px solid var(--border); }

@media (max-width: 480px) {
  .img-opts { grid-template-columns: 1fr; }
  .nps-btn { width: 30px; height: 30px; font-size: 13px; }
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
</style>
