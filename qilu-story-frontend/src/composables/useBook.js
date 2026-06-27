import { ref, computed, reactive, provide, inject, markRaw } from 'vue';
import LoginView from '../views/LoginView.vue';
import RegisterView from '../views/RegisterView.vue';
import HomeView from '../views/HomeView.vue';
import StoryView from '../views/StoryView.vue';

const BOOK_KEY = Symbol('book');

const PAGE_MAP = {
  cover: null,         // book cover → component is BookCover
  login: markRaw(LoginView),
  register: markRaw(RegisterView),
  home: markRaw(HomeView),
  reading: markRaw(StoryView)
};

/*
  Page stack semantics (matches user requirements precisely):
    cover → login   : opening animation (push forward)
    login → register: flip forward (+1 page)
    register → login: flip backward (-1 page)
    login → home    : flip forward (+1 page)
    home → reading  : flip forward (+1 page)
    reading choices : internal page-flip within StoryView (no stack change)
    ending → home   : flip backward (-1 page)
    home → login    : flip backward (-1 page, logout)
*/

export function createBookState() {
  const pageStack = ref(['cover']);
  const isFlipping = ref(false);
  const flipDirection = ref('forward');
  const showCoverAnimation = ref(true);
  const coverGlowDone = ref(false);
  const bookOpened = ref(false);

  const currentPage = computed(() => pageStack.value[pageStack.value.length - 1]);

  const currentComponent = computed(() => {
    if (currentPage.value === 'cover') return null;
    return PAGE_MAP[currentPage.value] || null;
  });

  const previousPage = computed(() => {
    if (pageStack.value.length < 2) return null;
    return pageStack.value[pageStack.value.length - 2];
  });

  function openBook() {
    if (bookOpened.value) return;
    coverGlowDone.value = true;
    // Wait for glow to finish, then open
    return new Promise((resolve) => {
      setTimeout(() => {
        bookOpened.value = true;
        pageStack.value = ['login'];
        showCoverAnimation.value = false;
        resolve();
      }, 1200);
    });
  }

  async function flipForward(pageId) {
    if (isFlipping.value) return;
    if (!PAGE_MAP[pageId] && pageId !== 'cover') {
      console.warn(`Unknown page: ${pageId}`);
      return;
    }
    isFlipping.value = true;
    flipDirection.value = 'forward';
    await new Promise((r) => setTimeout(r, 30));
    pageStack.value.push(pageId);
    await new Promise((r) => setTimeout(r, 800));
    isFlipping.value = false;
  }

  async function flipBackward() {
    if (isFlipping.value || pageStack.value.length <= 1) return;
    isFlipping.value = true;
    flipDirection.value = 'backward';
    await new Promise((r) => setTimeout(r, 30));
    pageStack.value.pop();
    await new Promise((r) => setTimeout(r, 800));
    isFlipping.value = false;
  }

  return {
    pageStack,
    currentPage,
    currentComponent,
    previousPage,
    isFlipping,
    flipDirection,
    showCoverAnimation,
    coverGlowDone,
    bookOpened,
    PAGE_MAP,
    openBook,
    flipForward,
    flipBackward
  };
}

export function provideBook(book) {
  provide(BOOK_KEY, book);
  return book;
}

export function useBook() {
  const book = inject(BOOK_KEY);
  if (!book) throw new Error('useBook() must be used within a BookLayout provider');
  return book;
}
