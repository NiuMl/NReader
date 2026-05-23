import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Book, ReadingSettings } from '@/types'
import {
  getBookshelf,
  saveBookshelf,
  addBookToShelf,
  removeBookFromShelf,
  updateBookProgress,
  getBookProgress,
  getBookCurrentPage,
  saveBookCurrentPage,
  getSettings,
  saveSettings,
  defaultSettings
} from '@/utils/storage'

export const useBookStore = defineStore('books', () => {
  const bookshelf = ref<Book[]>(getBookshelf())
  const currentBook = ref<Book | null>(null)
  const readingSettings = ref<ReadingSettings>(getSettings())

  const sortedBookshelf = computed(() => {
    return [...bookshelf.value].sort((a, b) => {
      return new Date(b.lastReadTime).getTime() - new Date(a.lastReadTime).getTime()
    })
  })

  function addBook(book: Book) {
    addBookToShelf(book)
    bookshelf.value = getBookshelf()
  }

  function removeBook(bookId: string) {
    removeBookFromShelf(bookId)
    bookshelf.value = getBookshelf()
  }

  function setCurrentBook(book: Book | null) {
    currentBook.value = book
  }

  function updateProgress(bookId: string, progress: number) {
    updateBookProgress(bookId, progress)
    const book = bookshelf.value.find(b => b.id === bookId)
    if (book) {
      book.progress = progress
      book.lastReadTime = new Date().toISOString()
    }
  }

  function getProgress(bookId: string): number {
    return getBookProgress(bookId)
  }

  function getCurrentPage(bookId: string): number {
    return getBookCurrentPage(bookId)
  }

  function updateCurrentPage(bookId: string, page: number): void {
    saveBookCurrentPage(bookId, page)
  }

  function updateSettings(settings: Partial<ReadingSettings>) {
    readingSettings.value = { ...readingSettings.value, ...settings }
    saveSettings(readingSettings.value)
  }

  function resetSettings() {
    readingSettings.value = { ...defaultSettings }
    saveSettings(readingSettings.value)
  }

  function checkBookInShelf(bookId: string): boolean {
    return bookshelf.value.some(b => b.id === bookId)
  }

  return {
    bookshelf,
    sortedBookshelf,
    currentBook,
    readingSettings,
    addBook,
    removeBook,
    setCurrentBook,
    updateProgress,
    getProgress,
    getCurrentPage,
    updateCurrentPage,
    updateSettings,
    resetSettings,
    checkBookInShelf
  }
})
