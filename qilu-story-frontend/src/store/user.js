import { defineStore } from 'pinia';

const savedToken = localStorage.getItem('qilu_token') || '';
const savedUser = localStorage.getItem('qilu_user');

export const useUserStore = defineStore('user', {
  state: () => ({
    token: savedToken,
    userInfo: savedUser ? JSON.parse(savedUser) : null
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token)
  },
  actions: {
    setAuth(token, userInfo) {
      this.token = token;
      this.userInfo = userInfo;
      localStorage.setItem('qilu_token', token);
      localStorage.setItem('qilu_user', JSON.stringify(userInfo));
    },
    clearAuth() {
      this.token = '';
      this.userInfo = null;
      localStorage.removeItem('qilu_token');
      localStorage.removeItem('qilu_user');
    }
  }
});
