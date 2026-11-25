package xyz.bluspring.cardinalhud.client

import com.mojang.blaze3d.platform.InputConstants
import com.mojang.serialization.Codec
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Camera
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.item.properties.numeric.CompassAngleState
import net.minecraft.core.Direction
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.util.ARGB
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import net.minecraft.world.waypoints.PartialTickSupplier
import net.minecraft.world.waypoints.TrackedWaypoint
import xyz.bluspring.cardinalhud.CardinalHUD
import xyz.bluspring.cardinalhud.api.CardinalHudApi
import xyz.bluspring.cardinalhud.api.CompassBehavior
import xyz.bluspring.sunset.SunsetConfig
import kotlin.math.round

class CardinalHUDClient : ClientModInitializer {
    companion object {
        internal val waypointTypes = mutableMapOf<Identifier, CardinalWaypointType>()

        const val WIDTH = 400
        const val HEIGHT = 100

        val config = SunsetConfig.create(FabricLoader.getInstance().configDir.resolve("cardinalhud_client.json")) {
            boolean("replaceLocatorBar", CardinalHUDClientConfig::replaceLocatorBar)
            boolean("hidden", CardinalHUDClientConfig::isHidden)
            boolean("showDistance", CardinalHUDClientConfig::showDistance)
            integer("maxDisplayDistance", CardinalHUDClientConfig::maxDisplayDistance)

            value("behaviors", Codec.unboundedMap(Identifier.CODEC, CompassBehavior.CODEC), CardinalHUDClientConfig::behaviors)
        }

        private val LOCATOR_BAR_ARROW_UP = Identifier.withDefaultNamespace("hud/locator_bar_arrow_up")
        private val LOCATOR_BAR_ARROW_DOWN = Identifier.withDefaultNamespace("hud/locator_bar_arrow_down")

        private val LODESTONE_ICON = CardinalHUD.id("hud/lodestone")
        private val RESPAWN_ICON = CardinalHUD.id("hud/respawn")
        private val SPAWN_ICON = CardinalHUD.id("hud/spawn")

        val CATEGORY = KeyMapping.Category(CardinalHUD.id("main"))
        val TOGGLE_KEY = KeyMappingHelper.registerKeyMapping(KeyMapping("key.cardinalhud.toggle", InputConstants.KEY_GRAVE, CATEGORY))
    }

    override fun onInitializeClient() {
        config.load()
        config.save()

        CardinalHudApi.registerWaypointType(CardinalHUD.id("compass/spawn"), SPAWN_ICON, CompassBehavior.HELD, 1, { instance -> instance.`is`(Items.COMPASS) && !instance.has(DataComponents.LODESTONE_TRACKER) },
            CompassAngleState.CompassTarget.SPAWN::get)

        CardinalHudApi.registerWaypointType(CardinalHUD.id("compass/recovery"), RESPAWN_ICON, CompassBehavior.HOTBAR, 1, { instance -> instance.`is`(Items.RECOVERY_COMPASS) },
            CompassAngleState.CompassTarget.RECOVERY::get)

        CardinalHudApi.registerWaypointType(CardinalHUD.id("compass/lodestone"), LODESTONE_ICON, CompassBehavior.INVENTORY, 50, { instance -> instance.`is`(Items.COMPASS) && instance.has(DataComponents.LODESTONE_TRACKER) },
            CompassAngleState.CompassTarget.LODESTONE::get, supportsNames = true)

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (TOGGLE_KEY.consumeClick()) {
                CardinalHUDClientConfig.isHidden = !CardinalHUDClientConfig.isHidden
                config.save()
            }
        }

        HudElementRegistry.attachElementBefore(VanillaHudElements.BOSS_BAR, CardinalHUD.id("cardinal_hud")) { graphics, deltaTracker ->
            val client = Minecraft.getInstance()
            val yaw = client.gameRenderer.mainCamera().yaw()
            graphics.pose().pushMatrix()

            val centerX = graphics.guiWidth() / 2
            val startX = centerX - (WIDTH / 2)
            val endX = centerX + (WIDTH / 2)
            graphics.enableScissor(startX, 0, endX, HEIGHT)

            val font = client.font

            // N/E/S/W compass
            for (direction in Direction.Plane.HORIZONTAL) {
                val relativeX = Mth.wrapDegrees(direction.toYRot() - yaw)
                val x = centerX + round(relativeX * 3f)

                graphics.fill(x.toInt() - 1, 5, x.toInt(), 15, -1)
                graphics.centeredText(font, "${direction.name.first()}", x.toInt(), 20, -1)
            }

            // dotted lines
            for (i in 0 until (360 / 15)) {
                val degrees = i * 15

                if (degrees % 90 == 0)
                    continue

                val relativeX = Mth.wrapDegrees(degrees - yaw)
                val x = centerX + round(relativeX * 3f)

                if (degrees % 45 == 0) {
                    // diagonal cardinals
                    graphics.fill(x.toInt() - 1, 6, x.toInt(), 14, -1)

                    val dir1 = Direction.fromYRot(degrees - 45.0)
                    val dir2 = Direction.fromYRot(degrees + 45.0)
                    val formatting = if (dir1.axis == Direction.Axis.Z)
                        "${dir1.name.first()}${dir2.name.first()}"
                    else
                        "${dir2.name.first()}${dir1.name.first()}"

                    graphics.centeredText(font, formatting, x.toInt(), 18, -1)
                } else {
                    graphics.fill(x.toInt() - 1, 8, x.toInt(), 12, -1)
                }
            }

            val level = client.level
            val player = client.player

            // locator bar
            if (CardinalHUDClientConfig.replaceLocatorBar && level != null && player != null) {
                val entity = client.cameraEntity ?: player
                val ptSupplier: PartialTickSupplier = { deltaTracker.getGameTimeDeltaPartialTick(!level.tickRateManager().isEntityFrozen(entity)) }
                player.connection.waypointManager.forEachWaypoint(entity) { waypoint ->
                   displayWaypointToHud(graphics, waypoint, level, client, entity, ptSupplier, centerX)
                }
            }

            if (player != null && level != null) {
                // custom waypoint types
                for ((typeId, type) in waypointTypes) {
                    val behavior = CardinalHUDClientConfig.behaviors[typeId] ?: type.defaultBehavior
                    val stacks = behavior.findStacksForPlayer(player, type.stackFilter::test)

                    for ((index, stack) in stacks.withIndex()) {
                        if (index >= type.maxWaypoints)
                            break

                        val pos = type.positionGetter(level, stack, player)
                        if (pos != null && pos.dimension == level.dimension()) {
                            val name = if (type.supportsNames && stack.has(DataComponents.CUSTOM_NAME))
                                stack.customName ?: Component.empty()
                            else
                                Component.empty()

                            displayIconToHud(graphics, type.colorGetter(stack), type.icon, centerX, Vec3.atCenterOf(pos.pos), client.gameRenderer.mainCamera(), client, name)
                        }
                    }
                }
            }

            graphics.disableScissor()

            graphics.pose().popMatrix()
        }
    }

    private fun displayWaypointToHud(graphics: GuiGraphicsExtractor, waypoint: TrackedWaypoint, level: Level, client: Minecraft, entity: Entity, ptSupplier: PartialTickSupplier, centerX: Int) {
        val yaw = waypoint.yawAngleToCamera(level, client.gameRenderer.mainCamera(), ptSupplier)
        val icon = waypoint.icon()
        val style = client.gui.hud.waypointStyles.get(icon.style)
        val distance = Mth.sqrt(waypoint.distanceSquared(entity).toFloat())
        val sprite = style.sprite(distance)
        val color = icon.color.orElseGet { waypoint.id().map(
            { ARGB.setBrightness(ARGB.color(255, it.hashCode()), 0.9f) },
            { ARGB.setBrightness(ARGB.color(255, it.hashCode()), 0.9f) }) }
        val pitchDirection = waypoint.pitchDirectionToCamera(level, client.gameRenderer, ptSupplier)

        displayIconToHud(graphics, color, sprite, yaw, pitchDirection, centerX)
    }

    private fun displayIconToHud(graphics: GuiGraphicsExtractor, color: Int, sprite: Identifier, centerX: Int, pos: Vec3, camera: Camera, client: Minecraft, text: Component = Component.empty()) {
        if (CardinalHUDClientConfig.maxDisplayDistance != 0 && camera.position().distanceTo(pos) > CardinalHUDClientConfig.maxDisplayDistance)
            return

        val direction = camera.position().subtract(pos).rotateClockwise90()
        val waypointAngle = Mth.atan2(direction.z(), direction.x()).toFloat() * (180f / Mth.PI)
        val yaw = Mth.degreesDifference(camera.yaw(), waypointAngle).toDouble()
        val pitchDirection = run {
            val pointOnScreen: Vec3 = client.gameRenderer.projectPointToScreen(pos)
            val isBehindCamera = pointOnScreen.z > 1.0
            val yInFrontOfCamera = if (isBehindCamera) -pointOnScreen.y else pointOnScreen.y
            if (yInFrontOfCamera < -1.0) {
                return@run TrackedWaypoint.PitchDirection.DOWN
            } else if (yInFrontOfCamera > 1.0) {
                return@run TrackedWaypoint.PitchDirection.UP
            } else {
                if (isBehindCamera) {
                    if (pointOnScreen.y > 0.0) {
                        return@run TrackedWaypoint.PitchDirection.UP
                    }

                    if (pointOnScreen.y < 0.0) {
                        return@run TrackedWaypoint.PitchDirection.DOWN
                    }
                }

                return@run TrackedWaypoint.PitchDirection.NONE
            }
        }

        val distance = if (CardinalHUDClientConfig.showDistance)
            camera.position().distanceTo(pos).toInt()
        else
            -1

        displayIconToHud(graphics, color, sprite, yaw, pitchDirection, centerX, text, distance)
    }

    private fun displayIconToHud(graphics: GuiGraphicsExtractor, color: Int, sprite: Identifier, yaw: Double, pitchDirection: TrackedWaypoint.PitchDirection, centerX: Int, text: Component = Component.empty(), distance: Int = -1) {
        val x = centerX + round(yaw * 3f)
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x.toInt(), 5, 9, 9, color)
        graphics.pose().pushMatrix()
        graphics.pose().scale(0.5f, 0.5f)
        graphics.centeredText(Minecraft.getInstance().font, text, (x.toInt() + 4) * 2, 15 * 2, -1)
        if (distance > 0) {
            graphics.centeredText(Minecraft.getInstance().font,
                if (distance > 100_000)
                    Component.translatable("cardinalhud.format.kilometers", "%.1f".format(distance / 1_000.0))
                else
                    Component.translatable("cardinalhud.format.meters", distance),
                (x.toInt() + 4) * 2, 21 * 2, -1)
        }

        graphics.pose().popMatrix()

        if (pitchDirection != TrackedWaypoint.PitchDirection.NONE) {
            val arrowTop = if (pitchDirection == TrackedWaypoint.PitchDirection.DOWN) 6 else -6
            val arrowSprite = if (pitchDirection == TrackedWaypoint.PitchDirection.DOWN) LOCATOR_BAR_ARROW_DOWN else LOCATOR_BAR_ARROW_UP

            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, arrowSprite, x.toInt() + 1, 5 + arrowTop, 7, 5)
        }
    }
}
