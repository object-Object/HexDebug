# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and [Pydantic's HISTORY.md](https://github.com/pydantic/pydantic/blob/main/HISTORY.md), and this project *mostly* adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [UNRELEASED]

### Added

- Added keyboard shortcuts to the Splicing Table, configurable in HexDebug's client config menu.
- The Splicing Table now works with Media Purification from [Hexpose](https://miyucomics.github.io/hexpose) (requires at least build [2e90cf5b](https://github.com/miyucomics/hexpose/commit/2e90cf5babb7677a26ca211bd66ac2777e5518cd), or any version after 1.0.0).

### Fixed

- Fixed an issue where newly placed splicing tables would default to having the first iota selected.

## `0.4.0+1.20.1` - 2025-09-21

### Added

- New blocks:
  - Mindsplice Table: An upgraded variant of the Splicing Table, adding a GUI button to cast a hex imbued into the table (suggested by cc-aaron in [#34](https://github.com/object-Object/HexDebug/pull/34)).
  - Focal Frame: A cheap and lag-friendly way to store and manipulate iotas in the world. (This used to be an undocumented block called the Focus Holder, but now it's craftable and has a better name.)
- Added support for manipulating the main item in the Splicing Table via [IoticBlocks](https://ioticblocks.hexxy.media).
- Added several new patterns for manipulating various aspects of the Splicing Table.
- Added several new config options related to the Splicing Table.

### Changed

- Updated zh_cn translations, by ChuijkYahus in [#36](https://github.com/object-Object/HexDebug/pull/36) and [#39](https://github.com/object-Object/HexDebug/pull/39).
- The Splicing Table now stores the current view index and selected iotas server-side, so that they don't reset when closing and reopening the GUI.
- Implemented scroll wheel handling in the Splicing Table GUI to make scrolling through the pattern view easier (suggested by abilliontrillionstars in [#35](https://github.com/object-Object/HexDebug/pull/35)).
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
