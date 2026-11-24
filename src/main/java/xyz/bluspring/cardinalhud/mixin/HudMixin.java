package xyz.bluspring.cardinalhud.mixin;

import java.nio.file.Path;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.bluspring.cardinalhud.client.CardinalHUDClientConfig;
import xyz.bluspring.cardinalhud.client.waypoint.CustomWaypointManager;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;

@Mixin(Hud.class)
public abstract class HudMixin {
    @WrapMethod(method = "extractBossOverlay")
    private void moveBossOverlayDown(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, Operation<Void> original) {
        if (!CardinalHUDClientConfig.INSTANCE.isHidden()) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(0f, 100f);
        }

        original.call(graphics, deltaTracker);

        if (!CardinalHUDClientConfig.INSTANCE.isHidden()) {
            graphics.pose().popMatrix();
        }
    }

    @ModifyExpressionValue(method = "nextContextualInfoState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/waypoints/ClientWaypointManager;hasWaypoints()Z"))
    private boolean disableLocatorBars(boolean original) {
        if (CardinalHUDClientConfig.INSTANCE.getReplaceLocatorBar())
            return false;

        return original;
    }
}
