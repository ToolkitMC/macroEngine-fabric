# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.0] — 2025-04-19

### Added
- Initial release of MacroEngine Fabric mod
- `/macro-engine create dp <name> <namespace> <tick_fn> <load_fn>` — scaffold datapacks with AME integration
- `/macro-engine create rp <name> <namespace> ...` — scaffold resource packs (1.21.4+ item model layout)
- `/macro-engine add <module> <pack_name> <function> <purpose>` — inject AME module templates
- `/macro-engine run <fn_id> [with <storage> <path>]` — execute macro functions with storage arguments
- `/macro-engine list [packs|modules]` — list installed datapacks and available modules
- `/macro-engine info` — mod version info
- `/macro-engine help` — command reference
- 19 built-in module templates: `cooldown`, `flag`, `log`, `event`, `hook`, `multi_cmd`, `scheduler`, `perm`, `math`, `string`, `nbt`, `player`, `entity`, `geo`, `dialog`, `inv`, `wand`, `interaction`, `particle`, `uuid`
- Datapack scaffold includes: `ame/init.mcfunction`, `macro/run.mcfunction`, `macro/dispatch.mcfunction`
- `supported_formats` in `pack.mcmeta` uses correct object syntax `{"min_inclusive":61,"max_inclusive":94}`
- Dialog module generates both a `.mcfunction` caller and a 1.21.6+ `dialog/` JSON file
- GitHub Actions: build, release, stale
- Community standards: SECURITY.md, CONTRIBUTING.md, CODE_OF_CONDUCT.md, issue templates, PR template
