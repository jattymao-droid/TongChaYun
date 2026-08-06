import request from '@/utils/request'

// 登录方法
export function login(username, password, code, uuid) {
  const data = {
    username,
    password,
    code,
    uuid
  }
  return request({
    url: '/login',
    headers: {
      isToken: false,
      repeatSubmit: false
    },
    method: 'post',
    data: data
  })
}

// 注册方法
export function register(data) {
  return request({
    url: '/register',
    headers: {
      isToken: false
    },
    method: 'post',
    data: data
  })
}

// 获取用户详细信息
export function getInfo() {
  return request({
    url: '/getInfo',
    method: 'get'
  })
}

// 解锁屏幕
export function unlockScreen(password) {
  return request({
    url: '/unlockscreen',
    method: 'post',
    data: { password }
  })
}

// 退出方法
export function logout() {
  return request({
    url: '/logout',
    method: 'post'
  })
}

// 获取验证码
export function getCodeImg() {
  return request({
    url: '/captchaImage',
    headers: {
      isToken: false
    },
    method: 'get',
    timeout: 20000
  })
}

/** 发送忘记密码邮箱验证码（需图形验证码） */
export function sendResetEmailCode(data) {
  const payload = typeof data === 'string' ? { email: data } : (data || {})
  return request({
    url: '/forgotPassword/sendCode',
    method: 'post',
    data: payload,
    headers: { isToken: false }
  })
}

/** 邮箱验证码重置密码 */
export function resetPasswordByEmail(data) {
  return request({
    url: '/forgotPassword/reset',
    method: 'post',
    data: data || {},
    headers: { isToken: false }
  })
}

/** 第三方登录公开配置 */
export function getOauthConfig() {
  return request({
    url: '/login/oauth/config',
    method: 'get',
    headers: { isToken: false }
  })
}

/** OAuth 一次性 ticket 换 JWT */
export function exchangeOauthTicket(ticket) {
  return request({
    url: '/login/oauth/exchange',
    method: 'post',
    data: { ticket },
    headers: {
      isToken: false,
      repeatSubmit: false
    }
  })
}
