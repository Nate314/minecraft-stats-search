package com.gawith.statssearch.mixin;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gawith.statssearch.EntrySearchText;

import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.tabs.MenuTabBar;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.network.chat.Component;

/**
 * Adds a search box to the Statistics screen that filters every stats tab
 * (General / Items / Mobs) to entries whose name contains the query.
 *
 * <p>In 26.x the screen is built around {@code TabManager} / {@link MenuTabBar}; each tab is a
 * {@code StatsScreen$StatisticsTab} holding an {@link AbstractSelectionList}. We enumerate the
 * tabs via {@link MenuTabBar#getTabs()} and rebuild each list's entries on every keystroke, so
 * switching tabs needs no extra hook — every list is already filtered.
 *
 * <p>Only public, stable Mojang-mapped types are referenced here; the private list/entry/tab
 * classes are reached through {@link StatisticsTabAccessor},
 * {@link AbstractSelectionListAccessor} and {@link EntrySearchText}.
 */
@Mixin(StatsScreen.class)
public abstract class StatsScreenMixin extends Screen {

	@Unique private static final int SEARCH_WIDTH = 180;
	@Unique private static final int SEARCH_HEIGHT = 18;
	@Unique private static final int SEARCH_PADDING = 4;

	// Pinned Items-tab header strip. Tune these if the buttons sit slightly off-column.
	@Unique private static final int HEADER_STRIP_HEIGHT = 22;
	@Unique private static final int HEADER_BTN_Y_OFFSET = 3;
	// HeaderEntry.extractContent positions each button at contentX + getColumnX(i) - 18,
	// where contentX = rowLeft + Entry.CONTENT_PADDING (4). That combines to rowLeft - 14.
	@Unique private static final int HEADER_BTN_X_OFFSET = -14;

	@Shadow private MenuTabBar tabNavigationBar;
	@Shadow @org.spongepowered.asm.mixin.Final private HeaderAndFooterLayout layout;
	@Shadow @org.spongepowered.asm.mixin.Final private TabManager tabManager;

	@Unique private EditBox statssearch$searchBox;

	/** Full, unfiltered entry list per stats list, captured the first time we filter it. */
	@Unique private final Map<AbstractSelectionList<?>, List<Object>> statssearch$snapshots = new IdentityHashMap<>();

	/** The Items list, its column-X helper, and its sort buttons once we've pinned the header. */
	@Unique private AbstractSelectionList<?> statssearch$itemsList;
	@Unique private ItemStatisticsListAccessor statssearch$itemsColumns;
	@Unique private List<AbstractWidget> statssearch$headerButtons;

	private StatsScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void statssearch$addSearchBox(CallbackInfo ci) {
		// The lists are rebuilt on each init, so any captured snapshots are stale.
		this.statssearch$snapshots.clear();
		EntrySearchText.clearCache();

		String previous = this.statssearch$searchBox != null ? this.statssearch$searchBox.getValue() : "";
		Component hint = Component.translatable("statssearch.search.hint");

		// Provisional position; statssearch$layoutSearchBox() places it precisely once the tab bar
		// has been arranged (in repositionElements, which always runs right after init).
		this.statssearch$searchBox = new EditBox(
				this.font,
				this.width / 2 - SEARCH_WIDTH / 2,
				SEARCH_PADDING,
				SEARCH_WIDTH,
				SEARCH_HEIGHT,
				hint);
		this.statssearch$searchBox.setHint(hint);
		this.statssearch$searchBox.setMaxLength(100);
		this.statssearch$searchBox.setValue(previous);
		this.statssearch$searchBox.setResponder(this::statssearch$applyFilter);
		this.addRenderableWidget(this.statssearch$searchBox);

		this.statssearch$applyFilter(previous);
	}

	/**
	 * Runs after vanilla has arranged the tab bar and lists (on open and on every resize). Vanilla
	 * sizes every list to the full content area; we re-size them with explicit
	 * {@code updateSizeAndPosition} calls so the top of each list sits below the search strip (and,
	 * for the Items tab, below the pinned-header strip too). Doing it directly avoids fighting the
	 * tab's {@code GridLayoutTab} alignment, which otherwise lets a full-height list spill back up
	 * over our widgets.
	 */
	@Inject(method = "repositionElements", at = @At("TAIL"))
	private void statssearch$layoutSearchBox(CallbackInfo ci) {
		if (this.statssearch$searchBox == null || this.tabNavigationBar == null) {
			return;
		}

		int contentTop = this.tabNavigationBar.getRectangle().bottom();
		int listTop = contentTop + SEARCH_HEIGHT + SEARCH_PADDING * 2;

		this.statssearch$searchBox.setX(this.width / 2 - SEARCH_WIDTH / 2);
		this.statssearch$searchBox.setY(contentTop + SEARCH_PADDING);

		this.statssearch$resizeLists();

		// Line the pinned header buttons up with the Items list columns.
		if (this.statssearch$headerButtons != null && this.statssearch$itemsList != null) {
			int rowLeft = this.statssearch$itemsList.getRowLeft();
			for (int i = 0; i < this.statssearch$headerButtons.size(); i++) {
				AbstractWidget button = this.statssearch$headerButtons.get(i);
				int columnX = this.statssearch$itemsColumns.statssearch$getColumnX(i);
				button.setX(rowLeft + HEADER_BTN_X_OFFSET + columnX);
				button.setY(listTop + HEADER_BTN_Y_OFFSET);
			}
		}
	}

	/**
	 * Runs every frame just before the lists render. Switching tabs makes vanilla re-size the active
	 * list back to the full content area, so we re-assert our reserved strips here (guarded, so it's
	 * a no-op once a list is already the right size). Also toggles the pinned header buttons so they
	 * only show on the Items tab.
	 */
	@Inject(method = "extractRenderState", at = @At("HEAD"))
	private void statssearch$beforeRender(CallbackInfo ci) {
		if (this.statssearch$headerButtons == null) {
			return;
		}
		this.statssearch$resizeLists();

		Tab current = this.tabManager.getCurrentTab();
		boolean onItemsTab = current instanceof StatisticsTabAccessor st
				&& st.statssearch$getList() == this.statssearch$itemsList;
		for (AbstractWidget button : this.statssearch$headerButtons) {
			button.visible = onItemsTab;
			button.active = onItemsTab;
		}
	}

	/**
	 * Size each stats list so its top sits below the search strip (and, for the Items list, below
	 * the pinned-header strip too). Only touches a list whose top is currently wrong, so re-running
	 * it every frame is cheap and doesn't disturb scrolling.
	 */
	@Unique
	private void statssearch$resizeLists() {
		if (this.tabNavigationBar == null) {
			return;
		}
		int contentTop = this.tabNavigationBar.getRectangle().bottom();
		int contentBottom = this.height - this.layout.getFooterHeight();
		int listTop = contentTop + SEARCH_HEIGHT + SEARCH_PADDING * 2;

		for (Tab tab : this.tabNavigationBar.getTabs()) {
			if (!(tab instanceof StatisticsTabAccessor st)) {
				continue;
			}
			AbstractSelectionList<?> list = st.statssearch$getList();
			boolean isItems = list == this.statssearch$itemsList && this.statssearch$itemsList != null;
			int top = isItems ? listTop + HEADER_STRIP_HEIGHT : listTop;
			if (list.getY() != top) {
				list.updateSizeAndPosition(this.width, Math.max(0, contentBottom - top), top);
			}
		}
	}

	/** Vanilla builds the real lists here once stats arrive; pin the Items header and re-filter. */
	@Inject(method = "onStatsUpdated", at = @At("TAIL"))
	private void statssearch$refilterOnUpdate(CallbackInfo ci) {
		this.statssearch$snapshots.clear();
		EntrySearchText.clearCache();
		this.statssearch$pinItemsHeader();
		if (this.statssearch$searchBox != null) {
			this.statssearch$applyFilter(this.statssearch$searchBox.getValue());
		}
	}

	/**
	 * Host the Items tab's column-header sort buttons on the screen as a fixed strip, and make the
	 * in-list header row vanish without removing it. We keep the header entry (index 0) because
	 * vanilla's {@code sortItems} does {@code clearEntriesExcept(children[0])} and would otherwise
	 * duplicate the first item row on every sort. Collapsing its height to 0 and cancelling its
	 * in-list render (see {@code HeaderEntryMixin}) makes it occupy no space and draw nothing, so
	 * the buttons only ever appear in the pinned strip. Runs once.
	 */
	@Unique
	private void statssearch$pinItemsHeader() {
		if (this.statssearch$headerButtons != null || this.tabNavigationBar == null) {
			return;
		}
		for (Tab tab : this.tabNavigationBar.getTabs()) {
			if (!(tab instanceof StatisticsTabAccessor statsTab)) {
				continue;
			}
			AbstractSelectionList<?> list = statsTab.statssearch$getList();
			if (!(list instanceof ItemStatisticsListAccessor columns)) {
				continue;
			}

			List<Object> children = ((AbstractSelectionListAccessor) list).statssearch$getChildren();
			if (children.isEmpty() || !(children.get(0) instanceof HeaderEntryAccessor header)) {
				return;
			}

			this.statssearch$itemsList = list;
			this.statssearch$itemsColumns = columns;
			this.statssearch$headerButtons = List.copyOf(header.statssearch$getButtons());

			// Collapse the in-list header to nothing, then host its buttons on the screen.
			((EntryAccessor) header).statssearch$setHeight(0);
			for (AbstractWidget button : this.statssearch$headerButtons) {
				this.addRenderableWidget(button);
			}

			// Now that the buttons exist, run a layout pass so they're positioned on first open
			// (otherwise they'd only snap into place after the first resize).
			this.repositionElements();
			return;
		}
	}

	@Unique
	private void statssearch$applyFilter(String query) {
		if (this.tabNavigationBar == null) {
			return;
		}
		for (Tab tab : this.tabNavigationBar.getTabs()) {
			if (tab instanceof StatisticsTabAccessor statsTab) {
				this.statssearch$filterList(statsTab.statssearch$getList(), query);
			}
		}
	}

	@Unique
	@SuppressWarnings("unchecked")
	private void statssearch$filterList(AbstractSelectionList<?> list, String query) {
		if (list == null) {
			return;
		}

		AbstractSelectionListAccessor accessor = (AbstractSelectionListAccessor) list;

		// Capture the full list the first time we touch it (before any clearing).
		List<Object> all = this.statssearch$snapshots.computeIfAbsent(
				list, l -> new ArrayList<>(((AbstractSelectionListAccessor) l).statssearch$getChildren()));

		String needle = query.trim().toLowerCase(Locale.ROOT);

		List<Object> matched;
		if (needle.isEmpty()) {
			matched = all;
		} else {
			matched = new ArrayList<>();
			for (Object entry : all) {
				String text = EntrySearchText.of(entry);
				// Keep structural rows that have no name (e.g. the Items tab's column-header row)
				// so a search never strips the headers; only filter out data rows that don't match.
				if (text.isEmpty() || text.contains(needle)) {
					matched.add(entry);
				}
			}
		}

		// clearEntries() empties the backing list and resets the selection; then repopulate it
		// with the matching entries (same instances, so their layout state is preserved).
		accessor.statssearch$clearEntries();
		accessor.statssearch$getChildren().addAll(matched);
		list.setScrollAmount(0.0);
	}
}
