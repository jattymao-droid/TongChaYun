import request from '@/utils/request'

export function getBizOverview() {
  return request({ url: '/biz/dashboard/overview', method: 'get' })
}
