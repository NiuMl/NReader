import type { Book, ReadingSettings } from '@/types'

const BOOKSHELF_KEY = 'simple_reader_bookshelf'
const SETTINGS_KEY = 'simple_reader_settings'
const PROGRESS_KEY = 'simple_reader_progress_'

export const defaultSettings: ReadingSettings = {
  fontSize: 19,
  lineSpacing: 'normal',
  pageMode: 'click',
  backgroundColor: 'white',
  pageEffect: 'none'
}

export function getBookshelf(): Book[] {
  try {
    const data = uni.getStorageSync(BOOKSHELF_KEY)
    return data ? JSON.parse(data) : []
  } catch {
    return []
  }
}

export function saveBookshelf(books: Book[]): void {
  uni.setStorageSync(BOOKSHELF_KEY, JSON.stringify(books))
}

export function addBookToShelf(book: Book): void {
  const books = getBookshelf()
  const exists = books.find(b => b.id === book.id || b.filePath === book.filePath)
  if (!exists) {
    books.push(book)
    saveBookshelf(books)
  }
}

export function removeBookFromShelf(bookId: string): void {
  const books = getBookshelf()
  const filtered = books.filter(b => b.id !== bookId)
  saveBookshelf(filtered)
}

export function clearBookshelf(): void {
  uni.setStorageSync(BOOKSHELF_KEY, '[]')
}

export function updateBookProgress(bookId: string, progress: number): void {
  const books = getBookshelf()
  const book = books.find(b => b.id === bookId)
  if (book) {
    book.progress = progress
    book.lastReadTime = new Date().toISOString()
    saveBookshelf(books)
  }
  uni.setStorageSync(`${PROGRESS_KEY}${bookId}`, Math.round(progress).toString())
}

export function getBookProgress(bookId: string): number {
  const progress = uni.getStorageSync(`${PROGRESS_KEY}${bookId}`)
  if (!progress) return 0
  const parsed = parseFloat(progress)
  return isNaN(parsed) ? 0 : parsed
}

export function getBookCurrentPage(bookId: string): number {
  const page = uni.getStorageSync(`${PROGRESS_KEY}${bookId}_page`)
  if (!page) return 1
  const parsed = parseInt(page, 10)
  return isNaN(parsed) ? 1 : parsed
}

export function saveBookCurrentPage(bookId: string, page: number): void {
  uni.setStorageSync(`${PROGRESS_KEY}${bookId}_page`, Math.max(1, page).toString())
}

export function getSettings(): ReadingSettings {
  try {
    const data = uni.getStorageSync(SETTINGS_KEY)
    return data ? { ...defaultSettings, ...JSON.parse(data) } : defaultSettings
  } catch {
    return defaultSettings
  }
}

export function saveSettings(settings: ReadingSettings): void {
  uni.setStorageSync(SETTINGS_KEY, JSON.stringify(settings))
}

export function getNickname(): string {
  try {
    return uni.getStorageSync('simple_reader_nickname') || '阅读者'
  } catch {
    return '阅读者'
  }
}

export function saveNickname(nickname: string): void {
  uni.setStorageSync('simple_reader_nickname', nickname)
}

export function clearCache(): void {
  uni.removeStorageSync(BOOKSHELF_KEY)
  uni.removeStorageSync(SETTINGS_KEY)
}
