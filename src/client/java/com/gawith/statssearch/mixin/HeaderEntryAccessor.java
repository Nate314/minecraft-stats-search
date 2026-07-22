package com.gawith.statssearch.mixin;

import java.util.List;

import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the column sort buttons held by the Items tab's header row
 * ({@code StatsScreen$ItemStatisticsList$HeaderEntry}). They are public {@link AbstractWidget}s,
 * so we can lift them out of the scrolling list and add them straight to the screen as a pinned
 * header — keeping their original sort behaviour intact.
 */
@Mixin(targets = "net.minecraft.client.gui.screens.achievement.StatsScreen$ItemStatisticsList$HeaderEntry")
public interface HeaderEntryAccessor {

	@Accessor("children")
	List<AbstractWidget> statssearch$getButtons();
}
