package com.remodex.mobile.ui.turn

import com.remodex.mobile.core.error.CodexServiceError
import com.remodex.mobile.core.model.RPCError
import kotlin.test.Test
import kotlin.test.assertEquals

class VoiceTranscriptionErrorFormattingTest {
    @Test
    fun voiceRpcErrorUsesBridgeMessageWithoutRpcPrefix() {
        val error =
            CodexServiceError.RpcFailure(
                RPCError(
                    code = -32000,
                    message = "Your ChatGPT login has expired. Sign in again.",
                    data = null,
                ),
            )

        assertEquals(
            "Your ChatGPT login has expired. Sign in again.",
            formatVoiceTranscriptionError(error, "Voice transcription failed."),
        )
    }
}
