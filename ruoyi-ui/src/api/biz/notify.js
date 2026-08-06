import request from '@/utils/request'

export function listBizNotify(query) {
  return request({ url: '/biz/notify/list', method: 'get', params: query })
}

export function listBizNotifyTop() {
  return request({ url: '/biz/notify/listTop', method: 'get' })
}

export function unreadBizNotifyCount() {
  return request({ url: '/biz/notify/unreadCount', method: 'get' })
}

export function readBizNotify(notifyId) {
  return request({ url: '/biz/notify/read/' + notifyId, method: 'put' })
}

export function readAllBizNotify() {
  return request({ url: '/biz/notify/readAll', method: 'put' })
}
