<template>
  <div class="book-page-content">
    <h2 class="page-title">注册账号</h2>
    <p class="page-desc">创建你的歧路之旅</p>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      size="large"
      class="form-card"
      @submit.prevent
    >
      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" placeholder="请输入用户名" />
      </el-form-item>

      <el-form-item label="密码" prop="password">
        <el-input v-model="form.password" type="password" placeholder="请输入密码" />
      </el-form-item>

      <el-form-item label="昵称" prop="nickname">
        <el-input v-model="form.nickname" placeholder="请输入昵称" />
      </el-form-item>

      <el-button type="primary" :loading="loading" @click="onSubmit" class="full-width">
        注册并登录
      </el-button>
    </el-form>

    <div class="page-link">
      <span>已有账号？</span>
      <el-button type="text" @click="goLogin">返回登录</el-button>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, computed } from 'vue';
import { ElMessage } from 'element-plus';
import { useUserStore } from '../store/user';
import { login, registerUser } from '../api';
import { useBook } from '../composables/useBook';

const book = useBook();
const store = useUserStore();
const formRef = ref(null);
const loading = ref(false);

const form = reactive({
  username: '',
  password: '',
  nickname: ''
});

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }]
};

const goLogin = () => {
  book.flipBackward();
};

const onSubmit = () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return;
    loading.value = true;
    try {
      await registerUser({
        username: form.username,
        password: form.password,
        nickname: form.nickname
      });
      ElMessage.success('注册成功，请登录');
      book.flipBackward();
    } catch (error) {
      ElMessage.error(error?.message || '注册失败，请稍后重试');
    } finally {
      loading.value = false;
    }
  });
};
</script>

<style scoped>
.book-page-content {
  padding: 24px 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: 100%;
  overflow-y: auto;
}

.page-title {
  margin: 0;
  font-size: 1.5rem;
  color: #4f3827;
  font-family: 'KaiTi', 'STKaiti', serif;
  text-align: center;
}

.page-desc {
  margin: 0;
  font-size: 0.9rem;
  color: #8a6c56;
  text-align: center;
}

.full-width {
  width: 100%;
}

.page-link {
  margin-top: 4px;
  text-align: center;
  color: #7a5b44;
  font-size: 0.9rem;
}
</style>
