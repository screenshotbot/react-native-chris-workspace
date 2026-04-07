package com.rnstorybookautoscreenshots

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ComponentSurfaceTest(private val componentName: String) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun components() = listOf(
            // Base components
            arrayOf("MyFeature"),
            arrayOf("SwitchFeature"),
            arrayOf("TimerFeature"),
            arrayOf("CoinFlipFeature"),
            arrayOf("Button"),
            arrayOf("Header"),

            // Button story variants
            arrayOf("Button_Primary"),
            arrayOf("Button_Secondary"),
            arrayOf("Button_Large"),
            arrayOf("Button_Small"),

            // Header story variants
            arrayOf("Header_LoggedIn"),
            arrayOf("Header_LoggedOut"),

            // MyFeature story variants
            arrayOf("MyFeature_Initial"),
            arrayOf("MyFeature_WithClicks"),
            arrayOf("MyFeature_ManyClicks"),

            // SwitchFeature story variants
            arrayOf("SwitchFeature_Off"),
            arrayOf("SwitchFeature_On"),

            // TimerFeature story variants
            arrayOf("TimerFeature_Initial"),
            arrayOf("TimerFeature_Running"),
            arrayOf("TimerFeature_Paused"),
            arrayOf("TimerFeature_LongDuration"),

            // CoinFlipFeature story variants
            arrayOf("CoinFlipFeature_Default"),
            arrayOf("CoinFlipFeature_Heads"),
            arrayOf("CoinFlipFeature_Tails"),
            arrayOf("CoinFlipFeature_ManyFlips"),
        )
    }

    @Test
    fun screenshot() {
        ReactHostFixture.screenshotComponent(componentName)
    }
}
