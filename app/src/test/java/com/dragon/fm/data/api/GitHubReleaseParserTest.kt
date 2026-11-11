package com.dragon.fm.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubReleaseParserTest {

    @Test
    fun parsesLatestReleaseAndChoosesApkAsset() {
        val info = GitHubReleaseParser.parseRelease(
            """
            {
              "tag_name": "v1.0.2",
              "body": "修复升级与备份顺序",
              "published_at": "2026-11-12T03:20:00Z",
              "draft": false,
              "prerelease": false,
              "assets": [
                {"name":"checksum.txt","browser_download_url":"https://example.com/checksum.txt","size":20},
                {"name":"app-release.apk","browser_download_url":"https://example.com/app-release.apk","size":24000000}
              ]
            }
            """.trimIndent()
        )

        assertEquals("1.0.2", info.versionName)
        assertEquals("https://example.com/app-release.apk", info.downloadUrl)
        assertEquals("2026-11-12", info.releaseDate)
        assertEquals(26111203, info.versionCode)
    }

    @Test
    fun rejectsDraftAndPrereleasePayloads() {
        val draft = GitHubReleaseParser.parseRelease(
            """{"tag_name":"v9.0.0","draft":true,"assets":[]}"""
        )
        val prerelease = GitHubReleaseParser.parseRelease(
            """{"tag_name":"v9.0.0","prerelease":true,"assets":[]}"""
        )

        assertEquals("", draft.downloadUrl)
        assertEquals("", prerelease.downloadUrl)
    }

    @Test
    fun comparesReleaseCodeAndSemanticVersion() {
        assertTrue(
            GitHubReleaseParser.isNewer(
                candidateName = "1.0.2",
                candidateCode = 26111203,
                currentName = "1.0.1",
                currentCode = 26110809,
            )
        )
        assertTrue(
            GitHubReleaseParser.isNewer(
                candidateName = "1.1.0",
                candidateCode = 26110809,
                currentName = "1.0.9",
                currentCode = 26110809,
            )
        )
        assertFalse(
            GitHubReleaseParser.isNewer(
                candidateName = "1.0.1",
                candidateCode = 26110809,
                currentName = "1.0.1",
                currentCode = 26110809,
            )
        )
    }
}
