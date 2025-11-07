# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and [Pydantic's HISTORY.md](https://github.com/pydantic/pydantic/blob/main/HISTORY.md), and this project *mostly* adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## `0.8.0+1.20.1` - 2025-11-07

### Added

- Added support for debugging spell circles! Right-click (or sneak-right-click, for toolsmiths) on an impetus with a Debugger to start a debug session.
- New items: Quenched Debugger and Quenched Evaluator! These post-enlightenment debug tools allow up to 4 hexes to be debugged at once. Scroll while holding sneak and sprint in order to switch between the available "debug thread" slots.
- API: Added API documentation to the web book! The API docs from the latest commit pushed to `main` are available [here](https://hexdebug.hexxy.media/v/latest/main/api/), and there is a link to the API docs for each version in the header of the corresponding web book.
- API: Added support for implementing debug support in casting devices/environments other than the Debugger item (eg. circles, wisps, cassettes). See the API documentation for more details, especially the sequence diagrams in the `hexdebug-core-common` module.
- API: Added a new mini-mod called HexDebug Core (`hexdebug-core-*`). This is a minimal, Java-only mod containing only the API classes required to implement debug support. The intention is for HexDebug Core to be included in other addons via Jar-in-Jar, allowing addons to implement optional debugging support in their casting environments while minimizing the amount of overhead and added complexity from checking whether HexDebug is loaded or not.
- API: Added Mojmap-remapped `common` jars (`hexdebug-common-mojmap` and `hexdebug-core-common-mojmap`) for use in VanillaGradle-based xplat projects.

### Changed

- Update zh_cn translations, by ChuijkYahus in [#60](https://github.com/object-Object/HexDebug/pull/60) and [#63](https://github.com/object-Object/HexDebug/pull/63).
- Source files can now be viewed after the hex that created them finishes debugging.
- Removed the random prefix from source filenames.
- Active debug sessions are now terminated on death.
- API: The `SplicingTableIotaRenderer` tooltip is now stored in a field on the renderer, allowing it to be updated in `render` if HexDebug isn't updating it frequently enough for your use case.

### Fixed

- Fixed a crash when closing the Splicing Table with Hexical 1.5.0 installed ([#62](https://github.com/object-Object/HexDebug/issues/62)).
- Fixed exceptions not being caught by exception breakpoints after the first hex in a debug session.

## `0.7.0+1.20.1` - 2025-10-02

### Added

- Added a tag (`hexdebug:focus_holder/blacklist`) to prevent specific items from being inserted into the Focal Frame.
- Added new methods to the `SplicingTableIotaRenderers` class to allow iota renderers to contain or reference other renderers.
- New Splicing Table iota renderer types:
  - `hexdebug:conditional/if_path_exists`
  - `hexdebug:layers`
  - `hexdebug:sub_iota`
- New built-in iota icons:
  - `hexdebug:builtin/wide/list`
  - `hexdebug:builtin/wide/type`

### Changed

- ⚠️ Breaking: The `hexdebug:list` renderer type now has a required field `renderer` to render the iota; the list renderer now only provides the tooltip.
- ⚠️ Breaking: Changed all Splicing Table iota renderer JSON fields from camelCase to snake_case.
- List iotas now render their first iota (if any) in the Splicing Table ([#48](https://github.com/object-Object/HexDebug/issues/48)).
- MoreIotas' item type iotas now render brackets around the item to distinguish them from item stack iotas.
- Added (official) support for inserting iota-holding items other than foci into the Focal Frame ([#52](https://github.com/object-Object/HexDebug/issues/52)).
- A single Focal Frame item can now be used like a bundle to insert/remove items in your inventory ([#49](https://github.com/object-Object/HexDebug/issues/49)).
- The Focal Frame now gives comparator output and can be broken by pushing it with a piston.
- Splicing Table iota renderers are now cached to improve performance.
- Projectionist's Purification, Projectionist's Gambit, and Shutter's Purification (but not the II variants) now work on Focal Frames in addition to Splicing Tables.

### Fixed

- Fixed a few bugs allowing arbitrary items to be inserted into the Splicing Table and Focal Frame using hoppers or modded item transportation ([#38](https://github.com/object-Object/HexDebug/issues/38), [#53](https://github.com/object-Object/HexDebug/issues/53)).
- Fixed the EMI integration failing to load on Forge.
- Fixed EMI not using the space where the cast button would be in a regular Splicing Table.
- Fixed Splicing Table iota renderer resources not being loaded when the game launches on Forge ([#58](https://github.com/object-Object/HexDebug/issues/58)).

### Removed

- Removed most recipes related to filling/emptying Focal Frames, other than the recipe to fill empty frames with new foci using stackable ingredients.

## `0.6.0+1.20.1` - 2025-09-29

### Added

- Added a new resource-pack-based data-driven system for addon devs to customize how their iotas are rendered in the Splicing Table. ([Docs](https://github.com/object-Object/HexDebug/wiki/Splicing-Table-Iota-Rendering))
- Added Hexical interop to make the telepathy and notebook keys work in the Splicing/Mindsplice Table GUI.
- Added support for "rainbow brackets" (also known as bracket pair colorization) to the Splicing Table. Introspection and Retrospection are now tinted rainbow colors based on their depth. The list of colors is configurable in HexDebug's client settings, and there's also an option to disable the feature entirely.
- Added a config option to disable showing pattern names in embedded lists.

### Changed

- ⚠️ Rearranged the client config. In particular, the `invertSplicingTableScrollDirection` option has been moved to `splicingTable.invertScrollDirection`, so you'll have to set this option again after updating if you've changed it from the default.
- Updated zh_cn translations, by ChuijkYahus in [#43](https://github.com/object-Object/HexDebug/pull/43).
- The Mindsplice Table casting button tooltip now includes the block's name (eg. from renaming it in an anvil).
- Item Type and Item Stack iotas from MoreIotas now render as the item they represent in the Splicing Table.

### Fixed

- Fixed a bug where the Splicing Table's contents would be dropped on the ground when upgrading it to a Mindsplice Table ([#47](https://github.com/object-Object/HexDebug/issues/47)).

## `0.5.0+1.20.1` - 2025-09-22

### Added

- Added keyboard shortcuts for almost all of the buttons in the Splicing Table, configurable in HexDebug's client config menu.
- Added several new keyboard-only actions to the Splicing Table, mostly related to manipulating the active selection:
  - Move Cursor Left/Right
  - Expand Selection Left/Right
  - Move Selection Left/Right
  - Backspace
- Added a tag (`hexdebug:splicing_table/media_blacklist`) to prevent specific items from being used to refill the Splicing Table's media buffer (eg. [Oneironaut](https://oneironaut.hexxy.media)'s Inexhaustible Phial).  

### Changed

- The Splicing Table now works with Media Purification from [Hexpose](https://miyucomics.github.io/hexpose) (requires at least build [2e90cf5b](https://github.com/miyucomics/hexpose/commit/2e90cf5babb7677a26ca211bd66ac2777e5518cd), or any version after 1.0.0).

### Fixed

- Fixed an issue where newly placed splicing tables would default to having the first iota selected.
- Fixed several bugs/exploits related to the Splicing Table's media buffer ([#40](https://github.com/object-Object/HexDebug/issues/40), [#41](https://github.com/object-Object/HexDebug/issues/41), [#44](https://github.com/object-Object/HexDebug/issues/44), [#45](https://github.com/object-Object/HexDebug/issues/45)).
- Fixed Lodestone Reflection not working in the Mindsplice Table ([#42](https://github.com/object-Object/HexDebug/issues/42)).
- Fixed a bug in the Splicing Table where the Select All button was enabled when the list was empty and the left edge was selected. 

## `0.4.0+1.20.1` - 2025-09-21

### Added

- New blocks:
  - Mindsplice Table: An upgraded variant of the Splicing Table, adding a GUI button to cast a hex imbued into the table (suggested by cc-aaron in [#34](https://github.com/object-Object/HexDebug/issues/34)).
  - Focal Frame: A cheap and lag-friendly way to store and manipulate iotas in the world. (This used to be an undocumented block called the Focus Holder, but now it's craftable and has a better name.)
- Added support for manipulating the main item in the Splicing Table via [IoticBlocks](https://ioticblocks.hexxy.media).
- Added several new patterns for manipulating various aspects of the Splicing Table.
- Added several new config options related to the Splicing Table.

### Changed

- Updated zh_cn translations, by ChuijkYahus in [#36](https://github.com/object-Object/HexDebug/pull/36) and [#39](https://github.com/object-Object/HexDebug/pull/39).
- The Splicing Table now stores the current view index and selected iotas server-side, so that they don't reset when closing and reopening the GUI.
- Implemented scroll wheel handling in the Splicing Table GUI to make scrolling through the pattern view easier (suggested by abilliontrillionstars in [#35](https://github.com/object-Object/HexDebug/issues/35)).
- The Splicing Table now moves the view position when performing actions or drawing patterns, to make sure the position being modified is visible.

### Fixed

- Added [EMI](https://modrinth.com/mod/emi) interop to fix an issue where EMI's item index would render on top of the Splicing Table GUI.
- Fixed an issue where the casting grid ambience sound would sometimes play for a fraction of a second when opening the Splicing Table.
- Fixed Splicing Table buttons that require media still being clickable when the table is out of media.
- Added missing localizations for a few config options.

## `0.3.0+1.20.1` - 2025-04-22

### Added

- New block: Splicing Table!
  - Essentially a graphical text editor for hexes. Allows inserting, moving, and deleting iotas from lists; copying/pasting iotas to/from a secondary focus; drawing patterns with a staff at a specific position in a list; and copying hexes to your clipboard in `.hexpattern` format. 
  - Amazing textures created by SamsTheNerd.
- New patterns:
  - Debug Locator's Reflection: Pushes the index of the next iota to be evaluated.
  - Cognitohazard Reflection: Pushes a cognitohazard iota to the stack, which halts debugging immediately if detected in a hex to be evaluated by a debugger.
- Added ru_ru translations, by JustS-js in [#25](https://github.com/object-Object/HexDebug/pull/25).
- New dependency: IoticBlocks.

### Changed

- Updated to Hex Casting 0.11.2.
- Updated zh_cn translations, by ChuijkYahus in [#13](https://github.com/object-Object/HexDebug/pull/13), [#17](https://github.com/object-Object/HexDebug/pull/17), and [#31](https://github.com/object-Object/HexDebug/pull/31).
- Added a link to [Setting up VSCode with HexDebug](https://github.com/object-Object/HexDebug/wiki/Setting-up-VSCode-with-HexDebug) to the debugger book entry.
- Documented the ability to change debugger step modes ingame.
- Many internal refactors.

### Fixed

- Fixed a bug where stepping the debug session would overwrite the displayed stack/ravenmind of an open non-Evaluator staff grid ([#6](https://github.com/object-Object/HexDebug/issues/6)).
  - This was only a visual bug - no actual data was modified, and it would reset if you reopened the staff.
- Server config is now synced to clients on join.
- Fixed some potential networking-related race conditions.
- Fixed an internal exception when FrameEvaluate contains an empty list.
- Fixed a bug where Introspection, Retrospection, Consideration, and Evanition would be displayed incorrectly if drawn in non-default orientations.

### Notes

- This update also contains a new undocumented block (Focus Holder). Please don't use this for anything important, as it'll probably be moved to a separate mod at some point.
- I likely will not be backporting this update to 1.19.2, due to the many GUI-related changes between that version and this one. 

## `0.2.2+1.20.1` - 2024-05-28

### Changed

- Updated zh_cn translations, by ChuijkYahus in [#5](https://github.com/object-Object/HexDebug/pull/5).

## `0.2.1+1.20.1` - 2024-05-27

### Changed

- The Debugger can now be used without VSCode being connected! The next evaluated iota is displayed above the hotbar each time the debugger stops (configurable).
- A message is now displayed if attempting to use an Evaluator when not debugging, instead of silently failing.
- Evaluators are now prevented from evaluating patterns after an Uncaught Mishap breakpoint is hit.

### Fixed

- Evaluators were unable to cast any spells requiring media. 
- When debugging, spells requiring media would fail if a Debugger was not in the hand that the debug session was started with.
- Evaluator mishaps were unintentionally caught by the Uncaught Mishaps option, and did not apply side effects to the stack.

## `0.2.0+1.20.1` - 2024-05-21

### Added

- New item: Evaluator!
  - A staff that allows evaluating expressions in an ongoing debug session. The stack and ravenmind update to reflect the state of the Debugger whenever it changes.
  - When the Evaluator's grid is cleared, the Debugger is reset to the state it was in just before the first pattern was drawn in the Evaluator.
- New config options:
  - Open Debug Port: Whether or not a port should be opened. If false, the Debugger effectively becomes useless.
  - Smart Debugger Sneak-Scroll: If a hex is not currently being debugged and a Debugger is in your main hand, prefer shift-scrolling whatever item is in your offhand (eg. a spellbook).
    - This was already a feature; it's just configurable now.

### Changed

- Moved some relatively unimportant log messages from INFO to DEBUG.

## `0.1.1+1.20.1` - 2024-05-18

### Fixed

- Fixed a server-only crash on launch caused by an incorrect Mod Menu dependency. 

## `0.1.0+1.20.1` - 2024-05-17

- Initial version.
