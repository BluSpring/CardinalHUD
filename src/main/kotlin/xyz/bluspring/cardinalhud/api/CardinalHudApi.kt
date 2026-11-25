package xyz.bluspring.cardinalhud.api

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.GlobalPos
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import org.apache.commons.lang3.function.TriFunction
import xyz.bluspring.cardinalhud.client.CardinalHUDClient
import xyz.bluspring.cardinalhud.client.CardinalWaypointType
import java.util.function.Function
import java.util.function.Predicate

object CardinalHudApi {
    @JvmOverloads
    @JvmStatic
    fun registerWaypointType(name: Identifier, icon: Identifier, defaultBehavior: CompassBehavior = CompassBehavior.INVENTORY, maxWaypoints: Int = Int.MAX_VALUE, stackFilter: Predicate<ItemStack>, positionGetter: TriFunction<ClientLevel, ItemStack, Player, GlobalPos?>, colorGetter: Function<ItemStack, Int> = { -1 }) {
        if (CardinalHUDClient.waypointTypes.contains(name))
            throw IllegalArgumentException("Waypoint type $name already registered!")

        CardinalHUDClient.waypointTypes[name] = CardinalWaypointType(icon, defaultBehavior, maxWaypoints, stackFilter, positionGetter::apply, colorGetter::apply)
    }
}
