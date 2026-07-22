package com.gawith.statssearch;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * Derives a lower-cased, searchable string from a statistics-list entry.
 *
 * <p>The vanilla list entries (e.g. {@code StatsScreen$GeneralStatisticsList$Entry}) are
 * package-private inner classes with no public name accessor, so we read their fields
 * reflectively rather than coupling to private types. We look for the field types that
 * actually carry a human-readable name — {@link Component}, {@link String},
 * {@link EntityType}, {@link Item}/{@link Block}/{@link ItemStack} — and concatenate their
 * display text. This is deliberately tolerant: if Mojang renames or reshapes the entry in a
 * future version, the worst case is an entry that simply doesn't match the query.
 *
 * <p>Results are cached per entry instance, since entry objects are long-lived and we
 * re-query them on every keystroke.
 */
public final class EntrySearchText {

	private static final Map<Object, String> CACHE = new HashMap<>();

	private EntrySearchText() {
	}

	public static String of(Object entry) {
		if (entry == null) {
			return "";
		}
		String cached = CACHE.get(entry);
		if (cached != null) {
			return cached;
		}

		StringBuilder sb = new StringBuilder();
		for (Class<?> c = entry.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
			for (Field f : c.getDeclaredFields()) {
				appendFieldValue(sb, entry, f);
			}
		}

		String result = sb.toString().trim().toLowerCase(Locale.ROOT);
		CACHE.put(entry, result);
		return result;
	}

	private static void appendFieldValue(StringBuilder sb, Object owner, Field f) {
		Class<?> type = f.getType();
		boolean interesting = type == String.class
				|| Component.class.isAssignableFrom(type)
				|| EntityType.class.isAssignableFrom(type)
				|| Item.class.isAssignableFrom(type)
				|| Block.class.isAssignableFrom(type)
				|| ItemStack.class.isAssignableFrom(type);
		if (!interesting) {
			return;
		}

		try {
			f.setAccessible(true);
			Object value = f.get(owner);
			append(sb, value);
		} catch (ReflectiveOperationException | RuntimeException ignored) {
			// Inaccessible field (module restrictions) or odd state — skip it.
		}
	}

	private static void append(StringBuilder sb, Object value) {
		switch (value) {
			case null -> {
			}
			case String s -> sb.append(s).append(' ');
			case Component c -> sb.append(c.getString()).append(' ');
			case EntityType<?> et -> sb.append(et.getDescription().getString()).append(' ');
			case ItemStack stack -> sb.append(stack.getHoverName().getString()).append(' ');
			case Item item -> sb.append(new ItemStack(item).getHoverName().getString()).append(' ');
			case Block block -> sb.append(block.getName().getString()).append(' ');
			default -> {
			}
		}
	}

	/** Clears the per-entry cache. Call when the stats lists are rebuilt. */
	public static void clearCache() {
		CACHE.clear();
	}
}
