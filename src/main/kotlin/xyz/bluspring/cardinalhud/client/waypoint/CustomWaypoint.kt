package xyz.bluspring.cardinalhud.client.waypoint

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.UUIDUtil
import net.minecraft.core.Vec3i
import net.minecraft.network.chat.TextColor
import net.minecraft.world.waypoints.TrackedWaypoint
import net.minecraft.world.waypoints.Waypoint
import java.util.UUID

data class CustomWaypoint(val uuid: UUID, var name: String, var pos: Vec3i, var color: TextColor, var shorthand: String = name.firstOrNull()?.toString() ?: "") {
    fun asTracked(): TrackedWaypoint {
        return TrackedWaypoint.setPosition(uuid, Waypoint.Icon.NULL, pos)
    }

    companion object {
        @JvmField val CODEC: Codec<CustomWaypoint> = RecordCodecBuilder.create { instance ->
            instance.group(
                UUIDUtil.CODEC.fieldOf("uuid")
                    .forGetter(CustomWaypoint::uuid),
                Codec.STRING.fieldOf("name")
                    .forGetter(CustomWaypoint::name),
                Vec3i.CODEC.fieldOf("pos")
                    .forGetter(CustomWaypoint::pos),
                TextColor.CODEC.fieldOf("color")
                    .forGetter(CustomWaypoint::color),
                Codec.STRING.fieldOf("shorthand")
                    .forGetter(CustomWaypoint::shorthand)
            )
                .apply(instance, ::CustomWaypoint)
        }
    }
}