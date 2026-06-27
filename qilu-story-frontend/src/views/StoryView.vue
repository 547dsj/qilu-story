<template>
  <div class="book-page-content">
    <div class="story-header">
      <div>
        <h2 class="page-title">故事阅读</h2>
        <p class="page-desc" v-if="node && !generating">当前章节：{{ node.id }}</p>
      </div>
      <el-button type="default" size="small" @click="goHome">返回主页</el-button>
    </div>

    <!-- Generating status -->
    <div v-if="generating" class="status-block">
      <div class="loading-icon">📖</div>
      <h3>故事正在生成中...</h3>
      <p>AI 正在为你创作精彩剧情，预计需要 1-2 分钟</p>
      <p class="sub-text">请稍候，会自动加载...</p>
    </div>

    <!-- Loading -->
    <div v-else-if="loading" class="status-block">
      <p>正在加载故事节点...</p>
    </div>

    <!-- Story content with internal page-flip animation -->
    <div class="story-body" v-else-if="node">
      <transition name="story-flip" mode="out-in">
        <div :key="node.id" class="story-text">
          <p>{{ node.content }}</p>
        </div>
      </transition>
    </div>

    <!-- Ending status -->
    <div class="ending-hint" v-if="node && node.isEnding">
      这是一个结局。你可以返回主页继续创作另一段故事。
    </div>

    <!-- Choice buttons -->
    <div class="choice-row" v-if="node && !node.isEnding && !generating">
      <el-button
        type="primary"
        :disabled="loading || choiceLocked"
        @click="choose('A')"
      >
        {{ node.optionALabel || '选项A' }}
      </el-button>
      <el-button
        type="default"
        :disabled="loading || choiceLocked"
        @click="choose('B')"
      >
        {{ node.optionBLabel || '选项B' }}
      </el-button>
    </div>

    <!-- Ending: return button -->
    <div class="choice-row" v-if="node && node.isEnding && !generating">
      <el-button type="primary" @click="goHome">返回主页</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { ElMessage } from 'element-plus';
import { fetchStartNode, fetchNextNode, checkStoryReady } from '../api';
import { useBook } from '../composables/useBook';

const book = useBook();
const node = ref(null);
const loading = ref(false);
const generating = ref(false);
const choiceLocked = ref(false);
let checkInterval = null;

const storyId = sessionStorage.getItem('qilu_current_story') || '';
const isGenerating = sessionStorage.getItem('qilu_story_generating') === 'true';

const checkReady = async () => {
  try {
    const ready = await checkStoryReady(storyId);
    if (ready) {
      if (checkInterval) {
        clearInterval(checkInterval);
        checkInterval = null;
      }
      sessionStorage.removeItem('qilu_story_generating');
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
  if (!node.value || node.value.isEnding || choiceLocked.value) return;
  choiceLocked.value = true;
  loading.value = true;
  try {
    node.value = await fetchNextNode(storyId, node.value.id, choice);
  } catch (error) {
    ElMessage.error(error?.message || '跳转失败，请重试');
  } finally {
    loading.value = false;
    choiceLocked.value = false;
  }
};

const goHome = () => {
  sessionStorage.removeItem('qilu_current_story');
  sessionStorage.removeItem('qilu_story_generating');
  book.flipBackward();
};

onMounted(async () => {
  if (!storyId) {
    ElMessage.warning('未选择故事');
    book.flipBackward();
    return;
  }

  if (isGenerating) {
    generating.value = true;
    ElMessage.info('故事正在生成中，请稍候...');
    checkInterval = setInterval(checkReady, 2000);
    setTimeout(async () => {
      if (generating.value) {
        try {
          await loadStartNode();
          if (node.value && (node.value.optionALabel || node.value.optionBLabel)) {
            generating.value = false;
            sessionStorage.removeItem('qilu_story_generating');
            if (checkInterval) {
              clearInterval(checkInterval);
              checkInterval = null;
            }
          }
        } catch (e) {}
      }
    }, 10000);
  } else {
    await loadStartNode();
  }
});

onUnmounted(() => {
  if (checkInterval) {
    clearInterval(checkInterval);
    checkInterval = null;
  }
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

.story-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  flex-wrap: wrap;
}

.page-title {
  margin: 0;
  font-size: 1.2rem;
  color: #4f3827;
  font-family: 'KaiTi', 'STKaiti', serif;
}

.page-desc {
  margin: 2px 0 0;
  font-size: 0.8rem;
  color: #8a6c56;
}

.status-block {
  text-align: center;
  padding: 40px 20px;
  color: #8a6c56;
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.loading-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.status-block h3 {
  margin: 0 0 8px;
  color: #4f3827;
}

.status-block p {
  margin: 0;
  font-size: 0.9rem;
}

.sub-text {
  font-size: 0.8rem !important;
  opacity: 0.7;
}

.story-body {
  flex: 1;
  min-height: 120px;
  position: relative;
  perspective: 1200px;
}

.story-text {
  white-space: pre-wrap;
  line-height: 1.9;
  font-size: 15px;
  color: #3e2f22;
  padding: 8px 0;
}

/* Internal story node flip animation */
.story-flip-enter-active {
  animation: story-flip-in 0.5s cubic-bezier(0.4, 0, 0.2, 1);
  transform-origin: left center;
}

.story-flip-leave-active {
  animation: story-flip-out 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  transform-origin: left center;
}

@keyframes story-flip-in {
  0% {
    transform: rotateY(-30deg);
    opacity: 0;
  }
  100% {
    transform: rotateY(0deg);
    opacity: 1;
  }
}

@keyframes story-flip-out {
  0% {
    transform: rotateY(0deg);
    opacity: 1;
  }
  100% {
    transform: rotateY(30deg);
    opacity: 0;
  }
}

.ending-hint {
  text-align: center;
  color: #c8855a;
  padding: 12px 0;
  font-size: 0.9rem;
  font-family: 'KaiTi', 'STKaiti', serif;
}

.choice-row {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding-top: 12px;
  border-top: 1px solid rgba(150, 120, 90, 0.12);
  flex-wrap: wrap;
}
</style>
