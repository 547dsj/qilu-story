<template>
  <div class="book-app" @click="onGlobalClick">
    <!-- ========== 开场动画：镜头推近 + 印章发光 ========== -->
    <div class="intro-overlay" v-if="introPhase !== 'done'">
      <div class="camera-zoom" :class="'intro-' + introPhase">
        <div class="book-cover-3d">
          <div class="cover-front">
            <div class="seal-container">
              <div class="seal-char qi" :class="{ 'glow-up': introPhase === 'glow' || introPhase === 'glow-done' }">歧</div>
              <div class="seal-char lu" :class="{ 'glow-down': introPhase === 'glow' || introPhase === 'glow-done' }">路</div>
            </div>
            <div class="book-decoration">
              <div class="book-spine-line"></div>
              <div class="book-corner tl"></div>
              <div class="book-corner tr"></div>
              <div class="book-corner bl"></div>
              <div class="book-corner br"></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ========== 书本主体 ========== -->
    <div class="book-scene" v-show="introPhase === 'done'">
      <div class="book-container">
        <div class="book-shadow"></div>
        <div class="book" ref="bookRef">
          <!-- 左页 -->
          <div class="page page-left" :class="{ 'is-turning': isTurning && turningDir === 'backward' }">
            <div class="page-content page-content-left">
              <div class="page-inner" v-if="leftPageContent">
                <component :is="leftPageContent.comp" v-bind="leftPageContent.props" @navigate="onNavigate" />
              </div>
              <div class="page-inner page-decorative" v-else>
                <div class="decorative-content">
                  <div class="deco-seal">歧路</div>
                  <div class="deco-line"></div>
                  <div class="deco-text">互动小说</div>
                </div>
              </div>
            </div>
          </div>
          <!-- 右页 -->
          <div class="page page-right" :class="{ 'is-turning': isTurning && turningDir === 'forward' }">
            <div class="page-content page-content-right">
              <div class="page-inner" v-if="rightPageContent">
                <component :is="rightPageContent.comp" v-bind="rightPageContent.props" @navigate="onNavigate" />
              </div>
            </div>
          </div>
          <!-- 翻页动画层（前进） -->
          <div class="page page-flip page-flip-forward" v-if="isTurning && turningDir === 'forward'" :class="{ 'animate-flip': isTurning && turningDir === 'forward' }">
            <div class="page-content page-content-right">
              <div class="page-inner">
                <component :is="oldRightComp" v-bind="oldRightProps" @navigate="() => {}" />
              </div>
            </div>
            <div class="page-back"></div>
          </div>
          <!-- 翻页动画层（后退） -->
          <div class="page page-flip page-flip-backward" v-if="isTurning && turningDir === 'backward'" :class="{ 'animate-flip-back': isTurning && turningDir === 'backward' }">
            <div class="page-content page-content-left">
              <div class="page-inner">
                <component :is="oldLeftComp" v-bind="oldLeftProps" @navigate="() => {}" />
              </div>
            </div>
            <div class="page-back"></div>
          </div>
        </div>
        <div class="book-edge"></div>
      </div>
    </div>

    <!-- 页码指示器 -->
    <div class="page-indicator" v-if="introPhase === 'done'">
      <span class="page-dot" v-for="i in totalSpreads" :key="i" :class="{ active: i - 1 === currentSpread }"></span>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, h, markRaw, shallowRef } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../store/user'
import { login, registerUser, createStory, fetchStartNode, fetchNextNode, getMyStories, checkStoryReady, deleteStory } from '../api'

// ========== Store ==========
const userStore = useUserStore()

// ========== 开场动画状态 ==========
const introPhase = ref('zoom')

// ========== 书本状态 ==========
const bookRef = ref(null)
const currentSpread = ref(0)
const isTurning = ref(false)
const turningDir = ref('forward')

const oldRightComp = shallowRef(null)
const oldRightProps = ref({})
const oldLeftComp = shallowRef(null)
const oldLeftProps = ref({})

// ========== 小说状态 ==========
const currentNode = ref(null)
const storyId = ref(null)
const generating = ref(false)
const loadingStory = ref(false)
const storyTurning = ref(false)
const oldStoryNode = ref(null)
let checkInterval = null

// ========== 故事列表 ==========
const storyList = ref([])
const loadingList = ref(false)

const totalSpreads = computed(() => 4)

// ========== 表单状态 ==========
const loginForm = reactive({ username: '', password: '' })
const loginLoading = ref(false)

const registerForm = reactive({ username: '', password: '', nickname: '' })
const registerLoading = ref(false)

const storyForm = reactive({ title: '', opening: '' })
const storyLoading = ref(false)

// ========== 内联子组件 ==========
const LoginPage = markRaw({
  props: ['form', 'loading', 'spread'],
  emits: ['navigate'],
  setup(props, { emit }) {
    return () => h('div', { class: 'book-page-content' }, [
      h('h3', { class: 'page-title' }, '登录'),
      h('p', { class: 'page-subtitle' }, '输入账号密码继续你的故事'),
      h('div', { class: 'page-form' }, [
        h('div', { class: 'field' }, [
          h('label', { class: 'field-label' }, '用户名'),
          h('input', {
            class: 'field-input', type: 'text',
            value: props.form.username,
            onInput: e => props.form.username = e.target.value,
            placeholder: '请输入用户名'
          })
        ]),
        h('div', { class: 'field' }, [
          h('label', { class: 'field-label' }, '密码'),
          h('input', {
            class: 'field-input', type: 'password',
            value: props.form.password,
            onInput: e => props.form.password = e.target.value,
            placeholder: '请输入密码',
            onKeydown: e => { if (e.key === 'Enter') emit('navigate', 'do-login') }
          })
        ]),
        h('button', {
          class: 'page-btn page-btn-primary',
          onClick: () => emit('navigate', 'do-login')
        }, '登录'),
        h('div', { class: 'page-link-row' }, [
          h('span', { class: 'page-link-text' }, '还没有账号？'),
          h('button', {
            class: 'page-link',
            onClick: () => emit('navigate', 'go-register')
          }, '去注册 →')
        ])
      ])
    ])
  }
})

const RegisterPage = markRaw({
  props: ['form', 'loading', 'spread'],
  emits: ['navigate'],
  setup(props, { emit }) {
    return () => h('div', { class: 'book-page-content' }, [
      h('h3', { class: 'page-title' }, '注册'),
      h('p', { class: 'page-subtitle' }, '创建你的专属故事账号'),
      h('div', { class: 'page-form' }, [
        h('div', { class: 'field' }, [
          h('label', { class: 'field-label' }, '用户名'),
          h('input', {
            class: 'field-input', type: 'text',
            value: props.form.username,
            onInput: e => props.form.username = e.target.value,
            placeholder: '请输入用户名'
          })
        ]),
        h('div', { class: 'field' }, [
          h('label', { class: 'field-label' }, '昵称'),
          h('input', {
            class: 'field-input', type: 'text',
            value: props.form.nickname,
            onInput: e => props.form.nickname = e.target.value,
            placeholder: '请输入昵称'
          })
        ]),
        h('div', { class: 'field' }, [
          h('label', { class: 'field-label' }, '密码'),
          h('input', {
            class: 'field-input', type: 'password',
            value: props.form.password,
            onInput: e => props.form.password = e.target.value,
            placeholder: '请输入密码',
            onKeydown: e => { if (e.key === 'Enter') emit('navigate', 'do-register') }
          })
        ]),
        h('button', {
          class: 'page-btn page-btn-primary',
          onClick: () => emit('navigate', 'do-register')
        }, '注册并返回登录'),
        h('div', { class: 'page-link-row' }, [
          h('span', { class: 'page-link-text' }, '已有账号？'),
          h('button', {
            class: 'page-link',
            onClick: () => emit('navigate', 'back-to-login')
          }, '← 返回登录')
        ])
      ])
    ])
  }
})

const HomePage = markRaw({
  props: ['form', 'loading', 'storyList', 'loadingList', 'spread'],
  emits: ['navigate'],
  setup(props, { emit }) {
    return () => h('div', { class: 'book-page-content' }, [
      h('h3', { class: 'page-title' }, '创作故事'),
      h('p', { class: 'page-subtitle' }, 'AI为你自动生成分支剧情'),
      h('div', { class: 'page-form' }, [
        h('div', { class: 'field' }, [
          h('label', { class: 'field-label' }, '故事标题'),
          h('input', {
            class: 'field-input', type: 'text',
            value: props.form.title,
            onInput: e => props.form.title = e.target.value,
            placeholder: '请输入故事标题'
          })
        ]),
        h('div', { class: 'field' }, [
          h('label', { class: 'field-label' }, '故事开头'),
          h('textarea', {
            class: 'field-textarea',
            value: props.form.opening,
            onInput: e => props.form.opening = e.target.value,
            placeholder: '例如：夜色渐深，古堡门前的烛光忽明忽暗...',
            rows: 4
          })
        ]),
        h('button', {
          class: 'page-btn page-btn-primary',
          onClick: () => emit('navigate', 'create-story')
        }, '开始生成')
      ]),
      h('div', { class: 'page-story-list' }, [
        h('h4', { class: 'page-subtitle', style: 'margin-top: 8px;' }, '我的故事'),
        props.loadingList
          ? h('p', { class: 'page-loading-text' }, '加载中...')
          : props.storyList && props.storyList.length > 0
            ? props.storyList.map(s => h('div', { class: 'story-item', key: s.id }, [
                h('span', { class: 'story-item-title' }, s.title || '未命名故事'),
                h('div', { class: 'story-item-actions' }, [
                  h('button', {
                    class: 'page-btn page-btn-sm',
                    onClick: () => emit('navigate', 'read-story', { id: s.id })
                  }, '阅读'),
                  h('button', {
                    class: 'page-btn page-btn-sm',
                    style: { marginLeft: '6px' },
                    onClick: (e) => { e.stopPropagation(); emit('navigate', 'delete-story', { id: s.id, title: s.title }) }
                  }, '删除')
                ])
              ]))
            : h('p', { class: 'page-empty-text' }, '暂无故事，创建一个吧')
      ]),
      h('button', {
        class: 'page-btn page-btn-outline',
        style: 'margin-top: 12px;',
        onClick: () => emit('navigate', 'logout')
      }, '退出登录')
    ])
  }
})

const StoryPage = markRaw({
  props: ['node', 'generating', 'loading', 'flipping', 'oldNode', 'spread'],
  emits: ['navigate'],
  setup(props, { emit }) {
    return () => {
      if (props.generating) {
        return h('div', { class: 'book-page-content story-generating' }, [
          h('div', { class: 'generating-icon' }, '📖'),
          h('h3', { class: 'page-title' }, '故事生成中...'),
          h('p', { class: 'page-subtitle' }, 'AI 正在为你创作精彩剧情，预计需要 1-2 分钟')
        ])
      }
      if (props.loading || !props.node) {
        return h('div', { class: 'book-page-content story-loading' }, [
          h('p', { class: 'page-subtitle' }, '加载中...')
        ])
      }
      const n = props.node
      const old = props.oldNode

      // 翻页动画状态：同时渲染旧内容和新内容
      if (props.flipping && old) {
        return h('div', { class: 'book-page-content story-node story-flipping' }, [
          h('div', { class: 'story-text story-text-out' }, old.content),
          h('div', { class: 'story-text story-text-in' }, n.content || '')
        ])
      }

      return h('div', { class: 'book-page-content story-node' }, [
        h('div', { class: 'story-text' }, n.content),
        (n.isEnding || (!n.optionALabel && !n.optionBLabel))
          ? h('div', { class: 'story-ending-section' }, [
              h('p', { class: 'story-ending-tag' }, n.isEnding ? '—— 结局 ——' : '—— 完 ——'),
              h('p', { class: 'page-subtitle', style: 'margin: 8px 0;' }, '故事到此结束'),
              h('button', {
                class: 'page-btn page-btn-primary',
                onClick: () => emit('navigate', 'back-to-home')
              }, '返回主页')
            ])
          : h('div', { class: 'story-choices' }, [
              h('button', {
                class: 'page-btn page-btn-choice',
                onClick: () => emit('navigate', 'choose-option', { choice: 'A' })
              }, n.optionALabel || '选项 A'),
              h('button', {
                class: 'page-btn page-btn-choice',
                onClick: () => emit('navigate', 'choose-option', { choice: 'B' })
              }, n.optionBLabel || '选项 B')
            ])
      ])
    }
  }
})

// ========== 页面定义 ==========
const pageDefs = [
  { type: 'login' },
  { type: 'register' },
  { type: 'home' },
  { type: 'story' }
]

// ========== Spread 内容 ==========
const spreadContent = computed(() => {
  const s = currentSpread.value
  if (s === 0) return { left: null, right: pageDefs[0] }
  if (s === 1) return { left: pageDefs[0], right: pageDefs[1] }
  if (s === 2) return { left: pageDefs[1], right: pageDefs[2] }
  if (s === 3) return { left: pageDefs[2], right: pageDefs[3] }
  return { left: null, right: null }
})

const getPageComponent = (def) => {
  switch (def.type) {
    case 'login':
      return { comp: LoginPage, props: { form: loginForm, loading: loginLoading.value, spread: currentSpread.value } }
    case 'register':
      return { comp: RegisterPage, props: { form: registerForm, loading: registerLoading.value, spread: currentSpread.value } }
    case 'home':
      return { comp: HomePage, props: { form: storyForm, loading: storyLoading.value, storyList: storyList.value, loadingList: loadingList.value, spread: currentSpread.value } }
    case 'story':
      return { comp: StoryPage, props: { node: currentNode.value, generating: generating.value, loading: loadingStory.value, flipping: storyTurning.value, oldNode: oldStoryNode.value, spread: currentSpread.value } }
    default:
      return null
  }
}

const leftPageContent = computed(() => {
  const def = spreadContent.value.left
  return def ? getPageComponent(def) : null
})

const rightPageContent = computed(() => {
  const def = spreadContent.value.right
  return def ? getPageComponent(def) : null
})

// ========== 翻页逻辑 ==========
const pageTurnDuration = 800

function flipForward(targetSpread) {
  if (isTurning.value) return
  if (targetSpread === currentSpread.value) return

  const rc = rightPageContent.value
  if (rc) {
    oldRightComp.value = rc.comp
    oldRightProps.value = rc.props
  }

  turningDir.value = 'forward'
  isTurning.value = true

  setTimeout(() => { currentSpread.value = targetSpread }, pageTurnDuration * 0.45)
  setTimeout(() => {
    isTurning.value = false
    oldRightComp.value = null
    oldRightProps.value = {}
  }, pageTurnDuration)
}

function flipBackward(targetSpread) {
  if (isTurning.value) return
  if (targetSpread === currentSpread.value) return

  const lc = leftPageContent.value
  if (lc) {
    oldLeftComp.value = lc.comp
    oldLeftProps.value = lc.props
  }

  turningDir.value = 'backward'
  isTurning.value = true

  setTimeout(() => { currentSpread.value = targetSpread }, pageTurnDuration * 0.45)
  setTimeout(() => {
    isTurning.value = false
    oldLeftComp.value = null
    oldLeftProps.value = {}
  }, pageTurnDuration)
}

// ========== 导航处理 ==========
async function onNavigate(action, payload) {
  const s = currentSpread.value

  switch (action) {
    case 'go-register':
      if (s === 0) flipForward(1)
      break

    case 'do-login':
      loginLoading.value = true
      try {
        const data = await login({ username: loginForm.username, password: loginForm.password })
        userStore.setAuth(data.token, data.userInfo)
        loginLoading.value = false
        await loadStories()
        flipForward(2)
      } catch (error) {
        loginLoading.value = false
        ElMessage.error(error?.message || '操作失败，请重试')
      }
      break

    case 'do-register':
      registerLoading.value = true
      try {
        await registerUser({
          username: registerForm.username,
          password: registerForm.password,
          nickname: registerForm.nickname || registerForm.username
        })
        loginForm.username = registerForm.username
        loginForm.password = registerForm.password
        registerLoading.value = false
        flipBackward(0)
      } catch (error) {
        registerLoading.value = false
        ElMessage.error(error?.message || '注册失败，请重试')
      }
      break

    case 'back-to-login':
      flipBackward(0)
      break

    case 'create-story':
      if (!storyForm.title.trim() || !storyForm.opening.trim()) {
        ElMessage.warning('请填写故事标题和开头')
        return
      }
      storyLoading.value = true
      try {
        const res = await createStory({ title: storyForm.title, opening: storyForm.opening })
        let sid = typeof res === 'number' ? res : (res.data?.id || res.data?.storyId || res.id || res.storyId)
        if (sid) {
          storyId.value = sid
          storyForm.title = ''
          storyForm.opening = ''
          storyLoading.value = false
          flipForward(3)
          await loadStoryAfterFlip()
        } else {
          storyLoading.value = false
          ElMessage.error('创建成功，但获取故事ID失败')
        }
      } catch (error) {
        storyLoading.value = false
        ElMessage.error(error?.message || '创建故事失败')
      }
      break

    case 'read-story':
      storyId.value = payload.id
      flipForward(3)
      await loadStoryAfterFlip()
      break

    case 'logout':
      userStore.clearAuth()
      storyList.value = []
      currentNode.value = null
      storyId.value = null
      flipBackward(0)
      break

    case 'choose-option':
      if (!currentNode.value || currentNode.value.isEnding) return
      await chooseStoryOption(payload.choice)
      break

    case 'delete-story':
      try {
        await ElMessageBox.confirm('确定要删除故事"' + (payload.title || '未命名') + '"吗？', '确认删除', {
          confirmButtonText: '删除',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await deleteStory(payload.id)
        ElMessage.success('删除成功')
        await loadStories()
      } catch (e) {
        if (e !== 'cancel' && e !== 'close') {
          ElMessage.error(e?.message || '删除失败')
        }
      }
      break

    case 'back-to-home':
      currentNode.value = null
      flipBackward(2)
      await loadStories()
      break
  }
}

// ========== 小说相关 ==========
function onAuthExpired() {
  storyList.value = []
  currentNode.value = null
  storyId.value = null
  currentSpread.value = 0
  isTurning.value = false
}

async function loadStoryAfterFlip() {
  await new Promise(r => setTimeout(r, pageTurnDuration + 200))
  await loadStories()
  await loadStoryNode()
}

async function loadStories() {
  console.log('loadStories called, token:', localStorage.getItem('qilu_token')?.substring(0,20))
  loadingList.value = true
  try {
    const res = await getMyStories()
    console.log('loadStories result:', JSON.stringify(res))
    storyList.value = Array.isArray(res) ? res : (res.data || [])
  } catch (e) {
    console.error('加载故事列表失败', e)
  } finally {
    loadingList.value = false
  }
}

async function loadStoryNode() {
  if (!storyId.value) return
  loadingStory.value = true
  try {
    const node = await fetchStartNode(storyId.value)
    if (node && (node.optionALabel || node.optionBLabel)) {
      currentNode.value = node
      loadingStory.value = false
      return
    }
    generating.value = true
    ElMessage.info('故事正在生成中，请稍候...')
    checkInterval = setInterval(async () => {
      try {
        const ready = await checkStoryReady(storyId.value)
        if (ready) {
          clearInterval(checkInterval)
          checkInterval = null
          generating.value = false
          const n = await fetchStartNode(storyId.value)
          currentNode.value = n
          loadingStory.value = false
        }
      } catch (e) { /* continue */ }
    }, 3000)
    setTimeout(() => {
      if (checkInterval) {
        clearInterval(checkInterval)
        checkInterval = null
        generating.value = false
        loadingStory.value = false
        ElMessage.warning('故事生成超时，请刷新重试')
      }
    }, 180000)
  } catch (error) {
    loadingStory.value = false
    ElMessage.error(error?.message || '加载故事失败')
  }
}

async function chooseStoryOption(choice) {
  if (!currentNode.value || !storyId.value) return
  // 保存旧内容，触发翻页动画
  oldStoryNode.value = currentNode.value
  storyTurning.value = true
  loadingStory.value = true
  try {
    const node = await fetchNextNode(storyId.value, currentNode.value.id, choice)
    // 等待翻页动画过半时切换内容
    await new Promise(r => setTimeout(r, 350))
    currentNode.value = node
    // 动画结束后清除翻转状态
    await new Promise(r => setTimeout(r, 400))
    storyTurning.value = false
    oldStoryNode.value = null
  } catch (error) {
    storyTurning.value = false
    oldStoryNode.value = null
    ElMessage.error(error?.message || '跳转失败')
  } finally {
    loadingStory.value = false
  }
}

// ========== 全局点击 ==========
function onGlobalClick() {
  if (introPhase.value === 'zoom') {
    introPhase.value = 'glow'
    setTimeout(() => { introPhase.value = 'glow-done' }, 2500)
    setTimeout(() => { introPhase.value = 'done' }, 3500)
  }
}

// ========== 生命周期 ==========
onMounted(() => {
  // 如果已登录，跳过封面直接进主页
  if (userStore.isLoggedIn) {
    introPhase.value = 'zoom'
    setTimeout(() => introPhase.value = 'glow', 200)
    setTimeout(() => introPhase.value = 'glow-done', 600)
    setTimeout(async () => {
      introPhase.value = 'done'
      currentSpread.value = 2
      await loadStories()
    }, 1000)
  }
  window.addEventListener('auth-expired', onAuthExpired)
})

onUnmounted(() => {
  if (checkInterval) clearInterval(checkInterval)
  window.removeEventListener('auth-expired', onAuthExpired)
})
</script>

<style scoped>
/* ============================================= */
/* 开场动画 */
/* ============================================= */
.intro-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: radial-gradient(ellipse at center, #1a0a00 0%, #0d0500 40%, #000 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.camera-zoom {
  transform: scale(0.3);
  opacity: 0;
  transition: transform 2.5s cubic-bezier(0.16, 1, 0.3, 1), opacity 1.5s ease;
}

.camera-zoom.intro-zoom {
  transform: scale(1);
  opacity: 1;
}

.camera-zoom.intro-glow,
.camera-zoom.intro-glow-done {
  transform: scale(1);
  opacity: 1;
}

.book-cover-3d {
  width: 320px;
  height: 440px;
  perspective: 800px;
  position: relative;
}

.cover-front {
  width: 100%;
  height: 100%;
  background: linear-gradient(145deg, #3d1c02 0%, #5a2d0c 30%, #3d1c02 70%, #2a1000 100%);
  border-radius: 8px 24px 24px 8px;
  box-shadow:
    0 20px 60px rgba(0,0,0,0.7),
    0 0 0 3px #2a1000,
    0 0 0 6px #5a2d0c,
    inset 0 0 80px rgba(0,0,0,0.4);
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.cover-front::before {
  content: '';
  position: absolute;
  inset: 12px;
  border: 2px solid rgba(180, 140, 80, 0.3);
  border-radius: 4px 18px 18px 4px;
}

.book-spine-line {
  position: absolute;
  left: 12px;
  top: 10%;
  bottom: 10%;
  width: 3px;
  background: linear-gradient(to bottom, transparent, rgba(180,140,80,0.5), transparent);
}

.book-corner {
  position: absolute;
  width: 24px;
  height: 24px;
  border-color: rgba(180, 140, 80, 0.5);
  border-style: solid;
}
.book-corner.tl { top: 14px; left: 18px; border-width: 2px 0 0 2px; border-radius: 2px 0 0 0; }
.book-corner.tr { top: 14px; right: 18px; border-width: 2px 2px 0 0; border-radius: 0 2px 0 0; }
.book-corner.bl { bottom: 14px; left: 18px; border-width: 0 0 2px 2px; border-radius: 0 0 0 2px; }
.book-corner.br { bottom: 14px; right: 18px; border-width: 0 2px 2px 0; border-radius: 0 0 2px 0; }

.seal-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  position: relative;
  z-index: 2;
}

.seal-char {
  font-family: 'STKaiti', 'KaiTi', '楷体', 'Noto Serif SC', 'SimSun', '宋体', serif;
  font-size: 72px;
  color: #c41e1e;
  text-shadow: 0 0 0 #8b0000, 0 0 10px rgba(180, 20, 20, 0.4);
  position: relative;
  font-weight: bold;
  letter-spacing: 2px;
  user-select: none;
}

.seal-char.qi.glow-up { animation: glowUp 2s ease-out forwards; }
.seal-char.lu.glow-down { animation: glowDown 2s ease-out forwards; }

@keyframes glowUp {
  0% { text-shadow: 0 0 0 #8b0000; color: #8b0000; }
  20% { text-shadow: 0 0 8px #ff6b35, 0 0 20px #c41e1e, 0 -10px 15px rgba(255,100,30,0.3); color: #b71c1c; }
  60% { text-shadow: 0 0 16px #ff8c42, 0 0 40px #e53935, 0 -30px 30px rgba(255,140,40,0.5), 0 -60px 20px rgba(255,100,30,0.15); color: #d32f2f; }
  100% { text-shadow: 0 0 8px #ffab76, 0 0 20px #e53935, 0 -40px 40px rgba(255,150,50,0.6), 0 -80px 30px rgba(255,120,40,0.25), 0 -120px 10px rgba(255,100,30,0.05); color: #f44336; }
}

@keyframes glowDown {
  0% { text-shadow: 0 0 0 #8b0000; color: #8b0000; }
  20% { text-shadow: 0 0 8px #ff6b35, 0 0 20px #c41e1e, 0 10px 15px rgba(255,100,30,0.3); color: #b71c1c; }
  60% { text-shadow: 0 0 16px #ff8c42, 0 0 40px #e53935, 0 30px 30px rgba(255,140,40,0.5), 0 60px 20px rgba(255,100,30,0.15); color: #d32f2f; }
  100% { text-shadow: 0 0 8px #ffab76, 0 0 20px #e53935, 0 40px 40px rgba(255,150,50,0.6), 0 80px 30px rgba(255,120,40,0.25), 0 120px 10px rgba(255,100,30,0.05); color: #f44336; }
}

.camera-zoom.intro-glow-done .seal-char.qi {
  color: #f44336;
  text-shadow: 0 0 8px #ffab76, 0 0 20px #e53935, 0 -40px 40px rgba(255,150,50,0.5), 0 -80px 30px rgba(255,120,40,0.2);
  animation: glowPulse 3s ease-in-out infinite;
}

.camera-zoom.intro-glow-done .seal-char.lu {
  color: #f44336;
  text-shadow: 0 0 8px #ffab76, 0 0 20px #e53935, 0 40px 40px rgba(255,150,50,0.5), 0 80px 30px rgba(255,120,40,0.2);
  animation: glowPulse 3s ease-in-out infinite;
}

@keyframes glowPulse {
  0%, 100% { opacity: 0.8; }
  50% { opacity: 1; }
}

/* ============================================= */
/* 书本场景 */
/* ============================================= */
.book-scene {
  position: fixed;
  inset: 0;
  background: radial-gradient(ellipse at center, #2c1810 0%, #1a0a00 50%, #0a0400 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
}

.book-container {
  perspective: 1800px;
  position: relative;
}

.book-shadow {
  position: absolute;
  bottom: -20px;
  left: 5%;
  width: 90%;
  height: 30px;
  background: radial-gradient(ellipse, rgba(0,0,0,0.6) 0%, transparent 70%);
}

.book {
  width: 760px;
  height: 520px;
  position: relative;
  transform-style: preserve-3d;
  transform: rotateY(-5deg) rotateX(3deg);
}

.book-edge {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 760px;
  height: 12px;
  background: linear-gradient(to bottom, #3d1c02, #2a1000);
  transform: translateY(12px) rotateX(-20deg);
  transform-origin: top;
  border-radius: 0 0 4px 4px;
}

.page {
  position: absolute;
  top: 0;
  width: 380px;
  height: 520px;
  background: linear-gradient(to right, #faf3e8 0%, #f5ecd7 50%, #efe3cc 100%);
  box-shadow: inset 0 0 40px rgba(0,0,0,0.06);
}

.page-left {
  left: 0;
  border-radius: 16px 0 0 16px;
  transform-origin: right center;
  z-index: 1;
}

.page-right {
  right: 0;
  border-radius: 0 16px 16px 0;
  transform-origin: left center;
  z-index: 1;
}

.page-content {
  width: 100%;
  height: 100%;
  padding: 36px 32px;
  overflow-y: auto;
}

.page-content-left { padding-right: 20px; }
.page-content-right { padding-left: 20px; }

.page-left::after {
  content: '';
  position: absolute;
  right: 0; top: 0; bottom: 0;
  width: 12px;
  background: linear-gradient(to right, rgba(0,0,0,0.06), transparent);
}

.page-right::before {
  content: '';
  position: absolute;
  left: 0; top: 0; bottom: 0;
  width: 12px;
  background: linear-gradient(to left, rgba(0,0,0,0.06), transparent);
}

.page-decorative {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.decorative-content { text-align: center; }

.deco-seal {
  font-family: 'STKaiti', 'KaiTi', '楷体', 'Noto Serif SC', serif;
  font-size: 48px;
  color: #c41e1e;
  opacity: 0.6;
}

.deco-line {
  width: 60px;
  height: 2px;
  background: linear-gradient(to right, transparent, rgba(196,30,30,0.4), transparent);
  margin: 12px auto;
}

.deco-text {
  font-size: 14px;
  color: #8a6c56;
  letter-spacing: 4px;
}

/* ============================================= */
/* 翻页动画 */
/* ============================================= */
.page-flip { z-index: 10; transform-style: preserve-3d; }

.page-flip-forward {
  right: 0;
  border-radius: 0 16px 16px 0;
  transform-origin: left center;
  transform: rotateY(0deg);
}

.page-flip-forward.animate-flip {
  animation: flipForward 0.8s ease-in-out forwards;
}

.page-flip-backward {
  left: 0;
  border-radius: 16px 0 0 16px;
  transform-origin: right center;
  transform: rotateY(0deg);
}

.page-flip-backward.animate-flip-back {
  animation: flipBackward 0.8s ease-in-out forwards;
}

.page-back {
  position: absolute;
  inset: 0;
  background: linear-gradient(to left, #e8dcc8, #ddd2ba);
  border-radius: inherit;
  backface-visibility: hidden;
}

.page-flip-forward .page-content,
.page-flip-backward .page-content {
  backface-visibility: hidden;
}

@keyframes flipForward {
  0% { transform: rotateY(0deg); box-shadow: -6px 0 20px rgba(0,0,0,0.15); }
  100% { transform: rotateY(-170deg); box-shadow: -12px 0 30px rgba(0,0,0,0.35); }
}

@keyframes flipBackward {
  0% { transform: rotateY(0deg); box-shadow: 6px 0 20px rgba(0,0,0,0.15); }
  100% { transform: rotateY(170deg); box-shadow: 12px 0 30px rgba(0,0,0,0.35); }
}

.page-left.is-turning,
.page-right.is-turning {
  pointer-events: none;
}

/* ============================================= */
/* 页面内部样式 */
/* ============================================= */
.book-page-content {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.page-title {
  font-family: 'STKaiti', 'KaiTi', '楷体', 'Noto Serif SC', serif;
  font-size: 22px;
  color: #3e2f22;
  margin: 0 0 4px;
  letter-spacing: 0.05em;
}

.page-subtitle {
  font-size: 12px;
  color: #8a6c56;
  margin: 0 0 18px;
  line-height: 1.5;
}

.page-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.field-label {
  font-size: 12px;
  color: #6f513f;
  font-weight: 500;
}

.field-input,
.field-textarea {
  padding: 8px 12px;
  border: 1px solid #d4c4b0;
  border-radius: 6px;
  font-size: 13px;
  color: #3e2f22;
  background: rgba(255,255,255,0.6);
  outline: none;
  transition: border-color 0.2s;
  font-family: inherit;
}

.field-input:focus,
.field-textarea:focus {
  border-color: #b56f42;
  box-shadow: 0 0 0 2px rgba(181,111,66,0.15);
}

.field-textarea {
  resize: vertical;
  min-height: 80px;
}

.page-btn {
  padding: 8px 20px;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}

.page-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.page-btn-primary {
  background: linear-gradient(135deg, #c41e1e 0%, #9b1b1b 100%);
  color: #fff;
  font-weight: 500;
}

.page-btn-primary:hover:not(:disabled) {
  background: linear-gradient(135deg, #d32f2f 0%, #b71c1c 100%);
  box-shadow: 0 2px 12px rgba(196,30,30,0.3);
}

.page-btn-outline {
  background: transparent;
  color: #8a6c56;
  border: 1px solid #d4c4b0;
}

.page-btn-outline:hover {
  background: rgba(0,0,0,0.04);
  border-color: #b56f42;
}

.page-btn-sm {
  padding: 4px 12px;
  font-size: 11px;
  background: rgba(196,30,30,0.08);
  color: #c41e1e;
  border: 1px solid rgba(196,30,30,0.2);
  border-radius: 4px;
}

.page-btn-sm:hover {
  background: rgba(196,30,30,0.15);
}

.page-btn-choice {
  padding: 10px 16px;
  background: linear-gradient(135deg, #faf3e8 0%, #f0e4d0 100%);
  border: 1px solid #d4c4b0;
  border-radius: 6px;
  color: #4f3827;
  font-size: 13px;
  text-align: left;
  line-height: 1.5;
}

.page-btn-choice:hover {
  background: linear-gradient(135deg, #f0e4d0 0%, #e8d8b8 100%);
  border-color: #b56f42;
}

.page-link-row {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: center;
  margin-top: 8px;
}

.page-link-text {
  font-size: 12px;
  color: #8a6c56;
}

.page-link {
  font-size: 12px;
  color: #c41e1e;
  background: none;
  border: none;
  cursor: pointer;
  font-family: inherit;
  text-decoration: underline;
  text-underline-offset: 2px;
}

.page-link:hover { color: #e53935; }

.page-story-list {
  margin-top: 16px;
  border-top: 1px solid rgba(0,0,0,0.06);
  padding-top: 12px;
  flex: 1;
  overflow-y: auto;
}

.story-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid rgba(0,0,0,0.04);
}

.story-item-title {
  font-size: 13px;
  color: #3e2f22;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.story-item-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.page-loading-text,
.page-empty-text {
  font-size: 12px;
  color: #b5a08c;
  text-align: center;
  padding: 16px 0;
}

.story-text {
  font-size: 14px;
  line-height: 2;
  color: #3e2f22;
  white-space: pre-wrap;
  flex: 1;
  overflow-y: auto;
}
/* 故事翻页动画 */
.story-flipping {
  position: relative;
  overflow: hidden;
}

.story-flipping .story-text {
  position: absolute;
  inset: 0;
  overflow-y: auto;
}

.story-text-out {
  animation: storyFlipOut 0.75s ease-in-out forwards;
  z-index: 1;
}

.story-text-in {
  animation: storyFlipIn 0.75s ease-in-out forwards;
  z-index: 0;
}

@keyframes storyFlipOut {
  0% { transform: translateX(0); opacity: 1; }
  100% { transform: translateX(-40px); opacity: 0; }
}

@keyframes storyFlipIn {
  0% { transform: translateX(40px); opacity: 0; }
  100% { transform: translateX(0); opacity: 1; }
}


.story-choices {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid rgba(0,0,0,0.06);
}

.story-ending-section {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid rgba(0,0,0,0.06);
  text-align: center;
}

.story-ending-tag {
  font-family: 'STKaiti', 'KaiTi', '楷体', serif;
  font-size: 16px;
  color: #c41e1e;
  margin: 0;
}

.story-generating,
.story-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
}

.generating-icon {
  font-size: 48px;
  margin-bottom: 16px;
  animation: float 2s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}

/* 页码指示器 */
.page-indicator {
  position: fixed;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 10px;
  z-index: 20;
}

.page-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(255,255,255,0.2);
  transition: all 0.3s;
}

.page-dot.active {
  background: rgba(196,30,30,0.8);
  box-shadow: 0 0 8px rgba(196,30,30,0.4);
  transform: scale(1.3);
}
</style>
