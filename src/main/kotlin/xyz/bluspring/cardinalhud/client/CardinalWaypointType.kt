package xyz.bluspring.cardinalhud.client

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.GlobalPos
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import xyz.bluspring.cardinalhud.api.CompassBehavior
import java.util.function.Predicate

@JvmRecord
data class CardinalWaypointType(
    val icon: Identifier, val defaultBehavior: CompassBehavior,
    val maxWaypoints: Int,
    val stackFilter: Predicate<ItemStack>,
    val positionGetter: (ClientLevel, ItemStack, Player) -> GlobalPos?,
    val colorGetter: (ItemStack) -> Int,
)
