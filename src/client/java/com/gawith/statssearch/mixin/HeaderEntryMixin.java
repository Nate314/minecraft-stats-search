package com.gawith.statssearch.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cancels the Items header row's in-list rendering. Its sort buttons are drawn by the screen as a
 * pinned strip (see {@code StatsScreenMixin#pinItemsHeader}); without this, the header would also
 * draw them again at its (collapsed, scrolling) position in the list.
 */
@Mixin(targets = "net.minecraft.client.gui.screens.achievement.StatsScreen$ItemStatisticsList$HeaderEntry")
public abstract class HeaderEntryMixin {

	@Inject(method = "extractContent", at = @At("HEAD"), cancellable = true)
	private void statssearch$skipInListRender(CallbackInfo ci) {
		ci.cancel();
	}
}
