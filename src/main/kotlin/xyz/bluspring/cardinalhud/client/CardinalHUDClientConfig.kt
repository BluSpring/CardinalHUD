package xyz.bluspring.cardinalhud.client

import net.minecraft.resources.Identifier
import xyz.bluspring.cardinalhud.api.CompassBehavior

object CardinalHUDClientConfig {
    var replaceLocatorBar = true
    var behaviors: Map<Identifier, CompassBehavior> = mapOf()
    var isHidden = false
    var showDistance = true
    var maxDisplayDistance = 100_000
}
