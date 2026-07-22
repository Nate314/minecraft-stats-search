package com.gawith.statssearch.mixin;

import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Lets us collapse a list entry's height. Used to make the Items tab's header row occupy no space
 * once its buttons have been relocated to the pinned strip — the entry stays in the list (so
 * vanilla's {@code sortItems} keeps working) but takes up zero rows.
 *
 * <p>Targeted by string because {@link AbstractSelectionList}{@code .Entry} is a {@code protected}
 * inner type; the {@code height} field's setter descriptor {@code (I)V} resolves regardless.
 */
@Mixin(targets = "net.minecraft.client.gui.components.AbstractSelectionList$Entry")
public interface EntryAccessor {

	@Accessor("height")
	void statssearch$setHeight(int height);
}
