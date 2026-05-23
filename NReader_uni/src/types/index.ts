export interface Book {
  id: string
  title: string
  author: string
  cover: string
  filePath: string
  format: 'txt' | 'epub'
  progress: number
  lastReadTime: string
}

export interface ReadingSettings {
  fontSize: number
  lineSpacing: 'compact' | 'normal' | 'relaxed'
  pageMode: 'click' | 'slide' | 'scroll'
  backgroundColor: 'white' | 'cream' | 'dark'
  pageEffect: 'none' | 'flip' | 'slideUpDown'
}

export interface LibraryBook {
  id: string
  title: string
  author: string
  cover: string
  isInShelf: boolean
  filePath?: string
}
