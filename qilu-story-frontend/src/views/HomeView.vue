<template>
  <div class="book-page-content">
    <h2 class="page-title">写下你的故事开头</h2>
    <p class="page-desc">AI 将为你自动补全分支剧情，读者可通过两个选项探索不同结局。</p>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large">
      <el-form-item label="故事标题" prop="title">
        <el-input v-model="form.title" placeholder="请输入故事标题" />
      </el-form-item>

      <el-form-item label="故事开头" prop="opening">
        <el-input type="textarea" :rows="4" v-model="form.opening" placeholder="例如：夜色渐深，古堡门前的烛光忽明忽暗……" />
      </el-form-item>

      <div class="button-row">
        <el-button type="primary" :loading="loading" @click="onSubmit">开始生成</el-button>
        <el-button type="default" @click="logout">退出登录</el-button>
      </div>
    </el-form>

    <!-- Story list -->
    <div class="story-section">
      <h3 class="section-title">我的故事</h3>
      <el-table :data="storyList" style="width: 100%" v-loading="loadingList" size="small">
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="readStory(row.id)">阅读</el-button>
            <el-button type="danger" size="small" @click="confirmDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="storyList.length === 0" class="empty-hint">
        暂无故事，点击上方开始创建你的第一个故事吧！
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, inject } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useUserStore } from '../store/user';
import { createStory, getMyStories, deleteStory } from '../api';
import { useBook } from '../composables/useBook';

const book = useBook();
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

const readStory = (id) => {
  // Store storyId so StoryView can use it
  sessionStorage.setItem('qilu_current_story', id);
  book.flipForward('reading');
};

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
        sessionStorage.setItem('qilu_current_story', storyId);
        sessionStorage.setItem('qilu_story_generating', 'true');
        await loadStories();
        book.flipForward('reading');
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
  book.flipBackward();
};

onMounted(() => {
  loadStories();
});
</script>

<style scoped>
.book-page-content {
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  height: 100%;
  overflow-y: auto;
}

.page-title {
  margin: 0;
  font-size: 1.3rem;
  color: #4f3827;
  font-family: 'KaiTi', 'STKaiti', serif;
  text-align: center;
}

.page-desc {
  margin: 0 0 6px;
  font-size: 0.85rem;
  color: #8a6c56;
  text-align: center;
  line-height: 1.5;
}

.button-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.story-section {
  margin-top: 8px;
  flex: 1;
  min-height: 0;
}

.section-title {
  margin: 0 0 8px;
  font-size: 1.1rem;
  color: #4f3827;
  font-family: 'KaiTi', 'STKaiti', serif;
}

.empty-hint {
  text-align: center;
  color: #ab8e74;
  padding: 30px 0;
  font-size: 0.85rem;
}
</style>
