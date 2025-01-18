package fr.enssat.singwithme.charbonneauGilles.model

import org.commonmark.node.Node
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import java.util.regex.Pattern

data class LyricLine(val timecode: String, val lyric: String)

fun parseMarkdown(markdown: String): List<LyricLine> {
    val parser: Parser = Parser.builder().build()
    val document: Node = parser.parse(markdown)
    val renderer = HtmlRenderer.builder().build()
    val htmlContent = renderer.render(document)

    val lyricLines = mutableListOf<LyricLine>()
    val pattern = Pattern.compile("\\s*\\{([^}]+)\\}\\s*([^{}]+)")
    val matcher = pattern.matcher(htmlContent)

    while (matcher.find()) {
        val timecode = matcher.group(1)?.trim() ?: ""
        val lyric = matcher.group(2)?.trim() ?: ""
        lyricLines.add(LyricLine(timecode, lyric))
    }

    return lyricLines
}
