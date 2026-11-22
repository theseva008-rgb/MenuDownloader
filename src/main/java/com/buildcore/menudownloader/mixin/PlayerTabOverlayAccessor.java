package com.buildcore.menudownloader.mixin;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Даёт доступ к шапке и подвалу списка игроков (таба), у которых нет публичных геттеров. */
@Mixin(PlayerTabOverlay.class)
public interface PlayerTabOverlayAccessor {

	@Accessor("header")
	Component menudownloader$getHeader();

	@Accessor("footer")
	Component menudownloader$getFooter();
}
