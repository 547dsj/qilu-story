<template>
  <div class="page-shell">
    <el-card class="panel">
      <h2 class="panel-title">歧路·互动小说</h2>
      <p class="text-warm">在这里，输入故事开头，AI 将自动生成分支剧情。</p>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        size="large"
        class="form-card"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" />
        </el-form-item>

        <el-form-item v-if="mode === 'register'" label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>

        <el-button type="primary" :loading="loading" @click="onSubmit" class="full-width">
          {{ mode === 'login' ? '登录' : '注册并登录' }}
        </el-button>

        <div style="margin-top: 16px; text-align: center; color: #7a5b44;">
          <span>{{ mode === 'login' ? '还没有账号？' : '已有账号？' }}</span>
          <el-button type="text" @click="toggleMode">
            {{ mode === 'login' ? '去注册' : '返回登录' }}
          </el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useUserStore } from '../store/user';
import { login, registerUser } from '../api';

const router = useRouter();
const store = useUserStore();
const formRef = ref(null);
const mode = ref('login');
const loading = ref(false);

const form = reactive({
  username: '',
  password: '',
  nickname: ''
});

const rules = computed(() => ({
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  nickname: mode.value === 'register'
    ? [{ required: true, message: '请输入昵称', trigger: 'blur' }]
    : []
}));

const toggleMode = () => {
  mode.value = mode.value === 'login' ? 'register' : 'login';
  form.password = '';
  form.nickname = '';
};

const onSubmit = () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return;
    loading.value = true;
    try {
      if (mode.value === 'register') {
        await registerUser({
          username: form.username,
          password: form.password,
          nickname: form.nickname
        });
      }
      const data = await login({
        username: form.username,
        password: form.password
      });
      store.setAuth(data.token, data.userInfo);
      router.push('/home');
    } catch (error) {
      ElMessage.error(error?.message || '登录失败，请稍后重试');
    } finally {
      loading.value = false;
    }
  });
};
</script>

<style scoped>
.full-width {
  width: 100%;
}
</style>
