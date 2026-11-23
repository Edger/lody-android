package app.lody.android.diagnostics

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import app.lody.android.chat.FilesSheet
import app.lody.android.chat.WorkspaceChangesSheet
import app.lody.android.designsystem.LodyColors
import app.lody.android.designsystem.LodyTheme
import app.lody.android.runtime.FileChange
import app.lody.android.runtime.FileDiff
import app.lody.android.runtime.WorkspaceChange
import app.lody.android.runtime.WorkspaceChanges

/**
 * Debug-only diff surface: the production FilesSheet (or the working-tree sheet with
 * `--ez workspace true`) and DiffPage over fixed text, so the Unified / Split, wrap and More
 * controls can be exercised without a signed-in conversation.
 *
 * adb shell am start -n ai.lody.android/app.lody.android.diagnostics.DiffProbeActivity [--ez workspace true]
 */
class DiffProbeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val workspace = intent.getBooleanExtra("workspace", false)
        setContent {
            LodyTheme {
                Box(Modifier.fillMaxSize().background(LodyTheme.colors.bgBase)) {
                    if (workspace) {
                        WorkspaceChangesSheet(
                            loadChanges = {
                                WorkspaceChanges.Ready(
                                    Fixtures.map { WorkspaceChange(it.path, it.added, it.removed) },
                                )
                            },
                            loadDiff = { path -> diffFor(path) },
                            onDismiss = {},
                        )
                    } else {
                        FilesSheet(entryId = "probe", files = Fixtures, loadDiff = { _, path -> diffFor(path) }, onDismiss = {})
                    }
                }
            }
        }
    }
}

private val Fixtures = listOf(
    FileChange("app/src/main/kotlin/app/lody/android/auth/AuthModels.kt", 11, 1),
    FileChange("app/src/main/kotlin/app/lody/android/auth/VeryLongDependencyNamesInAWorkspaceMemberList.kt", 3, 3),
    FileChange("app/src/main/kotlin/app/lody/android/auth/LongSingleLine.kt", 1, 1),
    FileChange("app/src/main/kotlin/app/lody/android/auth/LongSingleLine5k.kt", 1, 1),
    FileChange("app/src/main/kotlin/app/lody/android/auth/LongSingleLine10k.kt", 1, 1),
    FileChange("app/src/main/kotlin/app/lody/android/auth/LongSingleLine20k.kt", 1, 1),
    FileChange("app/src/main/kotlin/app/lody/android/auth/LongSingleLine1M.kt", 1, 1),
    FileChange("app/src/main/kotlin/app/lody/android/auth/HighLineNumbers.kt", 1, 1),
    FileChange("app/src/main/kotlin/app/lody/android/features/workspaces/members/directory/withAnExtremelyLongFolderName/andAnotherNestedFolder/andOneMoreLevelForGoodMeasure/LongDirectoryModel.kt", 1, 1),
)

private fun diffFor(path: String): FileDiff = when {
    path.endsWith("LongDirectoryModel.kt") -> FileDiff(
        path = path,
        oldText = "val directory = 0\n",
        newText = "val directory = 1\n",
        added = 1,
        removed = 1,
        base = "current",
    )
    path.endsWith("LongSingleLine5k.kt") -> longSingleLine(path, 5_000)
    path.endsWith("LongSingleLine10k.kt") -> longSingleLine(path, 10_000)
    path.endsWith("LongSingleLine20k.kt") -> longSingleLine(path, 20_000)
    path.endsWith("LongSingleLine1M.kt") -> longSingleLine(path, 1_200_000)
    path.endsWith("HighLineNumbers.kt") -> highLineNumbers(path)
    path.endsWith("LongSingleLine.kt") -> FileDiff(
        path = path,
        oldText = "val generated = 0\n",
        newText = "val generated = " + "veryLongToken".repeat(20_000) + "\n",
        added = 1,
        removed = 1,
        base = "current",
    )
    path.endsWith("VeryLongDependencyNamesInAWorkspaceMemberList.kt") -> FileDiff(
        path = path,
        oldText = "val member = WorkspaceMember(id = veryLongIdentifiersForTheFirstField, name = anotherVeryLongIdentifierForTheSecondField, image = aThirdIdentifier)\n",
        newText = "val member = WorkspaceMember(id = veryLongIdentifiersForTheFirstField, name = anotherVeryLongIdentifierForTheSecondField, image = aThirdIdentifier, role = aFourthVeryLongIdentifierThatNeedsHorizontalScrolling)\n",
        added = 3,
        removed = 3,
        base = "current",
    )
    else -> FileDiff(
        path = path,
        oldText = Old,
        newText = New,
        added = 11,
        removed = 1,
        base = "turn",
    )
}

/** One generated line of [chars] characters, no spaces: the worst case for text line breaking. */
private fun longSingleLine(path: String, chars: Int) = FileDiff(
    path = path,
    oldText = "val generated = 0\n",
    newText = "val generated = " + "x".repeat(chars) + "\n",
    added = 1,
    removed = 1,
    base = "current",
)

/** A change at line 10,000, so five-digit numbers exercise the fixed number column. */
private fun highLineNumbers(path: String): FileDiff {
    val old = (1..10_000).joinToString("") { "val line$it = $it\n" }
    return FileDiff(
        path = path,
        oldText = old,
        newText = old.replace("val line10000 = 10000", "val line10000 = 10_000"),
        added = 1,
        removed = 1,
        base = "current",
    )
}

private const val Old = """package app.lody.android.auth

data class Credential(val token: String) {
    override fun toString() = "Credential(<redacted>)"
}
/** [image] is the workspace logo (HTTPS only), null when it has none. */
data class WorkspaceMember(val id: String, val name: String, val image: String? = null)

data class Workspace(val id: String, val name: String, val members: List<WorkspaceMember>)
"""

private const val New = """package app.lody.android.auth

data class Credential(val token: String) {
    override fun toString() = "Credential(<redacted>)"
}
/** [image] is the workspace logo (HTTPS only), null when it has none. */
/** [role] is the Better Auth organization role (comma-separated for several); null when unknown. */
data class WorkspaceMember(val id: String, val name: String, val image: String? = null, val role: String? = null) {
    /**
     * A known role without workspace write access (Lody's write roles are owner, admin and
     * member, code-collaborator).
     * Unknown roles are not read-only; the server still decides.
     */
    val writes: Boolean get() = role?.split(',')?.any { it.trim() in setOf("owner", "admin", "member", "code-collaborator") } == true
}

data class Workspace(val id: String, val name: String, val members: List<WorkspaceMember>)
"""
