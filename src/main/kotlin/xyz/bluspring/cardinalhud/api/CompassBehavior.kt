package xyz.bluspring.cardinalhud.api

import com.mojang.serialization.Codec
import net.minecraft.util.StringRepresentable
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

enum class CompassBehavior(private val serialized: String) : StringRepresentable {
    /**
     * Disabled, is never displayed.
     */
    DISABLED("disabled"),

    /**
     * Only displays on the HUD when held in hand.
     */
    HELD("held"),

    /**
     * Only displays on the HUD when anywhere in the hotbar.
     */
    HOTBAR("hotbar"),

    /**
     * Displays on the HUD as long as the compass is in the inventory.
     */
    INVENTORY("inventory"),
    ;

    companion object {
        @JvmField val CODEC: Codec<CompassBehavior> = StringRepresentable.fromEnum(CompassBehavior::values)
    }

    fun findStacksForPlayer(player: Player, predicate: (ItemStack) -> Boolean): Collection<ItemStack> {
        if (this == DISABLED)
            return emptyList()

        val stacks = mutableSetOf<ItemStack>()

        if (this >= INVENTORY) {
            stacks.addAll(player.inventory.filter(predicate))
        } else {
            if (this >= HELD) {
                for (hand in InteractionHand.entries) {
                    val stack = player.getItemInHand(hand)
                    if (predicate(stack))
                        stacks.add(stack)
                }
            }

            if (this >= HOTBAR) {
                for (i in 0 until Inventory.SELECTION_SIZE) {
                    val stack = player.inventory.getItem(i)
                    if (predicate(stack))
                        stacks.add(stack)
                }
            }
        }

        return stacks
    }

    override fun getSerializedName(): String = this.serialized
}