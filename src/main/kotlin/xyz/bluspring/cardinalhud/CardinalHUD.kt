package xyz.bluspring.cardinalhud

import net.fabricmc.api.ModInitializer
import net.minecraft.resources.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class CardinalHUD : ModInitializer {

    override fun onInitialize() {
    }

    companion object {
        const val MOD_ID = "cardinalhud"
        val logger: Logger = LoggerFactory.getLogger(CardinalHUD::class.java)

        @JvmStatic
        fun id(name: String): Identifier = Identifier.fromNamespaceAndPath(MOD_ID, name)
    }
}
