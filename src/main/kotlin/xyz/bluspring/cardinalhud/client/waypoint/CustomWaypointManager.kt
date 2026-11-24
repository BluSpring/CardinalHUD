package xyz.bluspring.cardinalhud.client.waypoint

import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.multiplayer.ClientPacketListener
import net.minecraft.client.server.IntegratedServer
import net.minecraft.util.FileUtil
import xyz.bluspring.cardinalhud.CardinalHUD
import java.net.InetSocketAddress
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.UUID
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.reader
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

class CustomWaypointManager(val collections: MutableMap<UUID, WaypointCollection> = mutableMapOf()) {
    private var activeCollectionId: UUID? = null

    @OptIn(ExperimentalUuidApi::class)
    val activeCollection: WaypointCollection
        get() {
            if (activeCollectionId == null) {
                if (collections.isEmpty()) {
                    activeCollectionId = Uuid.generateV7().toJavaUuid()
                    collections[activeCollectionId!!] = WaypointCollection("Unnamed", mutableListOf())
                } else {
                    activeCollectionId = collections.keys.first()
                }
            }

            return collections[activeCollectionId]!!
        }

    companion object {
        private val MAIN_PATH: Path = FabricLoader.getInstance().gameDir.resolve("cardinalhud")

        fun resolvePathForLevel(level: ClientLevel, handler: ClientPacketListener): Path? {
            return if (handler.connection.isMemoryConnection) {
                val server = level.server
                if (server is IntegratedServer) {
                    MAIN_PATH.resolve("singleplayer/${FileUtil.sanitizeName(server.worldData.levelName)}")
                } else null
            } else {
                val address = handler.connection.remoteAddress
                if (address is InetSocketAddress) {
                    MAIN_PATH.resolve("multiplayer/${FileUtil.sanitizeName(address.address.hostAddress)}_${address.port}")
                } else null
            }?.resolve("${level.dimension().identifier().namespace}/${level.dimension().identifier().path}")
        }

        fun forLevel(level: ClientLevel, handler: ClientPacketListener): CustomWaypointManager {
            val path = resolvePathForLevel(level, handler) ?: return CustomWaypointManager()

            if (!path.exists())
                return CustomWaypointManager()

            val collections = mutableMapOf<UUID, WaypointCollection>()
            val collectionPaths = path.resolve("waypoints")
            if (collectionPaths.exists()) {
                for (collectionPath in collectionPaths) {
                    if (collectionPath.extension != "json")
                        continue

                    try {
                        val uuid = UUID.fromString(collectionPath.nameWithoutExtension)
                        collections[uuid] = WaypointCollection.CODEC.decode(JsonOps.INSTANCE, JsonParser.parseReader(collectionPath.reader(options = arrayOf(StandardOpenOption.READ))))
                            .orThrow.first
                    } catch (e: Throwable) {
                        CardinalHUD.logger.error("Could not load waypoints from ${collectionPath.name}!", e)
                    }
                }
            }

            return CustomWaypointManager(collections)
        }
    }
}
