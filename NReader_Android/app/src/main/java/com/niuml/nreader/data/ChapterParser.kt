package com.niuml.nreader.data

import android.util.Log
import java.util.regex.Pattern

class ChapterParser(private val content: String, private val language: String = "zh") {

    data class Chapter(
        val title: String,
        val startPos: Int,
        val endPos: Int,
        val isVolume: Boolean = false,
        val detected: Boolean = false
    )

    private val LINES_BETWEEN_SEGMENTS = 8
    private val FALLBACK_PARAGRAPHS_PER_CHAPTER = 100

    fun parse(): List<Chapter> {
        val sanitizedContent = sanitizeContent(content)
        
        if (language == "zh") {
            return parseChineseChapters(sanitizedContent)
        } else {
            return parseEnglishChapters(sanitizedContent)
        }
    }

    private fun sanitizeContent(content: String): String {
        var result = content
        do {
            val previous = result
            result = result.replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")
        } while (result != previous)
        return result.trim()
    }

    private fun parseChineseChapters(content: String): List<Chapter> {
        val chapters = mutableListOf<Chapter>()
        val segmentRegex = createSegmentRegex(LINES_BETWEEN_SEGMENTS)
        val segments = content.split(segmentRegex)

        var globalOffset = 0
        for (segment in segments) {
            if (segment.isEmpty()) {
                globalOffset += segment.length
                continue
            }

            val segmentChapters = extractChaptersFromSegment(segment, globalOffset)
            appendSegmentChapters(chapters, segmentChapters)
            globalOffset += segment.length
        }

        return mergeShortChapters(chapters)
    }

    private fun parseEnglishChapters(content: String): List<Chapter> {
        val chapters = mutableListOf<Chapter>()
        val chapterKeywords = listOf("Chapter", "Part", "Section", "Book", "Volume", "Act")
        val prefaceKeywords = listOf("Prologue", "Epilogue", "Introduction", "Foreword", "Preface", "Afterword")
        
        val numberPattern = "(?:\\d+|(?:[IVXLCDM]{2,}|V|X|L|C|D|M)\\b)"
        val dotNumberPattern = "\\.\\d{1,4}"
        val titlePattern = "[^\\n]{0,50}"
        
        val normalChapterPattern = chapterKeywords.joinToString("|") {
            "$it\\s*(?:$numberPattern|$dotNumberPattern)(?:[:.\\-–—]?\\s*$titlePattern)?"
        }
        
        val prefacePattern = prefaceKeywords.joinToString("|") {
            "$it(?:[:.\\-–—]?\\s*$titlePattern)?"
        }
        
        val combinedPattern = "(?:^|\\n)($normalChapterPattern|$prefacePattern)(?=\\s|$)"
        val chapterRegex = Pattern.compile(combinedPattern, Pattern.CASE_INSENSITIVE)

        val matcher = chapterRegex.matcher(content)
        var lastEnd = 0
        var chapterIndex = 0

        while (matcher.find()) {
            val title = matcher.group(1)?.trim() ?: continue
            val start = matcher.start()
            
            if (start > lastEnd) {
                val contentBeforeChapter = content.substring(lastEnd, start).trim()
                if (contentBeforeChapter.isNotEmpty()) {
                    chapters.add(Chapter(
                        title = "Prologue ${chapterIndex + 1}",
                        startPos = lastEnd,
                        endPos = start,
                        isVolume = false,
                        detected = false
                    ))
                    chapterIndex++
                }
            }

            val isVolume = title.contains(Regex("\\b(Volume|Book)\\b", RegexOption.IGNORE_CASE))
            chapters.add(Chapter(
                title = title,
                startPos = start,
                endPos = matcher.end(),
                isVolume = isVolume,
                detected = true
            ))
            chapterIndex++
            lastEnd = matcher.end()
        }

        if (lastEnd < content.length) {
            val remainingContent = content.substring(lastEnd).trim()
            if (remainingContent.isNotEmpty()) {
                chapters.add(Chapter(
                    title = "Afterword",
                    startPos = lastEnd,
                    endPos = content.length,
                    isVolume = false,
                    detected = false
                ))
            }
        }

        return chapters
    }

    private fun extractChaptersFromSegment(segment: String, offset: Int): List<Chapter> {
        val chapters = mutableListOf<Chapter>()
        val chapterRegexps = createChineseChapterRegexps()
        
        var bestMatches: List<String>? = null
        for (chapterRegex in chapterRegexps) {
            val matches = segment.split(chapterRegex)
            if (isGoodMatches(matches)) {
                bestMatches = matches
                break
            }
        }

        if (bestMatches != null && bestMatches.isNotEmpty()) {
            val joinedMatches = joinAroundUndefined(bestMatches)
            
            for (j in 1 until joinedMatches.size step 2) {
                val title = joinedMatches[j]?.trim() ?: ""
                val content = joinedMatches.getOrNull(j + 1)?.trim() ?: ""
                
                val titleStart = segment.indexOf(title, 0)
                val contentStart = if (titleStart >= 0) titleStart + title.length else 0
                val contentEnd = contentStart + content.length
                
                val isVolume = Regex("第[零〇一二三四五六七八九十百千万0-9]+(卷|本|册|部)").matches(title)
                
                if (title.isNotEmpty()) {
                    chapters.add(Chapter(
                        title = title,
                        startPos = offset + titleStart,
                        endPos = offset + contentEnd,
                        isVolume = isVolume,
                        detected = true
                    ))
                }
            }

            if (joinedMatches.isNotEmpty() && joinedMatches[0]?.trim()?.isNotEmpty() == true) {
                val initialContent = joinedMatches[0]!!.trim()
                val firstLine = initialContent.split("\n")[0].trim()
                val segmentTitle = if (firstLine.length > 16) {
                    initialContent.split(Regex("[\\n\\s\\p{P}]"))[0].trim()
                } else {
                    firstLine
                }
                
                chapters.add(0, Chapter(
                    title = segmentTitle.take(16),
                    startPos = offset,
                    endPos = offset + initialContent.length,
                    isVolume = false,
                    detected = false
                ))
            }
        } else {
            val paragraphs = segment.split(Regex("\\n+"))
            for (i in paragraphs.indices step FALLBACK_PARAGRAPHS_PER_CHAPTER) {
                val chunk = paragraphs.subList(i, minOf(i + FALLBACK_PARAGRAPHS_PER_CHAPTER, paragraphs.size))
                val chunkContent = chunk.joinToString("\n")
                val chunkStart = segment.indexOf(chunkContent)
                
                chapters.add(Chapter(
                    title = "Chapter ${chapters.size + 1}",
                    startPos = offset + chunkStart,
                    endPos = offset + chunkStart + chunkContent.length,
                    isVolume = false,
                    detected = false
                ))
            }
        }

        return chapters
    }

    private fun createChineseChapterRegexps(): List<Regex> {
        val regexps = mutableListOf<Regex>()
        
        regexps.add(Regex(
            "(?:^|\\n)\\s*(" +
                listOf(
                    "第[ 　零〇一二三四五六七八九十0-9][ 　零〇一二三四五六七八九十百千万0-9]*(?:[章卷节回讲篇封本册部话])(?:[：:、 　\\(\\)0-9]*[^\\n-]{0,36})(?!\\S)",
                    "(?:楔子|前言|简介|引言|序言|序章|总论|概论|后记|番外篇|番外|外传)(?:[：: 　][^\\n-]{0,36})?(?!\\S)",
                    "chapter[\\s.]*[0-9]+(?:[：:. 　]+[^\\n-]{0,50})?(?!\\S)"
                ).joinToString("|") +
                ")",
            setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)
        ))

        regexps.add(Regex(
            "(?:^|\\n)\\s*(" +
                listOf(
                    "[一二三四五六七八九十][零〇一二三四五六七八九十百千万]?[：:、 　][^\\n-]{0,36}(?=\\n|$)",
                    "[0-9]+[^\\n]{0,16}(?=\\n|$)"
                ).joinToString("|") +
                ")"
        ))

        return regexps
    }

    private fun createSegmentRegex(linesBetweenSegments: Int): Regex {
        return Regex("(?:\\r?\\n){$linesBetweenSegments,}|-{8,}\\r?\\n")
    }

    private fun appendSegmentChapters(chapters: MutableList<Chapter>, segmentChapters: List<Chapter>) {
        for (chapter in segmentChapters) {
            val previous = chapters.lastOrNull()
            if (!chapter.detected && previous?.detected == true) {
                val mergedChapter = previous.copy(endPos = chapter.endPos)
                chapters[chapters.size - 1] = mergedChapter
            } else {
                chapters.add(chapter)
            }
        }
    }

    private fun isGoodMatches(matches: List<String>, maxLength: Int = 100000): Boolean {
        val meaningfulParts = matches.filter { it.isNotBlank() }
        if (meaningfulParts.size <= 1) return false
        return !meaningfulParts.any { it.length > maxLength }
    }

    private fun joinAroundUndefined(arr: List<String?>): List<String> {
        return arr.foldIndexed(mutableListOf()) { i, acc, curr ->
            when {
                curr == null && i > 0 && i < arr.size - 1 && 
                    arr[i - 1] != null && arr[i + 1] != null -> {
                    acc[acc.size - 1] = acc[acc.size - 1] + arr[i + 1]
                    acc
                }
                curr != null && (i == 0 || arr[i - 1] != null) -> {
                    acc.add(curr)
                    acc
                }
                else -> acc
            }
        }
    }

    private fun mergeShortChapters(chapters: List<Chapter>): List<Chapter> {
        if (chapters.size <= 1) return chapters

        val merged = mutableListOf<Chapter>()
        var currentChapter = chapters[0]

        for (i in 1 until chapters.size) {
            val nextChapter = chapters[i]
            val currentLength = currentChapter.endPos - currentChapter.startPos
            val nextLength = nextChapter.endPos - nextChapter.startPos

            if (currentLength < 100 && !currentChapter.detected) {
                currentChapter = currentChapter.copy(
                    endPos = nextChapter.endPos,
                    title = "${currentChapter.title} + ${nextChapter.title}"
                )
            } else {
                merged.add(currentChapter)
                currentChapter = nextChapter
            }
        }
        merged.add(currentChapter)

        return merged
    }

    companion object {
        fun extractMetadataFromFilename(filename: String): Pair<String, String?> {
            val baseName = filename.substringBeforeLast('.')
            
            val cjkMatch = Regex("《([^》]+)》(.*)").find(baseName)
            if (cjkMatch != null) {
                val title = cjkMatch.groupValues[1].trim()
                val rest = cjkMatch.groupValues.getOrNull(2)?.trim() ?: ""
                val author = parseAuthor(rest)
                return Pair(title, author)
            }

            val author = parseLabeledAuthor(baseName)
            return if (author.isNotEmpty()) {
                Pair(baseName, author)
            } else {
                Pair(baseName, null)
            }
        }

        private fun parseLabeledAuthor(text: String): String {
            val match = Regex("作者\\s*[：:]\\s*(.+)$").find(text)
            return match?.groupValues?.getOrNull(1)?.trim()?.let { stripPunctuation(it) } ?: ""
        }

        private fun parseAuthor(text: String): String? {
            if (text.isEmpty()) return null
            
            val labeled = parseLabeledAuthor(text)
            if (labeled.isNotEmpty()) return labeled
            
            val bracketed = Regex("[[(（【［]\\s*([^\\])）】］]+?)\\s*[\\])）】］]").find(text)
            if (bracketed != null) {
                return stripPunctuation(bracketed.groupValues[1])
            }
            
            val stripped = stripPunctuation(text)
            return if (stripped.isNotEmpty()) stripped else null
        }

        private fun stripPunctuation(text: String): String {
            return text.trim().replace(Regex("^[\\p{P}\\p{S}\\s]+|[\\p{P}\\p{S}\\s]+$"), "")
        }
    }
}