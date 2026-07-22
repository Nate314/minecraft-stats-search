package com.gawith.statssearch.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Marks {@code StatsScreen$ItemStatisticsList} (the only stats list with a column-header row) and
 * exposes its per-column X helper so the relocated header buttons can be lined up with the
 * columns. {@code instanceof ItemStatisticsListAccessor} identifies the Items list without naming
 * the package-private type.
 */
@Mixin(targets = "net.minecraft.client.gui.screens.achievement.StatsScreen$ItemStatisticsList")
public interface ItemStatisticsListAccessor {

	@Invoker("getColumnX")
	int statssearch$getColumnX(int column);
}
