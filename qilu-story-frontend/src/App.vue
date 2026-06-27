<template>
  <BookView />
</template>

<script setup>
import BookView from './views/BookView.vue'
</script>

<style>
/* 全局重置 */
html, body, #app {
  margin: 0;
  padding: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  font-family: 'Noto Serif SC', 'STKaiti', 'KaiTi', '楷体', 'Microsoft YaHei', '微软雅黑', sans-serif;
  -webkit-font-smoothing: antialiased;
}

*, *::before, *::after {
  box-sizing: border-box;
}
</style>
