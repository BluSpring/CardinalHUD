package xyz.bluspring.cardinalhud.client.config

import dev.isxander.yacl3.api.Binding
import dev.isxander.yacl3.dsl.*
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import xyz.bluspring.cardinalhud.CardinalHUD
import xyz.bluspring.cardinalhud.api.CompassBehavior
import xyz.bluspring.cardinalhud.client.CardinalHUDClient
import xyz.bluspring.cardinalhud.client.CardinalHUDClientConfig

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
                binding = bindingFromSunset("hidden")
                controller = tickBox()
            }
        }

        val behaviors by groups.registering {
            for ((typeId, type) in CardinalHUDClient.waypointTypes) {
                options.register(typeId.toString()) {
                    binding = Binding.generic(type.defaultBehavior, { CardinalHUDClientConfig.behaviors[typeId] ?: type.defaultBehavior }) {
                        CardinalHUDClientConfig.behaviors[typeId] = it
                    }

                    controller(enumSwitch(CompassBehavior::class.java) { Component.translatable("cardinalhud.compass_behavior.${it.serializedName}") })
                }
            }
        }
    }
}.generateScreen(lastScreen)