import request from '@/utils/request'

export function listQueryRevisions(queryId) {
  return request({ url: '/biz/version/query/' + queryId + '/revisions', method: 'get' })
}

export function rollbackQueryRevision(queryId, revId) {
  return request({ url: '/biz/version/query/' + queryId + '/revisions/' + revId + '/rollback', method: 'post' })
}

export function listSurveyRevisions(surveyId) {
  return request({ url: '/biz/version/survey/' + surveyId + '/revisions', method: 'get' })
}

export function rollbackSurveyRevision(surveyId, revId) {
  return request({ url: '/biz/version/survey/' + surveyId + '/revisions/' + revId + '/rollback', method: 'post' })
}

export function listProjectAudit(projectType, projectId) {
  return request({ url: '/biz/version/audit', method: 'get', params: { projectType, projectId } })
}

export function listPublishRequests(query) {
  return request({ url: '/biz/version/publish/requests', method: 'get', params: query })
}

export function approvePublishRequest(requestId, remark) {
  return request({ url: '/biz/version/publish/requests/' + requestId + '/approve', method: 'post', data: { remark } })
}

export function rejectPublishRequest(requestId, remark) {
  return request({ url: '/biz/version/publish/requests/' + requestId + '/reject', method: 'post', data: { remark } })
}
