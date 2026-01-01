import client from './client';

export function accountRegister(data) {
  return client.post('/userInfo/account/register', data);
}

export function accountLogin(data) {
  return client.post('/userInfo/account/login', data);
}

export function sendEmailCode(data) {
  return client.post('/userInfo/email/sendCode', data);
}

export function emailLogin(data) {
  return client.post('/userInfo/email/login', data);
}

export function fetchProfile() {
  return client.get('/userInfo/getLoginUserInfo');
}

export function mockMiniLogin(data) {
  return client.post('/userInfo/mini/login', data);
}
