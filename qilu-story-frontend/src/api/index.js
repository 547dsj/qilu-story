import axios from 'axios';
import router from '../router';
import { useUserStore } from '../store/user';

const client = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 180000
});

// 请求拦截器 - 自动添加 token
client.interceptors.request.use((config) => {
  // 直接从 localStorage 读取 token（使用正确的 key）
  const token = localStorage.getItem('qilu_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

client.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const status = error?.response?.status;
    if (status === 401) {
      const store = useUserStore();
      store.clearAuth();
      router.push('/');
    }
    return Promise.reject(error);
  }
);

const handleResponse = async (promise) => {
  const res = await promise;
  if (res.code !== 200) {
    throw new Error(res.message || '接口返回异常');
  }
  return res.data;
};

export const login = (payload) => handleResponse(client.post('/auth/login', payload));
export const registerUser = (payload) => handleResponse(client.post('/auth/register', payload));
export const createStory = (payload) => handleResponse(client.post('/stories', payload));
export const fetchStartNode = (storyId) => handleResponse(client.get(`/stories/${storyId}/start`));
export const fetchNextNode = (storyId, nodeId, choice) =>
  handleResponse(client.get(`/stories/${storyId}/nodes/${nodeId}/next`, { params: { choice } }));

// 获取我的故事列表
export const getMyStories = () => handleResponse(client.get('/stories/my-stories'));

// 删除故事
export const deleteStory = (id) => handleResponse(client.delete(`/stories/${id}`));

// 检查故事是否已生成完成
export const checkStoryReady = (storyId) => {
    return handleResponse(client.get(`/stories/${storyId}/ready`));
};

