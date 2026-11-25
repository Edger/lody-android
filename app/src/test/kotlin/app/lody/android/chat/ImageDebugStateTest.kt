package app.lody.android.chat

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageDebugStateTest {
 @Test fun theDebugImageIdSelectsThePreviewState() {
  assertEquals(DebugImageState.Loading, debugImageState("debug-loading-1"))
  assertEquals(DebugImageState.Failed, debugImageState("debug-failed-1"))
  assertEquals(DebugImageState.Loaded, debugImageState("debug-loaded-1"))
  assertEquals(DebugImageState.Loaded, debugImageState("a-session-image"))
 }
}
