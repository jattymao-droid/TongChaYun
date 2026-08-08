import request from '@/utils/request'

export function scheduleSurveyPublish(surveyId, publishAt) {
  return request({
    url: '/biz/reach/survey/schedule/' + surveyId,
    method: 'post',
    params: { publishAt }
  })
}

export function cancelSurveySchedule(surveyId) {
  return request({
    url: '/biz/reach/survey/schedule/cancel/' + surveyId,
    method: 'post'
  })
}

export function scheduleQueryPublish(queryId, publishAt) {
  return request({
    url: '/biz/reach/query/schedule/' + queryId,
    method: 'post',
    params: { publishAt }
  })
}

export function cancelQuerySchedule(queryId) {
  return request({
    url: '/biz/reach/query/schedule/cancel/' + queryId,
    method: 'post'
  })
}

export function sendPublishNotify(data) {
  return request({
    url: '/biz/reach/publish-notify',
    method: 'post',
    data
  })
}
