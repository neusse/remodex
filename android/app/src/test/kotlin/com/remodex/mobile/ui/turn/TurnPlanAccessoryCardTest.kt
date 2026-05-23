package com.remodex.mobile.ui.turn

import com.remodex.mobile.core.model.CodexMessage
import com.remodex.mobile.core.model.CodexMessageKind
import com.remodex.mobile.core.model.CodexMessageRole
import com.remodex.mobile.core.model.CodexPlanState
import com.remodex.mobile.core.model.CodexPlanStep
import com.remodex.mobile.core.model.CodexPlanStepStatus
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TurnPlanAccessoryCardTest {
    @Test
    fun `snapshot marks inProgress when any step is running`() {
        val message =
            basePlanMessage(
                steps =
                    listOf(
                        CodexPlanStep(step = "A", status = CodexPlanStepStatus.completed),
                        CodexPlanStep(step = "B", status = CodexPlanStepStatus.inProgress),
                    ),
            )
        val snapshot = PlanAccessorySnapshot.fromMessage(message)
        assertEquals(PlanAccessoryStatus.InProgress, snapshot.status)
        assertEquals("B", snapshot.summary)
        assertEquals("1/2", snapshot.progressText)
    }

    @Test
    fun `snapshot marks completed when all steps are complete`() {
        val message =
            basePlanMessage(
                steps =
                    listOf(
                        CodexPlanStep(step = "A", status = CodexPlanStepStatus.completed),
                        CodexPlanStep(step = "B", status = CodexPlanStepStatus.completed),
                    ),
            )
        val snapshot = PlanAccessorySnapshot.fromMessage(message)
        assertEquals(PlanAccessoryStatus.Completed, snapshot.status)
        assertEquals("B", snapshot.summary)
    }

    @Test
    fun `snapshot falls back to explanation when no steps exist`() {
        val message =
            basePlanMessage(
                steps = emptyList(),
                explanation = "Explain first",
            )
        val snapshot = PlanAccessorySnapshot.fromMessage(message)
        assertEquals(PlanAccessoryStatus.Pending, snapshot.status)
        assertEquals("Explain first", snapshot.summary)
        assertNull(snapshot.progressText)
    }

    @Test
    fun `plan bar keeps latest created plan even when all steps are completed`() {
        val completed =
            basePlanMessage(
                id = "completed",
                steps = listOf(CodexPlanStep(step = "Done", status = CodexPlanStepStatus.completed)),
            )
        val active =
            basePlanMessage(
                id = "active",
                steps = listOf(CodexPlanStep(step = "Work", status = CodexPlanStepStatus.inProgress)),
            )

        val selected = selectPlanBarMessage(listOf(active, completed))
        assertEquals("completed", selected?.id)
    }

    @Test
    fun `plan bar skips hidden and closed plan ids`() {
        val oldActive =
            basePlanMessage(
                id = "old",
                steps = listOf(CodexPlanStep(step = "Old", status = CodexPlanStepStatus.inProgress)),
            )
        val latestActive =
            basePlanMessage(
                id = "latest",
                steps = listOf(CodexPlanStep(step = "Latest", status = CodexPlanStepStatus.pending)),
            )

        assertEquals(
            "old",
            selectPlanBarMessage(
                messages = listOf(oldActive, latestActive),
                hiddenMessageIds = listOf("latest"),
            )?.id,
        )
        assertNull(
            selectPlanBarMessage(
                messages = listOf(oldActive, latestActive),
                hiddenMessageIds = listOf("latest"),
                closedMessageIds = listOf("old"),
            ),
        )
    }

    @Test
    fun `proposed plan parser extracts plan body and leaves surrounding markdown`() {
        val source =
            """
            Intro

            <proposed_plan>
            # Plan

            - Do work
            </proposed_plan>

            Outro
            """.trimIndent()

        val extraction = ProposedPlanBlockParser.extract(source)

        assertEquals("Intro\n\nOutro", extraction.visibleMarkdown)
        assertEquals(1, extraction.plans.size)
        assertEquals("# Plan\n\n- Do work", extraction.plans.single().markdown)
    }

    @Test
    fun `proposed plan parser ignores empty plan tags`() {
        val extraction = ProposedPlanBlockParser.extract("Before <proposed_plan> </proposed_plan> After")

        assertEquals("Before  After", extraction.visibleMarkdown)
        assertEquals(emptyList<ProposedPlanBlock>(), extraction.plans)
    }

    private fun basePlanMessage(
        id: String = "id",
        isStreaming: Boolean = false,
        steps: List<CodexPlanStep>,
        explanation: String = "Plan",
    ): CodexMessage =
        CodexMessage(
            id = id,
            threadId = "thread",
            role = CodexMessageRole.system,
            kind = CodexMessageKind.plan,
            text = "plan",
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            isStreaming = isStreaming,
            planState = CodexPlanState(explanation = explanation, steps = steps),
        )
}
