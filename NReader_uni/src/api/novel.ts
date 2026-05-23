import type { LibraryBook } from '@/types'

const API_BASE_URL = 'http://localhost:5000'

export interface FetchNovelsResponse {
  novels: LibraryBook[]
  total: number
  page: number
  page_size: number
}

export async function fetchNovels(page: number = 1, pageSize: number = 10, search: string = ''): Promise<FetchNovelsResponse> {
  try {
    const response = await uni.request({
      url: `${API_BASE_URL}/api/novels`,
      method: 'GET',
      timeout: 10000,
      data: {
        page,
        page_size: pageSize,
        search
      }
    })

    if (response.statusCode === 200 && response.data) {
      return response.data as FetchNovelsResponse
    }
    throw new Error(`Request failed with status ${response.statusCode}`)
  } catch (error) {
    console.error('Failed to fetch novels:', error)
    throw error
  }
}

export async function fetchNovelContent(novelId: string): Promise<{ id: string; title: string; content: string }> {
  try {
    const response = await uni.request({
      url: `${API_BASE_URL}/api/novel/${novelId}`,
      method: 'GET',
      timeout: 10000
    })

    if (response.statusCode === 200 && response.data) {
      return response.data as { id: string; title: string; content: string }
    }
    throw new Error(`Request failed with status ${response.statusCode}`)
  } catch (error) {
    console.error('Failed to fetch novel content:', error)
    throw error
  }
}