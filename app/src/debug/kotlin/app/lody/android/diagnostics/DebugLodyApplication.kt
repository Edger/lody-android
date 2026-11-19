package app.lody.android.diagnostics

import app.lody.android.LodyApplication
import app.lody.android.runtime.*
import kotlinx.coroutines.launch

class DebugLodyApplication : LodyApplication() {
    companion object { val fixtureScope = RuntimeScope("offline-fixture", "workspace-a") }
    override fun admitRuntime(scope: RuntimeScope) = admitDebugRuntime(scope) { super.admitRuntime(it) }
    override fun createOfflineRuntime(scope: RuntimeScope): CloudRuntime = DiagnosticRuntime(this, scope)
    override fun onCreate() {
        super.onCreate()
        applicationScope.launch { runtimeOwner.activate(fixtureScope) }
    }
}

internal fun admitDebugRuntime(scope: RuntimeScope, admitAccount: (RuntimeScope) -> Unit) {
    if (scope.accountId != DebugLodyApplication.fixtureScope.accountId) admitAccount(scope)
}
