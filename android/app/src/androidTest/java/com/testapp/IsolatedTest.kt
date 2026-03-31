package com.rnstorybookautoscreenshots

import androidx.test.platform.app.InstrumentationRegistry
import com.facebook.react.ReactRootView
import junit.framework.TestCase.assertTrue
import org.junit.Test

class IsolatedTest {
    @Test
    fun simpleTest() {
        assertTrue(true);
    }

    @Test
    fun constructViewTest() {
        val view = ReactRootView(InstrumentationRegistry.getInstrumentation().targetContext)
        assertTrue(view != null)
    }
}