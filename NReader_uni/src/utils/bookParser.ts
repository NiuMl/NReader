export function parseTxtContent(content: string): string[] {
  const lines = content.split(/\r\n|\n|\r/)
  return lines.filter(line => line.trim())
}

export function generateBookId(): string {
  return Date.now().toString(36) + Math.random().toString(36).substr(2)
}

export function getFileExtension(filename: string): string {
  return filename.split('.').pop()?.toLowerCase() || ''
}

export function formatFileName(filename: string): string {
  const ext = getFileExtension(filename)
  return filename.replace(new RegExp(`\\.${ext}$`, 'i'), '')
}

export function parseBookTitle(content: string): string {
  const lines = content.split(/\r\n|\n|\r/).filter(line => line.trim())
  if (lines.length === 0) return '未知书名'
  return lines[0].trim().substring(0, 50)
}

export function estimateReadingProgress(currentIndex: number, totalLines: number): number {
  if (totalLines === 0) return 0
  return Math.round((currentIndex / totalLines) * 100)
}
