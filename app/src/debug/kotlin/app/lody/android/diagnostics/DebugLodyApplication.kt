package app.lody.android.diagnostics

import app.lody.android.LodyApplication
import app.lody.android.runtime.*
import kotlinx.coroutines.launch

class DebugLodyApplication : LodyApplication() {
    companion object {
        private const val FIXTURE_ACCOUNT = "offline-fixture"
        val fixtureScope = RuntimeScope(FIXTURE_ACCOUNT, "workspace-a")
    }
    override fun createOfflineRuntime(scope: RuntimeScope): CloudRuntime = DiagnosticRuntime(this, scope)
    // The offline fixture has no account, workspace catalog or plaintext mode to verify.
    override fun admitRuntime(scope: RuntimeScope) {
        if (scope.accountId != FIXTURE_ACCOUNT) account.requirePlaintext(scope)
    }
    override fun onCreate() {
        super.onCreate()
        applicationScope.launch { runtimeOwner.activate(fixtureScope) }
    }
}
