package xyz.bluspring.cardinalhud.client

import net.minecraft.resources.Identifier
import xyz.bluspring.cardinalhud.api.CompassBehavior
import java.util.*

object CardinalHUDClientConfig {
    var replaceLocatorBar = true
    var behaviors: MutableMap<Identifier, CompassBehavior> = Collections.synchronizedMap(mutableMapOf())
    var isHidden = false
}
