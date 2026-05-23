package com.remodex.mobile.data



import com.remodex.mobile.core.model.PendingStructuredInputQuestion



/**

 * Plain-text body for ephemeral structured-input timeline rows (no Android resources).

 */

internal object StructuredInputTimelineFormatter {

    fun bodyText(questions: List<PendingStructuredInputQuestion>): String {

        val qs = questions.map { sanitizeQuestion(it) }.filter { it.isNotEmpty() }

        return when {

            qs.isEmpty() -> "Input requested"

            qs.size == 1 -> qs.single()

            else ->

                qs

                    .mapIndexed { i, line -> "${i + 1}. $line" }

                    .joinToString("\n\n")

        }

    }

    fun proposedPlanMarkdown(questions: List<PendingStructuredInputQuestion>): String? {
        val question =
            questions.firstOrNull { q ->
                val marker = listOf(q.header, q.question, q.id).joinToString(" ").lowercase()
                marker.contains("proposed plan") || marker.contains("proposed_plan")
            } ?: return null
        return sanitizeQuestion(question).takeIf { it.isNotEmpty() }
    }



    private fun sanitizeQuestion(question: PendingStructuredInputQuestion): String {

        val q = question.question.trim()

        val h = question.header.trim()

        val id = question.id.trim()

        return when {

            q.isNotEmpty() -> q

            h.isNotEmpty() -> h

            else -> id

        }

    }

}


