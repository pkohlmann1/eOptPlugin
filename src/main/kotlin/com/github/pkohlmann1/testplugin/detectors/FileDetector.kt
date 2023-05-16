package com.github.pkohlmann1.testplugin.detectors

import java.util.regex.Matcher
import java.util.regex.Pattern

class FileDetector{
    data class FileReference(val fileName: String, val line: Int, val column: Int)
    // Regular expression patterns to detect image, video and document file extensions
    private val IMAGE_PATTERN:Pattern=Pattern.compile(".jpg|.jpeg|.png|.gif|.bmp$")
    private val VIDEO_PATTERN:Pattern=Pattern.compile(".mp4|.avi|.mov|.wmv|.flv|.mkv$")
    private val DOCUMENT_PATTERN:Pattern=Pattern.compile(".pdf|.docx?|.xlsx?|.pptx?|.txt$")

    /**
     * Detects file references in the given string and returns a list of FileReference objects
     * containing the file name and the line and column where the reference occurs.
     */
    fun detectFiles(input: String): List<Triple<String, Int, Int>> {
        val references = mutableListOf<Triple<String, Int, Int>>()
        val lines = input.lines()
        for (i in lines.indices) {
            val line = lines[i]
            var column = 0
            var matcher: Matcher
            matcher = IMAGE_PATTERN.matcher(line)
            while (matcher.find()) {
                val fileName = matcher.group()
                val reference = Triple(fileName, i + 1, matcher.start() + 1 + column)
                references.add(reference)
                column += matcher.end() - matcher.start()
            }
            matcher = VIDEO_PATTERN.matcher(line)
            while (matcher.find()) {
                val fileName = matcher.group()
                val reference = Triple(fileName, i + 1, matcher.start() + 1 + column)
                references.add(reference)
                column += matcher.end() - matcher.start()
            }
            matcher = DOCUMENT_PATTERN.matcher(line)
            while (matcher.find()) {
                val fileName = matcher.group()
                val reference = Triple(fileName, i + 1, matcher.start() + 1 + column)
                references.add(reference)
                column += matcher.end() - matcher.start()
            }
        }
        return references
    }
}
