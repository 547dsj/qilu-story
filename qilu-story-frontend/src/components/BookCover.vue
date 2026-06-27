<template>
  <div class="book-cover-page">
    <!-- Decorative border pattern -->
    <div class="cover-border">
      <div class="cover-border-inner">
        <!-- Seal stamp container -->
        <div class="seal-stamp" :class="{ 'glow-done': glowDone }">
          <div class="seal-border">
            <span class="seal-char seal-qi">歧</span>
            <span class="seal-char seal-lu">路</span>
          </div>
        </div>
        <!-- Subtitle -->
        <p class="cover-subtitle">互动小说</p>
      </div>
    </div>
    <!-- Click hint -->
    <div class="cover-hint" v-if="glowDone">
      <span>点击翻开</span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';

const emit = defineEmits(['open']);
const glowDone = ref(false);

onMounted(() => {
  // Glow animation completes after ~2.5s
  setTimeout(() => {
    glowDone.value = true;
  }, 2500);
});
</script>

<style scoped>
.book-cover-page {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(160deg, #3a1010 0%, #5a1a1a 30%, #4a1515 60%, #2d0a0a 100%);
  border-radius: 2px 8px 8px 2px;
  position: relative;
  overflow: hidden;
  cursor: pointer;
  user-select: none;
}

/* Subtle leather texture */
.book-cover-page::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    repeating-linear-gradient(
      0deg,
      transparent,
      transparent 2px,
      rgba(255, 255, 255, 0.008) 2px,
      rgba(255, 255, 255, 0.008) 4px
    ),
    repeating-linear-gradient(
      90deg,
      transparent,
      transparent 2px,
      rgba(255, 255, 255, 0.005) 2px,
      rgba(255, 255, 255, 0.005) 4px
    );
  pointer-events: none;
}

.cover-border {
  border: 2px solid rgba(201, 169, 110, 0.4);
  padding: 24px;
  border-radius: 4px;
  position: relative;
}

.cover-border::before {
  content: '';
  position: absolute;
  inset: 4px;
  border: 1px solid rgba(201, 169, 110, 0.25);
  border-radius: 2px;
  pointer-events: none;
}

.cover-border-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

/* Seal Stamp */
.seal-stamp {
  position: relative;
}

.seal-border {
  position: relative;
  display: flex;
  gap: 8px;
  padding: 14px 18px;
  border: 4px solid #c41e3a;
  border-radius: 3px;
  background: transparent;
}

.seal-border::before {
  content: '';
  position: absolute;
  inset: -10px;
  border: 1px solid rgba(196, 30, 58, 0.3);
  pointer-events: none;
}

.seal-char {
  font-family: 'KaiTi', 'STKaiti', 'SimSun', 'FangSong', serif;
  font-size: clamp(52px, 8vw, 80px);
  color: #c41e3a;
  position: relative;
  line-height: 1;
  font-weight: bold;
  text-shadow: 1px 1px 0 rgba(0, 0, 0, 0.2);
}

/* Glow effect - character ::after pseudo elements */
.seal-char::after {
  content: attr(data-char);
  position: absolute;
  top: 0;
  left: 0;
  color: #ffd700;
  font-size: inherit;
  font-family: inherit;
  font-weight: inherit;
  line-height: inherit;
  text-shadow:
    0 0 8px #ffd700,
    0 0 18px #ffa500,
    0 0 30px #ff8c00,
    0 0 45px #ff6600;
  opacity: 0;
  pointer-events: none;
}

/* 歧: glow spreads from center upward */
.seal-qi {
  --char: '歧';
}

.seal-qi::after {
  clip-path: inset(50% 0 50% 0);
}

.glow-done .seal-qi::after,
.seal-stamp:not(.glow-done) .seal-qi::after {
  animation: glow-up 1.6s 0.4s ease-out forwards;
}

/* 路: glow spreads from center downward */
.seal-lu {
  --char: '路';
}

.seal-lu::after {
  clip-path: inset(50% 0 50% 0);
}

.glow-done .seal-lu::after,
.seal-stamp:not(.glow-done) .seal-lu::after {
  animation: glow-down 1.6s 0.4s ease-out forwards;
}

@keyframes glow-up {
  0% {
    clip-path: inset(55% 0 45% 0);
    opacity: 0;
  }
  20% {
    opacity: 1;
  }
  100% {
    clip-path: inset(0 0 0 0);
    opacity: 1;
  }
}

@keyframes glow-down {
  0% {
    clip-path: inset(45% 0 55% 0);
    opacity: 0;
  }
  20% {
    opacity: 1;
  }
  100% {
    clip-path: inset(0 0 -5% 0);
    opacity: 1;
  }
}

/* After glow completes, keep a subtle residual glow */
.glow-done .seal-qi::after {
  clip-path: inset(0 0 0 0);
  opacity: 0.6;
  animation: glow-residual-up 0.6s ease-out forwards;
}

.glow-done .seal-lu::after {
  clip-path: inset(0 0 0 0);
  opacity: 0.6;
  animation: glow-residual-down 0.6s ease-out forwards;
}

@keyframes glow-residual-up {
  to {
    text-shadow:
      0 0 3px #ffd700,
      0 0 8px #ffa500;
    opacity: 0.5;
  }
}

@keyframes glow-residual-down {
  to {
    text-shadow:
      0 0 3px #ffd700,
      0 0 8px #ffa500;
    opacity: 0.5;
  }
}

.cover-subtitle {
  font-family: 'KaiTi', 'STKaiti', serif;
  font-size: 16px;
  color: rgba(201, 169, 110, 0.8);
  letter-spacing: 8px;
  margin: 0;
  padding-right: -8px;
}

.cover-hint {
  position: absolute;
  bottom: 40px;
  font-family: 'KaiTi', 'STKaiti', serif;
  font-size: 14px;
  color: rgba(201, 169, 110, 0.6);
  letter-spacing: 4px;
  animation: hint-pulse 2s ease-in-out infinite;
}

@keyframes hint-pulse {
  0%, 100% { opacity: 0.4; }
  50% { opacity: 0.8; }
}
</style>
