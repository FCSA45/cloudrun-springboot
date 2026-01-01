import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

const TOKEN_KEY = 'user-web-token';

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '');
  const userInfo = ref(null);

  const isLoggedIn = computed(() => Boolean(token.value));

  function setToken(value) {
    token.value = value || '';
    if (token.value) {
      localStorage.setItem(TOKEN_KEY, token.value);
    } else {
      localStorage.removeItem(TOKEN_KEY);
    }
  }

  function setUserInfo(info) {
    userInfo.value = info;
  }

  function logout() {
    setToken('');
    setUserInfo(null);
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    setToken,
    setUserInfo,
    logout
  };
});
