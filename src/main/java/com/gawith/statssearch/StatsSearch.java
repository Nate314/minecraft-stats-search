package com.gawith.statssearch;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common (server-safe) entrypoint. The actual feature is client-only, so this does
 * almost nothing — it exists only so the mod loads on a dedicated server without error.
 */
public class StatsSearch implements ModInitializer {
	public static final String MOD_ID = "statssearch";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// No common-side behaviour.
	}
}
