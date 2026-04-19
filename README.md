# MacroEngine — Fabric Mod

> **Developer-only toolkit.** Not intended for public distribution.

A Fabric server-side mod that provides scaffolding commands for Minecraft datapacks and resource packs, with deep integration for the Advanced Macro Engine (AME) datapack framework.

## Features

- `/macro-engine create dp <name> <namespace> <tick_fn> <load_fn>` — scaffold a complete datapack with AME integration
- `/macro-engine create rp <name> <namespace> <tick_fn> <load_fn>` — scaffold a resource pack (1.21.4+ item model layout)
- `/macro-engine add <module> <pack_name> <function> <purpose>` — inject AME module templates into existing packs
- `/macro-engine run <namespace:function> [with <storage> <path>]` — execute a macro function with storage arguments
- `/macro-engine list [packs|modules]` — list installed datapacks or available module types
- `/macro-engine info` — show mod version
- `/macro-engine help` — full command reference

All commands require **permission level 2** (operator).

## Available Modules

| Module | Description |
|--------|-------------|
| `cooldown` | Player cooldown tracking (macro-based, scoreboard-backed) |
| `flag` | Boolean flag storage in engine NBT |
| `log` | Dev-only message logging to op-tagged players |
| `event` | Custom event dispatch system |
| `hook` | Item/block interaction hooks |
| `multi_cmd` | Queue-based multi-command execution |
| `scheduler` | Tick-countdown task scheduler |
| `perm` | Permission level check (scoreboard-based, no op dependency) |
| `math` | Scoreboard-backed arithmetic macro |
| `string` | Formatted tellraw via macro substitution |
| `nbt` | NBT copy between storages (macro path injection) |
| `player` | Player heal/feed/glow utilities |
| `entity` | Entity summoning macro |
| `geo` | Teleport / position macro |
| `dialog` | 1.21.6+ `/dialog show` integration (JSON + inline) |
| `inv` | Inventory item replacement macro |
| `wand` | Right-click wand detection (carrot_on_a_stick + trigger) |
| `interaction` | Left-click via `interaction` entity sensor (< pack_format 88) |
| `particle` | Particle spawning macro |
| `uuid` | Store entity UUID array into storage |

## Scaffold Layout (Datapack)

```
datapacks/<name>/
  pack.mcmeta                          ← supported_formats {min:61, max:94}
  README.md
  data/
    <namespace>/
      function/
        <tick_fn>.mcfunction           ← guarded tick entry
        <load_fn>.mcfunction           ← scoreboard + storage init
        ame/
          init.mcfunction              ← AME bootstrap
        macro/
          run.mcfunction               ← macro execution template
          dispatch.mcfunction          ← central dispatcher (per-tick)
    minecraft/
      tags/function/
        tick.json
        load.json
```

## Building

Requirements: Java 21, Gradle 8.x

```bash
./gradlew build
# Output: build/libs/macroengine-fabric-1.0.0.jar
```

## Compatibility

| Minecraft | pack_format | Status |
|-----------|-------------|--------|
| 1.21.4    | 61          | ✅ Primary target |
| 1.21.5    | 71          | ✅ Supported |
| 1.21.6    | 80          | ✅ Supported (dialog module) |
| 1.21.10   | 88          | ✅ Supported |
| 1.21.11   | 94          | ✅ Supported |

## Organisation

[ToolkitMC](https://github.com/ToolkitMC) — internal developer tools
