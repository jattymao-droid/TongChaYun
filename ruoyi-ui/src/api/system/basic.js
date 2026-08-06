import request from '@/utils/request'

/** ����վ����Ϣ����¼ҳ���ã� */
export function getSiteInfo() {
  return request({
    url: '/system/basic/site',
    method: 'get',
    headers: { isToken: false }
  })
}

export function getBasicSettings() {
  return request({
    url: '/system/basic',
    method: 'get'
  })
}

export function saveBasicSettings(data) {
  return request({
    url: '/system/basic',
    method: 'put',
    data
  })
}

export function testMail(to) {
  return request({
    url: '/system/basic/testMail',
    method: 'post',
    data: { to }
  })
}

export function sendRegisterEmailCode(data) {
  const payload = typeof data === 'string' ? { email: data } : (data || {})
  return request({
    url: '/system/basic/sendRegisterCode',
    method: 'post',
    data: payload,
    headers: { isToken: false }
  })
}
