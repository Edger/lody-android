package app.lody.android.chat

import app.lody.android.runtime.DiffScope
import app.lody.android.runtime.WorkspaceChange

/**
 * The working tree list's row count and, when every file reports both sides, its ± totals.
 * [files] is the list length, including binary and deferred entries; [totals] is null when any
 * file omitted a side, because a partial total would read as the whole change.
 */
internal data class UncommittedSummary(val files: Int, val totals: WorkspaceDiffTotals?)

internal data class WorkspaceDiffTotals(val added: Int, val removed: Int, val files: Int)

internal fun uncommittedSummary(files: List<WorkspaceChange>): UncommittedSummary {
    var added = 0
    var removed = 0
    var complete = true
    for (file in files) {
        val lineAdded = file.added
        val lineRemoved = file.removed
        if (lineAdded == null || lineRemoved == null) complete = false else {
            added += lineAdded
            removed += lineRemoved
        }
    }
    return UncommittedSummary(
        files = files.size,
        totals = if (complete) WorkspaceDiffTotals(added = added, removed = removed, files = files.size) else null,
    )
}

/**
 * The menu's change rows. The live working tree list can add the Uncommitted row before the
 * catalog reports the change, but never removes rows: it covers HEAD against the working tree, so
 * it cannot tell whether the branch also has committed changes.
 */
internal fun workspaceDiffScopes(catalogHasDiff: Boolean, liveFiles: Int): List<DiffScope> = buildList {
    if (catalogHasDiff) add(DiffScope.Branch)
    if (catalogHasDiff || liveFiles > 0) add(DiffScope.Uncommitted)
}
