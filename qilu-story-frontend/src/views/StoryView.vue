<template>
  <div class="page-shell">
    <el-card class="panel">
      <div style="display: flex; justify-content: space-between; align-items: center; gap: 12px; flex-wrap: wrap; margin-bottom: 14px;">
        <div>
          <h2 class="panel-title">故事阅读</h2>
          <p class="text-warm" v-if="node && !generating">当前章节：{{ node.id }}</p>
        </div>
        <el-button type="default" @click="goHome">返回首页</el-button>
      </div>

      <!-- 生成中状态 -->
      <div v-if="generating" style="text-align: center; padding: 60px;">
        <div style="font-size: 48px; margin-bottom: 20px;">📖</div>
        <h3>故事正在生成中...</h3>
        <p style="color: #8a6c56;">AI 正在为你创作精彩剧情，预计需要 1-2 分钟</p>
        <p style="color: #8a6c56; font-size: 14px;">请稍候，会自动加载...</p>
      </div>

      <!-- 加载中状态 -->
      <div v-else-if="loading" style="min-height: 160px; display: flex; align-items: center; justify-content: center; color: #8a6c56;">
        正在加载故事节点...
      </div>

      <!-- 故事内容 -->
      <transition name="fade" mode="out-in" v-else-if="node">
        <div key="story-node" class="story-content">
          <p style="white-space: pre-wrap; line-height: 1.8; font-size: 16px;">{{ node.content }}</p>
        </div>
      </transition>

      <!-- 结局状态 -->
      <div class="story-status" v-if="node && node.isEnding">
        这是一个结局。你可以返回首页继续创作另一段故事。
      </div>

      <!-- 选项按钮 -->
      <div class="story-footer" v-if="node && !node.isEnding && !generating">
        <el-button
          type="primary"
          :disabled="loading || selectedChoice !== null"
          @click="choose('A')"
        >
          {{ node.optionALabel || '选项A' }}
        </el-button>
        <el-button
          type="default"
          :disabled="loading || selectedChoice !== null"
          @click="choose('B')"
        >
          {{ node.optionBLabel || '选项B' }}
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { fetchStartNode, fetchNextNode, checkStoryReady } from '../api';

const route = useRoute();
const router = useRouter();
const node = ref(null);
const loading = ref(false);
const generating = ref(false);
const selectedChoice = ref(null);
let checkInterval = null;

const storyId = route.params.id;

// 检查故事是否生成完成
const checkReady = async () => {
  try {
    const ready = await checkStoryReady(storyId);
    if (ready) {
      // 生成完成，停止轮询，加载故事
      if (checkInterval) {
        clearInterval(checkInterval);
        checkInterval = null;
      }
      generating.value = false;
      await loadStartNode();
    }
  } catch (error) {
    console.error('检查故事状态失败', error);
  }
};

const loadStartNode = async () => {
  loading.value = true;
  try {
    node.value = await fetchStartNode(storyId);
    // 检查根节点是否有选项（有选项说明故事已生成）
    if (node.value && (node.value.optionALabel || node.value.optionBLabel)) {
      generating.value = false;
    }
  } catch (error) {
    ElMessage.error(error?.message || '读取故事失败，请检查故事是否存在');
  } finally {
    loading.value = false;
  }
};

const choose = async (choice) => {
  if (!node.value || node.value.isEnding) return;
  selectedChoice.value = choice;
  loading.value = true;
  try {
    node.value = await fetchNextNode(storyId, node.value.id, choice);
  } catch (error) {
    ElMessage.error(error?.message || '跳转失败，请重试');
  } finally {
    loading.value = false;
    selectedChoice.value = null;
  }
};

const goHome = () => {
  router.push('/');
};

onMounted(async () => {
  // 检查是否是新建的故事（需要生成）
  const isGenerating = route.query.generating === 'true';
  
  if (isGenerating) {
    generating.value = true;
    ElMessage.info('故事正在生成中，请稍候...');
    // 开始轮询，每2秒检查一次
    checkInterval = setInterval(checkReady, 2000);
    // 10秒后先尝试加载一次
    setTimeout(async () => {
      if (generating.value) {
        try {
          await loadStartNode();
          if (node.value && (node.value.optionALabel || node.value.optionBLabel)) {
            generating.value = false;
            if (checkInterval) {
              clearInterval(checkInterval);
              checkInterval = null;
            }
          }
        } catch (e) {
          // 继续等待
        }
      }
    }, 10000);
  } else {
    await loadStartNode();
  }
});
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
.story-content {
  min-height: 200px;
  padding: 20px 0;
}
.story-status {
  text-align: center;
  color: #e6a23c;
  padding: 20px;
  font-size: 14px;
}
.story-footer {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}
</style>