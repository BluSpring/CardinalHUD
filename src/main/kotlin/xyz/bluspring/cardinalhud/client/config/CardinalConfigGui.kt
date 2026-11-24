package xyz.bluspring.cardinalhud.client.config

import dev.isxander.yacl3.api.Binding
import dev.isxander.yacl3.dsl.*
import net.minecraft.client.gui.screens.Screen
import xyz.bluspring.cardinalhud.CardinalHUD
import xyz.bluspring.cardinalhud.client.CardinalHUDClient
import xyz.bluspring.cardinalhud.client.CompassBehavior

private fun <T> bindingFromSunset(name: String): Binding<T> {
    val value = CardinalHUDClient.config.rootCategory.getValueById<T>(name)!!
    return Binding.generic(value.default, value::value) {
        value.value = it
    }
}

fun generateConfigGui(lastScreen: Screen?): Screen = YetAnotherConfigLib(CardinalHUD.MOD_ID) {
    save {
        CardinalHUDClient.config.save()
    }

    val main by categories.registering {
        val preferences by groups.registering {
            val replaceLocatorBar by options.registering {
                binding = bindingFromSunset("replaceLocatorBar")
                controller = tickBox()
            }

            val isHidden by options.registering {
                binding = bindingFromSunset("isHidden")
                controller = tickBox()
            }
        }

        val behaviors by groups.registering {
            val spawnCompass by options.registering {
                binding = bindingFromSunset("spawnCompassBehavior")
                controller = enumSwitch(CompassBehavior::class.java)
            }

            val recoveryCompass by options.registering {
                binding = bindingFromSunset("recoveryCompassBehavior")
                controller = enumSwitch(CompassBehavior::class.java)
            }

            val lodestoneCompass by options.registering {
                binding = bindingFromSunset("lodestoneCompassBehavior")
                controller = enumSwitch(CompassBehavior::class.java)
            }
        }
    }
}.generateScreen(lastScreen)