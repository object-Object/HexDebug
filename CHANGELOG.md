# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and [Pydantic's HISTORY.md](https://github.com/pydantic/pydantic/blob/main/HISTORY.md), and this project *mostly* adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [UNRELEASED]

### Changed

- A message is now displayed if attempting to use an Evaluator when not debugging, instead of silently failing.
- Evaluators are now prevented from evaluating patterns after an Uncaught Mishap breakpoint is hit.

### Fixed

- Evaluators were unable to cast any spells requiring media. 
- When debugging, spells requiring media would fail if a Debugger was not in the hand that the debug session was started with.
- Evaluator mishaps were unintentionally caught by the Uncaught Mishaps option, and did not apply side effects to the stack.

## 0.2.0+1.20.1

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

## 0.1.1+1.20.1

### Fixed

- Fixed a server-only crash on launch caused by an incorrect Mod Menu dependency. 

## 0.1.0+1.20.1

- Initial version.
