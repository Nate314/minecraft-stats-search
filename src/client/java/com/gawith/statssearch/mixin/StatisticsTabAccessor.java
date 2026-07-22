package com.gawith.statssearch.mixin;

import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the {@code list} field of {@code StatsScreen$StatisticsTab}.
 *
 * <p>That inner class is package-private, so it can't be named directly from our package —
 * we target it by string and reach the field via an {@code @Accessor}. Applying this mixin
 * also makes every {@code StatisticsTab} implement this interface, so an {@code instanceof}
 * check cleanly distinguishes the stat tabs from the loading tab.
 */
@Mixin(targets = "net.minecraft.client.gui.screens.achievement.StatsScreen$StatisticsTab")
public interface StatisticsTabAccessor {

	@Accessor("list")
	AbstractSelectionList<?> statssearch$getList();
}
