package xyz.bluspring.cardinalhud.client.waypoint

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder

data class WaypointCollection(var name: String, val waypoints: MutableList<CustomWaypoint>) {
    companion object {
        @JvmField val CODEC: Codec<WaypointCollection> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("name")
                    .forGetter(WaypointCollection::name),
                CustomWaypoint.CODEC.listOf()
                    .fieldOf("waypoints")
                    .forGetter(WaypointCollection::waypoints),
            )
                .apply(instance, ::WaypointCollection)
        }
    }
}
