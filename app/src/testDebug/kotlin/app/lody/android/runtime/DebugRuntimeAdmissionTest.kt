package app.lody.android.runtime

import app.lody.android.auth.AuthFailure
import app.lody.android.diagnostics.DebugLodyApplication
import app.lody.android.diagnostics.admitDebugRuntime
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class DebugRuntimeAdmissionTest {
    private class OfflineRuntime(override val scope: RuntimeScope) : CloudRuntime {
        override val state = MutableStateFlow(RuntimeStatus())
        override val events = MutableSharedFlow<RuntimeEmission>()
        override suspend fun start() { state.value = RuntimeStatus(phase = RuntimePhase.READY) }
        override suspend fun stop() { state.value = RuntimeStatus(phase = RuntimePhase.STOPPED) }
        override suspend fun setActive(active: Boolean) = Unit
        override suspend fun execute(command: RuntimeCommand): RuntimeResult = RuntimeResult.Pong(true)
    }

    @Test fun offlineFixtureStartsRestartsAndSwitchesWithoutAnAccount() = runTest {
        val owner = RuntimeOwner(::OfflineRuntime, StandardTestDispatcher(testScheduler)) { scope ->
            admitDebugRuntime(scope) { throw AuthFailure("account_changed") }
        }
        val initial = DebugLodyApplication.fixtureScope
        try {
            for (scope in listOf(initial, initial, initial.copy(workspaceId = "workspace-b"))) {
                owner.activate(scope)
                assertEquals(scope, owner.state.value.scope)
                assertEquals(RuntimePhase.READY, owner.state.value.status.phase)
                assertEquals(RuntimeResult.Pong(true), owner.use(scope) { it.execute(RuntimeCommand.Ping) })
                owner.clear()
            }
        } finally { owner.clear() }
    }

    @Test fun otherAccountsRemainBlockedByAccountAndWorkspaceAdmission() = runTest {
        for (code in listOf("account_changed", "workspace_e2ee_unsupported", "workspace_mode_unconfirmed")) {
            val owner = RuntimeOwner(::OfflineRuntime, StandardTestDispatcher(testScheduler)) { scope ->
                admitDebugRuntime(scope) { throw AuthFailure(code) }
            }
            for (account in listOf("real-account", "offline-fixture-other")) {
                try {
                    owner.activate(RuntimeScope(account, "workspace-a"))
                    fail("Admission must reject $account")
                } catch (error: AuthFailure) { assertEquals(code, error.code) }
                assertNull(owner.state.value.scope)
            }
            owner.clear()
        }
    }

    @Test fun admittedNonFixtureAccountCanRun() = runTest {
        val scope = RuntimeScope("real-account", "plaintext-workspace")
        val owner = RuntimeOwner(::OfflineRuntime, StandardTestDispatcher(testScheduler)) { key ->
            admitDebugRuntime(key) { require(it == scope) }
        }
        try {
            owner.activate(scope)
            assertEquals(RuntimeResult.Pong(true), owner.use(scope) { it.execute(RuntimeCommand.Ping) })
        } finally { owner.clear() }
    }
}
