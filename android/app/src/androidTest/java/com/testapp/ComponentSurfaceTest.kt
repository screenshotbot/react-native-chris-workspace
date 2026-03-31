package com.rnstorybookautoscreenshots

import androidx.test.platform.app.InstrumentationRegistry
import com.testapp.MainApplication
import junit.framework.TestCase.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ComponentSurfaceTest(private val componentName: String) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun components() = listOf(
            arrayOf("MyFeature"),
            arrayOf("SwitchFeature"),
            arrayOf("TimerFeature"),
            arrayOf("CoinFlipFeature"),
            arrayOf("Button"),
            arrayOf("Header"),
        )
    }

    @Test
    fun constructViewTest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val app = context.applicationContext as MainApplication
        val surface = app.reactHost.createSurface(context, componentName, null)
        surface.start()
        assertNotNull(surface.view)
    }
}
