package com.gawith.statssearch.mixin;

import java.util.List;

import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Lets {@link StatsScreenMixin} read and rebuild a vanilla list's contents.
 *
 * <p>We deliberately avoid an {@code addEntry} invoker: its parameter {@code E} is bounded by the
 * {@code protected} {@code AbstractSelectionList.Entry}, so the method's real descriptor is
 * {@code (Entry)I} — which we can't express from this package (an {@code Object} param produces
 * {@code (Object)I} and never matches, even with {@code @Coerce}).
 *
 * <p>Instead we reach the private {@code children} field directly. Its accessor descriptor is just
 * {@code ()Ljava/util/List;} (generics erased), so it always resolves, and the field is the real
 * mutable backing list — not the unmodifiable view returned by {@code children()}.
 */
@Mixin(AbstractSelectionList.class)
public interface AbstractSelectionListAccessor {

	@Invoker("clearEntries")
	void statssearch$clearEntries();

	@Accessor("children")
	List<Object> statssearch$getChildren();
}
