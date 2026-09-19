package com.gawith.statssearch;

import net.fabricmc.api.ClientModInitializer;

/**
 * Client entrypoint. All real work happens in the mixins (see
 * {@code com.gawith.statssearch.mixin}); this just confirms the mod loaded.
 */
public class StatsSearchClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		StatsSearch.LOGGER.info("Stats Search loaded: the statistics screen now has a search box.");
	}
}
