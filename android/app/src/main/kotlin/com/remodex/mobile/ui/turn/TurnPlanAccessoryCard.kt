package com.remodex.mobile.ui.turn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.remodex.mobile.R
import com.remodex.mobile.core.model.CodexMessage
import com.remodex.mobile.core.model.CodexMessageKind
import com.remodex.mobile.core.model.CodexMessageRole
import com.remodex.mobile.core.model.CodexPlanStep
import com.remodex.mobile.core.model.CodexPlanStepStatus
import com.remodex.mobile.ui.theme.isAgentLightChrome
import com.valentinilk.shimmer.shimmer

@Composable
internal fun TurnPlanBar(
    message: CodexMessage,
    onOpenDetails: () -> Unit,
    onHide: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snapshot = PlanAccessorySnapshot.fromMessage(message)
    val lightChrome = isAgentLightChrome()
    val statusTint =
        if (lightChrome) {
            when (snapshot.status) {
                PlanAccessoryStatus.Pending -> Color(0xFFE4C25F)
                PlanAccessoryStatus.InProgress -> Color(0xFFF1D475)
                PlanAccessoryStatus.Completed -> Color(0xFFAED2C0)
            }
        } else {
            when (snapshot.status) {
                PlanAccessoryStatus.Pending -> MaterialTheme.colorScheme.tertiary
                PlanAccessoryStatus.InProgress -> MaterialTheme.colorScheme.primary
                PlanAccessoryStatus.Completed -> MaterialTheme.colorScheme.secondary
            }
        }
    val background =
        if (lightChrome) {
            Color(0xFF202326).copy(alpha = 0.95f)
        } else {
            Color(0xFF5A4312).copy(alpha = 0.56f)
        }
    val contentTint = if (lightChrome) Color(0xFFF4F2EC) else MaterialTheme.colorScheme.onPrimaryContainer
    val secondaryTint = contentTint.copy(alpha = 0.72f)

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .shimmer(),
        shape = RoundedCornerShape(14.dp),
        color = background,
        contentColor = contentTint,
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 5.dp, bottom = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = statusTint.copy(alpha = 0.18f),
                modifier = Modifier.size(18.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = statusTint,
                    modifier = Modifier.size(8.dp),
                ) {}
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.turn_timeline_kind_plan),
                        style = MaterialTheme.typography.labelMedium,
                        color = contentTint,
                    )
                    Text(
                        text =
                            when (snapshot.status) {
                                PlanAccessoryStatus.Pending -> stringResource(R.string.turn_plan_status_pending)
                                PlanAccessoryStatus.InProgress -> stringResource(R.string.turn_plan_status_in_progress)
                                PlanAccessoryStatus.Completed -> stringResource(R.string.turn_plan_status_completed)
                            },
                        style = MaterialTheme.typography.labelSmall,
                        color = statusTint,
                    )
                    snapshot.progressText?.let { progress ->
                        Text(
                            text = progress,
                            style = MaterialTheme.typography.labelSmall,
                            color = secondaryTint,
                        )
                    }
                    if (message.isStreaming) {
                        Text(
                            text = stringResource(R.string.turn_plan_status_streaming),
                            style = MaterialTheme.typography.labelSmall,
                            color = secondaryTint,
                        )
                    }
                }
                Text(
                    text = snapshot.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentTint.copy(alpha = 0.94f),
                    maxLines = 1,
                )
            }
            TextButton(onClick = onOpenDetails) {
                Text(
                    text = stringResource(R.string.turn_message_see_more),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentTint,
                )
            }
            IconButton(onClick = onHide) {
                Icon(
                    imageVector = Icons.Filled.VisibilityOff,
                    contentDescription = stringResource(R.string.turn_plan_hide_cd),
                    tint = secondaryTint,
                )
            }
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.turn_plan_close_cd),
                    tint = secondaryTint,
                )
            }
        }
    }
}

internal enum class PlanAccessoryStatus {
    Pending,
    InProgress,
    Completed,
}

internal data class PlanAccessorySnapshot(
    val summary: String,
    val status: PlanAccessoryStatus,
    val completedStepCount: Int,
    val totalStepCount: Int,
    val steps: List<CodexPlanStep>,
) {
    val progressText: String?
        get() = if (totalStepCount > 0) "$completedStepCount/$totalStepCount" else null

    companion object {
        fun fromMessage(message: CodexMessage): PlanAccessorySnapshot {
            val steps = message.planState?.steps.orEmpty()
            val completed = steps.count { it.status == CodexPlanStepStatus.completed }
            return PlanAccessorySnapshot(
                summary = resolveSummary(message, steps),
                status = resolveStatus(steps, completed),
                completedStepCount = completed,
                totalStepCount = steps.size,
                steps = steps,
            )
        }

        private fun resolveStatus(
            steps: List<CodexPlanStep>,
            completedStepCount: Int,
        ): PlanAccessoryStatus {
            if (steps.any { it.status == CodexPlanStepStatus.inProgress }) return PlanAccessoryStatus.InProgress
            if (steps.isNotEmpty() && completedStepCount == steps.size) return PlanAccessoryStatus.Completed
            return PlanAccessoryStatus.Pending
        }

        private fun resolveSummary(
            message: CodexMessage,
            steps: List<CodexPlanStep>,
        ): String {
            steps.firstOrNull { it.status == CodexPlanStepStatus.inProgress }?.let { return it.step }
            steps.firstOrNull { it.status == CodexPlanStepStatus.pending }?.let { return it.step }
            steps.lastOrNull()?.let { return it.step }
            val explanation = message.planState?.explanation?.trim().orEmpty()
            if (explanation.isNotEmpty()) return explanation
            return message.text.trim().takeIf { it.isNotEmpty() } ?: "Open plan details"
        }
    }
}

internal fun selectPlanBarMessage(
    messages: List<CodexMessage>,
    hiddenMessageIds: Collection<String> = emptySet(),
    closedMessageIds: Collection<String> = emptySet(),
): CodexMessage? {
    val hidden = hiddenMessageIds.toSet()
    val closed = closedMessageIds.toSet()
    return messages
        .asReversed()
        .firstOrNull { message ->
            message.id !in hidden &&
                message.id !in closed &&
                message.shouldDisplayPlanBarAccessory()
        }
}

internal fun CodexMessage.shouldDisplayPlanBarAccessory(): Boolean {
    if (role != CodexMessageRole.system || kind != CodexMessageKind.plan) return false
    if (isStreaming) return true
    val state = planState ?: return text.trim().isNotEmpty()
    return state.explanation?.trim()?.isNotEmpty() == true || state.steps.isNotEmpty()
}

internal fun planMarkdownFromMessage(message: CodexMessage): String {
    val state = message.planState
    val parts = mutableListOf<String>()
    state?.explanation?.trim()?.takeIf { it.isNotEmpty() }?.let { parts += it }
    val steps = state?.steps.orEmpty()
    if (steps.isNotEmpty()) {
        parts +=
            steps.mapIndexed { index, step ->
                val status =
                    when (step.status) {
                        CodexPlanStepStatus.completed -> "done"
                        CodexPlanStepStatus.inProgress -> "doing"
                        CodexPlanStepStatus.pending -> "todo"
                    }
                "${index + 1}. **[$status]** ${step.step}"
            }.joinToString("\n")
    }
    val body = message.text.trim()
    if (parts.isEmpty() && body.isNotEmpty() && body != "plan") {
        parts += body
    }
    return parts.joinToString("\n\n").trim().ifEmpty { "Plan generated." }
}

internal data class ProposedPlanBlock(
    val markdown: String,
    val startIndex: Int,
    val endIndex: Int,
)

internal data class ProposedPlanExtraction(
    val visibleMarkdown: String,
    val plans: List<ProposedPlanBlock>,
)

internal object ProposedPlanBlockParser {
    private val planRegex =
        Regex("""(?is)<\s*proposed_plan\s*>\s*(.*?)\s*<\s*/\s*proposed_plan\s*>""")

    fun extract(markdown: String): ProposedPlanExtraction {
        val matches = planRegex.findAll(markdown).toList()
        if (matches.isEmpty()) {
            return ProposedPlanExtraction(visibleMarkdown = markdown, plans = emptyList())
        }

        val visible = StringBuilder()
        val plans = mutableListOf<ProposedPlanBlock>()
        var cursor = 0
        matches.forEach { match ->
            if (match.range.first > cursor) {
                visible.append(markdown.substring(cursor, match.range.first))
            }
            match.groups[1]?.value?.trim()?.takeIf { it.isNotEmpty() }?.let { body ->
                plans +=
                    ProposedPlanBlock(
                        markdown = body,
                        startIndex = match.range.first,
                        endIndex = match.range.last + 1,
                    )
            }
            cursor = match.range.last + 1
        }
        if (cursor < markdown.length) {
            visible.append(markdown.substring(cursor))
        }

        return ProposedPlanExtraction(
            visibleMarkdown = visible.toString().replace(Regex("""\n{3,}"""), "\n\n").trim(),
            plans = plans,
        )
    }
}
