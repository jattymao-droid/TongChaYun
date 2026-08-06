import request from '@/utils/request'

/** Admin: users with query/survey counts */
export function listUserProjects(query) {
  return request({ url: '/biz/user-projects/list', method: 'get', params: query })
}
