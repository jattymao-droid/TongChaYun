import request from '@/utils/request'

export function listSurvey(query) {
  return request({ url: '/biz/survey/list', method: 'get', params: query })
}

export function getSurvey(surveyId) {
  return request({ url: '/biz/survey/' + surveyId, method: 'get' })
}

export function addSurvey(data) {
  return request({ url: '/biz/survey', method: 'post', data })
}

export function updateSurvey(data) {
  return request({ url: '/biz/survey', method: 'put', data })
}

export function delSurvey(surveyIds) {
  return request({ url: '/biz/survey/' + surveyIds, method: 'delete' })
}

export function saveSurveyQuestions(surveyId, data) {
  return request({ url: '/biz/survey/questions/' + surveyId, method: 'put', data })
}

export function publishSurvey(surveyId) {
  return request({ url: '/biz/survey/publish/' + surveyId, method: 'post' })
}

export function offlineSurvey(surveyId) {
  return request({ url: '/biz/survey/offline/' + surveyId, method: 'post' })
}

export function listSurveyAnswers(query) {
  return request({ url: '/biz/survey/answer/list', method: 'get', params: query })
}

export function getSurveyAnswer(answerId) {
  return request({ url: '/biz/survey/answer/' + answerId, method: 'get' })
}

export function updateSurveyAnswer(answerId, data) {
  return request({ url: '/biz/survey/answer/' + answerId, method: 'put', data: data || {} })
}

export function batchUpdateSurveyAnswers(data) {
  return request({ url: '/biz/survey/answers/batch', method: 'put', data: data || {} })
}

export function getSurveyStats(surveyId) {
  return request({ url: '/biz/survey/stats/' + surveyId, method: 'get' })
}

export function getSurveyAnswerMatrix(surveyId, query) {
  return request({
    url: '/biz/survey/stats/' + surveyId + '/answers',
    method: 'get',
    params: query || {}
  })
}

export function getSurveyCrossStats(surveyId, q1, q2) {
  return request({
    url: '/biz/survey/stats/' + surveyId + '/cross',
    method: 'get',
    params: { q1, q2 }
  })
}

export function openSurveyMeta(code, accessPwd, channel) {
  const params = {}
  if (accessPwd) params.accessPwd = accessPwd
  if (channel) params.channel = channel
  return request({
    url: '/open/survey/' + code + '/meta',
    method: 'get',
    params,
    headers: { isToken: false }
  })
}

export function openSurveyEvent(code, data) {
  return request({
    url: '/open/survey/' + code + '/event',
    method: 'post',
    data: data || {},
    headers: { isToken: false }
  })
}

export function openSurveySubmit(code, data) {
  return request({ url: '/open/survey/' + code + '/submit', method: 'post', data, headers: { isToken: false } })
}

export function openSurveyDraft(code, params) {
  return request({
    url: '/open/survey/' + code + '/draft',
    method: 'get',
    params: params || {},
    headers: { isToken: false }
  })
}

export function saveOpenSurveyDraft(code, data) {
  return request({
    url: '/open/survey/' + code + '/draft',
    method: 'put',
    data: data || {},
    headers: { isToken: false }
  })
}

export function exportSurveyAnswers(surveyId, query) {
  return request({
    url: '/biz/survey/answer/export/' + surveyId,
    method: 'post',
    params: query || {},
    responseType: 'blob'
  })
}

export function exportSurveyStats(surveyId, query) {
  return request({
    url: '/biz/survey/stats/export/' + surveyId,
    method: 'post',
    params: query || {},
    responseType: 'blob'
  })
}

export function openSurveyUploadUrl(code) {
  return process.env.VUE_APP_BASE_API + '/open/survey/' + code + '/upload'
}

export function copySurvey(surveyId) {
  return request({ url: '/biz/survey/copy/' + surveyId, method: 'post' })
}

export function testSurveyWebhook(surveyId) {
  return request({ url: '/biz/survey/webhook/test/' + surveyId, method: 'post' })
}

export function listSurveyTemplates() {
  return request({ url: '/biz/survey/templates', method: 'get' })
}

export function createSurveyFromTemplate(key) {
  return request({ url: '/biz/survey/fromTemplate/' + key, method: 'post' })
}

export function transferSurvey(surveyId, targetUserId) {
  return request({ url: '/biz/survey/' + surveyId + '/transfer/' + targetUserId, method: 'put' })
}

export function listSurveyAdmins(surveyId) {
  return request({ url: '/biz/survey/' + surveyId + '/admins', method: 'get' })
}

export function searchSurveyAdminUsers(keyword) {
  return request({ url: '/biz/survey/user-search', method: 'get', params: { keyword } })
}

export function addSurveyAdmin(surveyId, data) {
  return request({ url: '/biz/survey/' + surveyId + '/admins', method: 'post', data: data || {} })
}

export function removeSurveyAdmin(surveyId, userId) {
  return request({ url: '/biz/survey/' + surveyId + '/admins/' + userId, method: 'delete' })
}

export function addSurveyBlacklist(surveyId, data) {
  return request({ url: '/biz/risk/survey/' + surveyId + '/blacklist', method: 'post', data: data || {} })
}

export function removeSurveyBlacklist(surveyId, id) {
  return request({ url: '/biz/risk/survey/' + surveyId + '/blacklist/' + id, method: 'delete' })
}

