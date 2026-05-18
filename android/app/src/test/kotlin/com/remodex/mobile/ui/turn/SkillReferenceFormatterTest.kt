package com.remodex.mobile.ui.turn

import org.junit.Assert.assertEquals
import org.junit.Test

class SkillReferenceFormatterTest {
    @Test
    fun formatsMentionTokensInVisibleProse() {
        assertEquals(
            "Use Code Review before sending.",
            SkillReferenceFormatter.formatVisibleProse("Use \$code-review before sending."),
        )
    }

    @Test
    fun preservesInlineCodeMentions() {
        assertEquals(
            "Run `\$code-review` after Code Review.",
            SkillReferenceFormatter.formatVisibleProse("Run `\$code-review` after \$code-review."),
        )
    }

    @Test
    fun preservesFencedCodeBlocks() {
        val input =
            """
            Use ${'$'}code-review here.
            ```text
            ${'$'}code-review stays raw
            ```
            Then ${'$'}bug-fix.
            """.trimIndent()

        val expected =
            """
            Use Code Review here.
            ```text
            ${'$'}code-review stays raw
            ```
            Then Bug Fix.
            """.trimIndent()

        assertEquals(expected, SkillReferenceFormatter.formatVisibleProse(input))
    }

    @Test
    fun formatsExplicitSkillMarkdownLinks() {
        assertEquals(
            "Use Code Review now.",
            SkillReferenceFormatter.formatVisibleProse("Use [ignored](skill:code-review) now."),
        )
    }

    @Test
    fun leavesNormalMarkdownLinksAlone() {
        val input = "Open [docs](https://example.test/skills)."
        assertEquals(input, SkillReferenceFormatter.formatVisibleProse(input))
    }

    @Test
    fun formatsCodexSkillPathReferenceInProse() {
        val input = "See C:/Users/andre/.codex/skills/code-review/Skill.md for details."
        assertEquals("See Code Review for details.", SkillReferenceFormatter.formatVisibleProse(input))
    }

    @Test
    fun formatsAgentsSkillPathReferenceInProse() {
        val input = "Read /.agents/skills/bug-fix/Skill.md before continuing."
        assertEquals("Read Bug Fix before continuing.", SkillReferenceFormatter.formatVisibleProse(input))
    }

    @Test
    fun preservesSkillPathInsideInlineCode() {
        val input = "Use `/.codex/skills/code-review/Skill.md` and /.codex/skills/code-review/Skill.md."
        val expected = "Use `/.codex/skills/code-review/Skill.md` and Code Review."
        assertEquals(expected, SkillReferenceFormatter.formatVisibleProse(input))
    }

    @Test
    fun removesSearchCitationMarkersFromVisibleProse() {
        val input =
            "The docs list `turn/steer` as a real method.\uE200cite\uE202turn0search2\uE202turn0search3\uE201"

        assertEquals(
            "The docs list `turn/steer` as a real method.",
            SkillReferenceFormatter.formatVisibleProse(input),
        )
    }

    @Test
    fun removesSearchCitationMarkersWithLargeTurnAndSearchNumbers() {
        val input = "The current contract is clear.\uE200cite\uE202turn11103search49005\uE201"

        assertEquals(
            "The current contract is clear.",
            SkillReferenceFormatter.formatVisibleProse(input),
        )
    }

    @Test
    fun extractsSearchCitationRefsFromVisibleProse() {
        val input =
            "The docs list it.\uE200cite\uE202turn0search2\uE202turn11103search49005\uE201 " +
                "and again 〖cite〗turn0search2〖/cite〗"

        assertEquals(
            listOf("turn0search2", "turn11103search49005"),
            SkillReferenceFormatter.extractSearchCitations(input),
        )
    }

    @Test
    fun doesNotExtractSearchCitationRefsInsideCode() {
        val input =
            """
            Keep `\uE200cite\uE202turn0search2\uE201` inline.
            ```text
            \uE200cite\uE202turn0search3\uE201
            ```
            """.trimIndent()

        assertEquals(emptyList<String>(), SkillReferenceFormatter.extractSearchCitations(input))
    }

    @Test
    fun removesBracketedSearchCitationMarkersFromVisibleProse() {
        val input = "This changes the conclusion. 〖cite〗turn0search2turn0search3〖/cite〗"

        assertEquals(
            "This changes the conclusion. ",
            SkillReferenceFormatter.formatVisibleProse(input),
        )
    }

    @Test
    fun removesMalformedBracketedSearchCitationMarkersFromVisibleProse() {
        val input = "Follow-up behavior. 〖cite〗turn1search0〖turn1search3〗"

        assertEquals(
            "Follow-up behavior. ",
            SkillReferenceFormatter.formatVisibleProse(input),
        )
    }

    @Test
    fun preservesNonNumericSearchCitationMarkersFromVisibleProse() {
        val input = "Search source.\u3016cite\u3017turnxsearchyturnAlphaSearchBeta\u3016/cite\u3017"

        assertEquals(input, SkillReferenceFormatter.formatVisibleProse(input))
    }

    @Test
    fun preservesCitationMarkersInsideCode() {
        val input = "Keep `\uE200cite\uE202turn0search2\uE201` literal."

        assertEquals(input, SkillReferenceFormatter.formatVisibleProse(input))
    }
}
