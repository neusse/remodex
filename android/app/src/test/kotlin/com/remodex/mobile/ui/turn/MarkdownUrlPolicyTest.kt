package com.remodex.mobile.ui.turn

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MarkdownUrlPolicyTest {
    @Test
    fun allowsOnlyExpectedExternalSchemes() {
        assertTrue(MarkdownUrlPolicy.isAllowedLink("https://example.com"))
        assertTrue(MarkdownUrlPolicy.isAllowedLink("http://example.com"))
        assertTrue(MarkdownUrlPolicy.isAllowedLink("mailto:security@example.com"))

        assertFalse(MarkdownUrlPolicy.isAllowedLink("intent://scan/#Intent;scheme=zxing;end"))
        assertFalse(MarkdownUrlPolicy.isAllowedLink("file:///data/data/com.remodex.mobile/shared_prefs/store.xml"))
        assertFalse(MarkdownUrlPolicy.isAllowedLink("content://com.example.provider/item"))
        assertFalse(MarkdownUrlPolicy.isAllowedLink("javascript:alert(1)"))
        assertFalse(MarkdownUrlPolicy.isAllowedLink("./local/file.kt"))
    }

    @Test
    fun repoMarkdownPreviewPathAcceptsUncommonLocalFileExtensions() {
        assertEquals("example.ex", RepoMarkdownFileLink.previewPath("example.ex"))
        assertEquals("./src/example.ex", RepoMarkdownFileLink.previewPath("./src/example.ex"))
    }

    @Test
    fun repoMarkdownPreviewPathDropsLineSuffixes() {
        assertEquals("README.md", RepoMarkdownFileLink.previewPath("README.md:12"))
        assertEquals("./Docs/README.md", RepoMarkdownFileLink.previewPath("./Docs/README.md:12:3"))
        assertEquals("C:/repo/Docs/README.md", RepoMarkdownFileLink.previewPath("file:///C:/repo/Docs/README.md:12"))
        assertEquals("./Docs/README.md", RepoMarkdownFileLink.previewPath("./Docs/README.md:12#L12"))
    }

    @Test
    fun repoMarkdownPreviewCwdUsesAbsoluteLinkParent() {
        assertEquals("C:/repo/Docs", RepoMarkdownFileLink.previewWorkspaceCwd("C:/repo/Docs/README.md"))
        assertEquals("/Users/me/repo/Docs", RepoMarkdownFileLink.previewWorkspaceCwd("/Users/me/repo/Docs/README.md"))
        assertEquals(null, RepoMarkdownFileLink.previewWorkspaceCwd("./Docs/README.md"))
    }
}
