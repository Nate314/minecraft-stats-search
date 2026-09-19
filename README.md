# Stats Search

A client-side [Fabric](https://fabricmc.net/) mod that adds a **search box to the in-game
Statistics screen** (`Esc` → Statistics) so you can filter the General / Items / Mobs lists by
name instead of scrolling. It also **pins the Items tab's column header** so the sort-button
labels stay visible while you scroll.

- **Minecraft:** 26.3 (Fabric)
- **Fabric Loader:** 0.19.5 · **Fabric API:** 0.161.0+26.3 · **Loom:** 1.17
- **Java:** 25 (required: the Minecraft 26.x toolchain does not build on older JDKs)

> Minecraft 26.x is the first **unobfuscated** release line, so this project uses Mojang's
> official mappings via the new non-remapping `net.fabricmc.fabric-loom` plugin, so there is no
> Yarn and no `mappings` dependency.

## Install (players)

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 26.3.
2. Put [Fabric API](https://modrinth.com/mod/fabric-api) (`0.161.0+26.3` or compatible) in your
   `mods` folder.
3. Put `statssearch-1.0.0+26.3.jar` (from `./gradlew build`, or a release) in the same `mods` folder.

Then open a world → `Esc` → **Statistics** and type in the search box.

## Build (developers)

Requires **JDK 25**. The Gradle wrapper is included, so:

```sh
./gradlew build         # produces build/libs/statssearch-1.0.0+26.3.jar
./gradlew runClient     # launches a dev client with the mod loaded
./gradlew genSources    # (optional) decompile the MC sources for reference
```

Import the project into IntelliJ IDEA **2025.3+** for mixin support.

## How it works

In 26.x the Statistics screen is built around `TabManager` / `MenuTabBar`; each tab
(`StatsScreen$StatisticsTab`) holds an `AbstractSelectionList`.

**Search:** on every keystroke the mod enumerates the tabs and rebuilds each list from a saved
snapshot, keeping only entries whose name contains the query. All three lists are filtered
together, so switching tabs needs no extra hook.

**Pinned Items header:** vanilla builds the column header (the sort buttons that label each
number column) as the *first scrolling list entry*, so it slides off-screen. The mod relocates
those buttons onto the screen as a fixed strip, and collapses the in-list header row to zero
height with its rendering cancelled, rather than removing it, because
`ItemStatisticsList.sortItems` keeps `children[0]` and would otherwise duplicate the first row on
every sort. Each list is then explicitly re-sized to sit below the reserved strips; that sizing is
re-asserted every frame so a tab switch (which makes vanilla re-lay-out the list) can't undo it.

Everything is done through mixins/accessors so only **public, stable** types are referenced; the
package-private inner list/entry/tab classes are reached via accessors:

| File | Role |
| --- | --- |
| [`StatsScreenMixin`](src/client/java/com/gawith/statssearch/mixin/StatsScreenMixin.java) | Adds the `EditBox`, filters the lists, pins the Items header, and reserves the layout strips. |
| [`StatisticsTabAccessor`](src/client/java/com/gawith/statssearch/mixin/StatisticsTabAccessor.java) | `@Accessor` for the package-private `StatisticsTab.list` field. |
| [`ItemStatisticsListAccessor`](src/client/java/com/gawith/statssearch/mixin/ItemStatisticsListAccessor.java) | Marks the Items list and exposes `getColumnX` for aligning the pinned buttons. |
| [`HeaderEntryAccessor`](src/client/java/com/gawith/statssearch/mixin/HeaderEntryAccessor.java) | `@Accessor` for the header row's sort buttons (`children`). |
| [`HeaderEntryMixin`](src/client/java/com/gawith/statssearch/mixin/HeaderEntryMixin.java) | Cancels the header row's in-list rendering (buttons are drawn on the screen instead). |
| [`EntryAccessor`](src/client/java/com/gawith/statssearch/mixin/EntryAccessor.java) | `@Accessor` setter for an entry's `height`, used to collapse the header row. |
| [`AbstractSelectionListAccessor`](src/client/java/com/gawith/statssearch/mixin/AbstractSelectionListAccessor.java) | `@Invoker clearEntries` + `@Accessor` for the backing `children` list. |
| [`EntrySearchText`](src/client/java/com/gawith/statssearch/EntrySearchText.java) | Reflectively derives a searchable name from a (package-private) list entry, catching `GeneralStatisticsList$Entry.statDisplay` and `MobsStatisticsList$MobRow.mobName`. |

## Tuning

Layout constants live at the top of
[`StatsScreenMixin`](src/client/java/com/gawith/statssearch/mixin/StatsScreenMixin.java). If the
pinned Items-tab buttons sit a few pixels off their columns on your resolution, nudge
`HEADER_BTN_X_OFFSET` / `HEADER_BTN_Y_OFFSET` / `HEADER_STRIP_HEIGHT`; `SEARCH_WIDTH` /
`SEARCH_PADDING` control the search box.

> Searching the **Mobs** tab also matches kill-count text, since `MobRow` stores those numbers as
> components. To match names only, narrow `EntrySearchText` to the first `Component` field.

## License

[MIT](LICENSE).
