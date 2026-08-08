import request from '@/utils/request'

export function listQuery(query) {
  return request({ url: '/biz/query/list', method: 'get', params: query })
}

export function getQuery(queryId) {
  return request({ url: '/biz/query/' + queryId, method: 'get' })
}

export function addQuery(data) {
  return request({ url: '/biz/query', method: 'post', data })
}

export function updateQuery(data) {
  return request({ url: '/biz/query', method: 'put', data })
}

export function delQuery(queryId) {
  return request({ url: '/biz/query/' + queryId, method: 'delete' })
}

export function uploadQueryExcel(queryId, file, mode = 'replace') {
  const form = new FormData()
  form.append('queryId', queryId)
  form.append('file', file)
  form.append('mode', mode)
  return request({
    url: '/biz/query/upload',
    method: 'post',
    data: form,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function exportQuery(queryId) {
  return request({
    url: '/biz/query/export/' + queryId,
    method: 'post',
    responseType: 'blob'
  })
}

export function exportQueryPdf(queryId) {
  return request({
    url: '/biz/query/exportPdf/' + queryId,
    method: 'post',
    responseType: 'blob'
  })
}

export function saveQueryFields(queryId, fields) {
  return request({ url: '/biz/query/fields/' + queryId, method: 'put', data: fields })
}

export function saveQueryPage(data) {
  return request({ url: '/biz/query/page', method: 'put', data })
}

export function publishQuery(queryId) {
  return request({ url: '/biz/query/publish/' + queryId, method: 'post' })
}

export function offlineQuery(queryId) {
  return request({ url: '/biz/query/offline/' + queryId, method: 'post' })
}

export function getQueryLink(queryId) {
  return request({ url: '/biz/query/link/' + queryId, method: 'get' })
}

export function openQueryMeta(code, accessPwd) {
  return request({
    url: '/open/query/' + code + '/meta',
    method: 'get',
    params: accessPwd ? { accessPwd } : {},
    headers: { isToken: false }
  })
}

export function openQuerySearch(code, data) {
  return request({ url: '/open/query/' + code + '/search', method: 'post', data, headers: { isToken: false } })
}

export function previewQueryMeta(queryId) {
  return request({ url: '/biz/query/preview/' + queryId + '/meta', method: 'get' })
}

export function previewQuerySearch(queryId, data) {
  return request({ url: '/biz/query/preview/' + queryId + '/search', method: 'post', data })
}

export function copyQuery(queryId) {
  return request({ url: '/biz/query/copy/' + queryId, method: 'post' })
}

export function queryFieldDist(queryId, fieldKey) {
  return request({ url: '/biz/query/dist/' + queryId, method: 'get', params: { fieldKey } })
}

export function openQueryExport(code, data) {
  return request({
    url: '/open/query/' + code + '/export',
    method: 'post',
    data,
    responseType: 'blob',
    headers: { isToken: false }
  })
}

export function openQueryExportPdf(code, data) {
  return request({
    url: '/open/query/' + code + '/exportPdf',
    method: 'post',
    data,
    responseType: 'blob',
    headers: { isToken: false }
  })
}

export function querySampleRows(queryId, limit = 5) {
  return request({ url: '/biz/query/sample/' + queryId, method: 'get', params: { limit } })
}

export function openQueryFieldDist(code, data) {
  return request({ url: '/open/query/' + code + '/dist', method: 'post', data, headers: { isToken: false } })
}

export function listQueryTemplates() {
  return request({ url: '/biz/query/templates', method: 'get' })
}

export function createQueryFromTemplate(key) {
  return request({ url: '/biz/query/fromTemplate/' + key, method: 'post' })
}

export function listQueryDatasets(queryId) {
  return request({ url: '/biz/query/' + queryId + '/datasets', method: 'get' })
}

export function uploadQueryDataset(queryId, file, opts = {}) {
  const form = new FormData()
  form.append('file', file)
  if (opts.datasetName) form.append('datasetName', opts.datasetName)
  if (opts.isPrimary != null) form.append('isPrimary', opts.isPrimary)
  form.append('mode', opts.mode || 'replace')
  return request({
    url: '/biz/query/' + queryId + '/datasets/upload',
    method: 'post',
    data: form,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function updateQueryDataset(data) {
  return request({ url: '/biz/query/datasets', method: 'put', data })
}

export function deleteQueryDataset(queryId, datasetId) {
  return request({ url: '/biz/query/' + queryId + '/datasets/' + datasetId, method: 'delete' })
}

export function listQueryRelations(queryId) {
  return request({ url: '/biz/query/' + queryId + '/relations', method: 'get' })
}

export function saveQueryRelations(queryId, relations) {
  return request({ url: '/biz/query/' + queryId + '/relations', method: 'put', data: relations })
}

export function materializeQueryJoin(queryId) {
  return request({ url: '/biz/query/' + queryId + '/materialize', method: 'post' })
}


export function listQueryAccessLogs(queryId, params) {
  return request({ url: '/biz/query/' + queryId + '/access-logs', method: 'get', params })
}

export function transferQuery(queryId, targetUserId) {
  return request({ url: '/biz/query/' + queryId + '/transfer/' + targetUserId, method: 'put' })
}

export function listQueryAdmins(queryId) {
  return request({ url: '/biz/query/' + queryId + '/admins', method: 'get' })
}

export function searchQueryAdminUsers(keyword) {
  return request({ url: '/biz/query/user-search', method: 'get', params: { keyword } })
}

export function addQueryAdmin(queryId, data) {
  return request({ url: '/biz/query/' + queryId + '/admins', method: 'post', data })
}

export function removeQueryAdmin(queryId, userId) {
  return request({ url: '/biz/query/' + queryId + '/admins/' + userId, method: 'delete' })
}

