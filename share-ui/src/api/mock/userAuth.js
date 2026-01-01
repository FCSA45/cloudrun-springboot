import request from '@/utils/request'

export function accountRegister(data) {
  return request({
    url: '/userInfo/account/register',
    method: 'post',
    headers: { isToken: false },
    data
  })
}

export function accountLogin(data) {
  return request({
    url: '/userInfo/account/login',
    method: 'post',
    headers: { isToken: false },
    data
  })
}

export function emailSendCode(data) {
  return request({
    url: '/userInfo/email/sendCode',
    method: 'post',
    headers: { isToken: false },
    data
  })
}

export function emailLogin(data) {
  return request({
    url: '/userInfo/email/login',
    method: 'post',
    headers: { isToken: false },
    data
  })
}

export function miniLogin(data) {
  return request({
    url: '/userInfo/mini/login',
    method: 'post',
    headers: { isToken: false },
    data
  })
}

export function getLoginUserInfoWithToken(token) {
  return request({
    url: '/userInfo/getLoginUserInfo',
    method: 'get',
    headers: {
      isToken: false,
      Authorization: token ? `Bearer ${token}` : undefined
    }
  })
}
