<template>
  <div class="page-shell">
    <el-card class="panel">
      <h2 class="panel-title">写下你的故事开头</h2>
      <p class="text-warm">AI 将为你自动补全分支剧情，读者可通过两个选项探索不同结局。</p>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large">
        <el-form-item label="故事标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入故事标题" />
        </el-form-item>

        <el-form-item label="故事开头" prop="opening">
          <el-input type="textarea" :rows="6" v-model="form.opening" placeholder="例如：夜色渐深，古堡门前的烛光忽明忽暗……" />
        </el-form-item>

        <div style="display: flex; justify-content: space-between; gap: 12px; flex-wrap: wrap;">
          <el-button type="primary" :loading="loading" @click="onSubmit">开始生成</el-button>
          <el-button type="default" @click="logout">退出登录</el-button>
        </div>
      </el-form>
    </el-card>

    <el-card class="panel" style="margin-top: 30px;">
      <h2 class="panel-title">我的故事</h2>
      <el-table :data="storyList" style="width: 100%" v-loading="loadingList">
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="readStory(row.id)">阅读</el-button>
            <el-button type="danger" size="small" @click="confirmDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="storyList.length === 0" style="text-align: center; color: #999; padding: 40px;">
        暂无故事，点击上方开始创建你的第一个故事吧！
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useUserStore } from '../store/user';
import { createStory, getMyStories, deleteStory } from '../api';

const router = useRouter();
const store = useUserStore();
const formRef = ref(null);
const loading = ref(false);
const loadingList = ref(false);
const storyList = ref([]);

const form = reactive({
  title: '',
  opening: ''
});

const rules = {
  title: [{ required: true, message: '请输入故事标题', trigger: 'blur' }],
  opening: [{ required: true, message: '请输入故事开头', trigger: 'blur' }]
};

// 加载故事列表
const loadStories = async () => {
  loadingList.value = true;
  try {
    const res = await getMyStories();
    storyList.value = res.data || res || [];
  } catch (error) {
    console.error('加载故事列表失败', error);
  } finally {
    loadingList.value = false;
  }
};

// 阅读故事
const readStory = (id) => {
  router.push(`/story/${id}`);
};

// 确认删除
const confirmDelete = (story) => {
  ElMessageBox.confirm(
    `确定要删除故事《${story.title}》吗？删除后无法恢复。`,
    '确认删除',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(async () => {
    try {
      await deleteStory(story.id);
      ElMessage.success('删除成功');
      loadStories();
    } catch (error) {
      ElMessage.error('删除失败');
    }
  }).catch(() => {});
};

const onSubmit = () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return;
    loading.value = true;
    try {
      const res = await createStory({
        title: form.title,
        opening: form.opening
      });
      let storyId;
      if (typeof res === 'number') {
        storyId = res;
      } else {
        storyId = res.data?.id || res.data?.storyId || res.id || res.storyId;
      }
      
      if (storyId) {
        form.title = '';
        form.opening = '';
        await loadStories();
        router.push(`/story/${storyId}`);
      } else {
        console.error('返回数据中没有故事ID:', res);
        ElMessage.error('创建成功，但获取故事ID失败');
      }
    } catch (error) {
      ElMessage.error(error?.message || '创建故事失败，请稍后重试');
    } finally {
      loading.value = false;
    }
  });
};

const logout = () => {
  store.clearAuth();
  router.push('/');
};

// 页面加载时获取故事列表
onMounted(() => {
  loadStories();
});
</script>