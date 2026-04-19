package mc.toolkitmc.macroengine.module;

import mc.toolkitmc.macroengine.util.FileUtil;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Registry of all known macroEngine (AME) modules.
 *
 * Each module knows how to:
 *  1. Generate a template .mcfunction file.
 *  2. Optionally add helper sub-functions.
 *
 * Module IDs are the canonical names used in /macro-engine add.
 */
public final class ModuleRegistry {

    private static final Map<String, ModuleTemplate> REGISTRY = new LinkedHashMap<>();

    static {
        // ── Core ──────────────────────────────────────────────────────────────
        register("cooldown",   ModuleRegistry::templateCooldown);
        register("flag",       ModuleRegistry::templateFlag);
        register("log",        ModuleRegistry::templateLog);

        // ── Event / Hook ──────────────────────────────────────────────────────
        register("event",      ModuleRegistry::templateEvent);
        register("hook",       ModuleRegistry::templateHook);

        // ── Command / Control ─────────────────────────────────────────────────
        register("multi_cmd",  ModuleRegistry::templateMultiCmd);
        register("scheduler",  ModuleRegistry::templateScheduler);
        register("perm",       ModuleRegistry::templatePerm);

        // ── Math / String / Data ──────────────────────────────────────────────
        register("math",       ModuleRegistry::templateMath);
        register("string",     ModuleRegistry::templateString);
        register("nbt",        ModuleRegistry::templateNbt);

        // ── Player / Entity ───────────────────────────────────────────────────
        register("player",     ModuleRegistry::templatePlayer);
        register("entity",     ModuleRegistry::templateEntity);

        // ── World / Geometry ──────────────────────────────────────────────────
        register("geo",        ModuleRegistry::templateGeo);

        // ── UI / Interaction ──────────────────────────────────────────────────
        register("dialog",     ModuleRegistry::templateDialog);
        register("inv",        ModuleRegistry::templateInv);
        register("wand",       ModuleRegistry::templateWand);
        register("interaction",ModuleRegistry::templateInteraction);

        // ── Particles / Visual ────────────────────────────────────────────────
        register("particle",   ModuleRegistry::templateParticle);

        // ── UUID ──────────────────────────────────────────────────────────────
        register("uuid",       ModuleRegistry::templateUuid);
    }

    private static void register(String id, ModuleTemplate template) {
        REGISTRY.put(id, template);
    }

    public static boolean exists(String id) {
        return REGISTRY.containsKey(id);
    }

    public static List<String> moduleNames() {
        return new ArrayList<>(REGISTRY.keySet());
    }

    /**
     * Generate module files inside the given pack.
     *
     * @param packRoot  root path of the datapack (contains pack.mcmeta)
     * @param namespace primary namespace detected from the pack
     * @param module    module ID
     * @param function  function name to create
     * @param purpose   description / command string for cmd-type modules
     */
    public static void addModule(Path packRoot, String namespace,
                                 String module, String function, String purpose) throws IOException {
        ModuleTemplate template = REGISTRY.get(module);
        if (template == null) throw new IllegalArgumentException("Unknown module: " + module);

        Path fnDir = packRoot.resolve("data").resolve(namespace).resolve("function");
        FileUtil.mkdirs(fnDir);

        template.generate(fnDir, namespace, function, purpose);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Module templates
    // ─────────────────────────────────────────────────────────────────────────

    // ── cooldown ──────────────────────────────────────────────────────────────

    private static void templateCooldown(Path fnDir, String ns, String fn, String purpose) throws IOException {
        Path dir = fnDir.resolve("cooldown");
        FileUtil.mkdirs(dir);

        FileUtil.write(dir.resolve(fn + ".mcfunction"), """
                # %s:cooldown/%s — %s
                # Macro-based cooldown check.
                # Call:  function %s:cooldown/%s with storage %s:input Args
                # Args:  {target: "@s", objective: "%s_cooldown", ticks: 20}

                $execute as $(target) run scoreboard players get @s $(objective)
                $execute as $(target) if score @s $(objective) matches 1.. run return 0
                $execute as $(target) run scoreboard players set @s $(objective) $(ticks)
                return 1
                """.formatted(ns, fn, purpose, ns, fn, ns, ns));
    }

    // ── flag ─────────────────────────────────────────────────────────────────

    private static void templateFlag(Path fnDir, String ns, String fn, String purpose) throws IOException {
        Path dir = fnDir.resolve("flag");
        FileUtil.mkdirs(dir);

        FileUtil.write(dir.resolve(fn + ".mcfunction"), """
                # %s:flag/%s — %s
                # Boolean flag stored in engine storage.
                # Call:  function %s:flag/%s with storage %s:input Args
                # Args:  {flag: "my_flag", value: 1b}

                $data modify storage %s:engine Flags.$(flag) set value $(value)
                """.formatted(ns, fn, purpose, ns, fn, ns, ns));

        FileUtil.write(dir.resolve(fn + "_check.mcfunction"), """
                # %s:flag/%s_check — Check a flag value
                # Call:  function %s:flag/%s_check with storage %s:input Args
                # Args:  {flag: "my_flag"}

                $execute if data storage %s:engine Flags{$(flag): 1b} run return 1
                return 0
                """.formatted(ns, fn, ns, fn, ns, ns));
    }

    // ── log ──────────────────────────────────────────────────────────────────

    private static void templateLog(Path fnDir, String ns, String fn, String purpose) throws IOException {
        Path dir = fnDir.resolve("log");
        FileUtil.mkdirs(dir);

        FileUtil.write(dir.resolve(fn + ".mcfunction"), """
                # %s:log/%s — %s
                # Log a message to all ops.
                # Call:  function %s:log/%s with storage %s:input Args
                # Args:  {level: "INFO", msg: "your message here"}

                $tellraw @a[tag=%s.dev] {"text":"[$(level)] $(msg)","color":"gray"}
                """.formatted(ns, fn, purpose, ns, fn, ns, ns));
    }

    // ── event ─────────────────────────────────────────────────────────────────

    private static void templateEvent(Path fnDir, String ns, String fn, String purpose) throws IOException {
        Path dir = fnDir.resolve("event");
        FileUtil.mkdirs(dir);

        FileUtil.write(dir.resolve(fn + ".mcfunction"), """
                # %s:event/%s — %s
                # Custom event handler.
                # Register this in your tick function or hook.
                # Args:  {event_id: "my_event", data: {}}

                $data modify storage %s:engine LastEvent set value {id:"$(event_id)"}
                function %s:event/%s_handle
                """.formatted(ns, fn, purpose, ns, ns, fn));

        FileUtil.write(dir.resolve(fn + "_handle.mcfunction"), """
                # %s:event/%s_handle — Dispatch to event-specific handlers
                # Add execute-if-data branches for each event type.

                execute if data storage %s:engine LastEvent{id:"my_event"} run say [Event] my_event fired
                """.formatted(ns, fn, ns));
    }

    // ── hook ─────────────────────────────────────────────────────────────────

    private static void templateHook(Path fnDir, String ns, String fn, String purpose) throws IOException {
        Path dir = fnDir.resolve("hook");
        FileUtil.mkdirs(dir);

        FileUtil.write(dir.resolve(fn + ".mcfunction"), """
                # %s:hook/%s — %s
                # Item/block interaction hook.
                # Link to advancement reward or #minecraft:tick tag.
                #
                # Example: detect right-click with carrot_on_a_stick
                execute as @a[nbt={SelectedItem:{id:"minecraft:carrot_on_a_stick"}}] \
                    at @s \
                    if entity @s[scores={%s_trigger=1..}] \
                    run function %s:hook/%s_fire

                scoreboard players set @a %s_trigger 0
                """.formatted(ns, fn, purpose, ns, ns, fn, ns));

        FileUtil.write(dir.resolve(fn + "_fire.mcfunction"), """
                # %s:hook/%s_fire — Executed when the hook condition triggers
                # Add your hook logic here.
                say [Hook] %s triggered by @s
                """.formatted(ns, fn, fn));
    }

    // ── multi_cmd ────────────────────────────────────────────────────────────

    private static void templateMultiCmd(Path fnDir, String ns, String fn, String purpose) throws IOException {
        Path dir = fnDir.resolve("multi_cmd");
        FileUtil.mkdirs(dir);

        FileUtil.write(dir.resolve(fn + ".mcfunction"), """
                # %s:multi_cmd/%s — %s
                # Execute a list of commands stored in %s:engine Queue.
                # Push commands: data modify storage %s:engine Queue append value {cmd:"say hi"}
                # Then call this function to drain the queue.

                execute if data storage %s:engine Queue[0] run function %s:multi_cmd/%s_next
                """.formatted(ns, fn, purpose, ns, ns, ns, ns, fn));

        FileUtil.write(dir.resolve(fn + "_next.mcfunction"), """
                # %s:multi_cmd/%s_next — Pop and execute one queue entry
                # Advanced macro: receives Queue[0].cmd as $(cmd)
                $$(cmd)
                data remove storage %s:engine Queue[0]
                execute if data storage %s:engine Queue[0] run function %s:multi_cmd/%s_next
                """.formatted(ns, fn, ns, ns, ns, fn));
    }

    // ── scheduler ────────────────────────────────────────────────────────────

    private static void templateScheduler(Path fnDir, String ns, String fn, String purpose) throws IOException {
        Path dir = fnDir.resolve("scheduler");
        FileUtil.mkdirs(dir);

        FileUtil.write(dir.resolve(fn + ".mcfunction"), """
                # %s:scheduler/%s — %s
                # Tick-based task scheduler.
                # Usage: set %s_timer score, then this function counts down and fires.
                # Call once per tick from your main tick function.

                scoreboard players remove .scheduler_active %s_cooldown 1
                execute if score .scheduler_active %s_cooldown matches ..0 run function %s:scheduler/%s_fire
                """.formatted(ns, fn, purpose, ns, ns, ns, ns, fn));

        FileUtil.write(dir.resolve(fn + "_fire.mcfunction"), """
                # %s:scheduler/%s_fire — Runs when scheduler countdown reaches 0
                scoreboard players set .scheduler_active %s_cooldown 0
                say [Scheduler] %s fired!
                # Add your scheduled logic here
                """.formatted(ns, fn, ns, fn));

        FileUtil.write(dir.resolve(fn + "_set.mcfunction"), """
                # %s:scheduler/%s_set — Set a delay before firing
                # Call:  function %s:scheduler/%s_set with storage %s:input Args
                # Args:  {ticks: 100}
                $scoreboard players set .scheduler_active %s_cooldown $(ticks)
                """.formatted(ns, fn, ns, fn, ns, ns));
    }

    // ── perm ─────────────────────────────────────────────────────────────────

    private static void templatePerm(Path fnDir, String ns, String fn, String purpose) throws IOException {
        Path dir = fnDir.resolve("perm");
        FileUtil.mkdirs(dir);

        FileUtil.write(dir.resolve(fn + ".mcfunction"), """
                # %s:perm/%s — %s
                # Permission level check using scoreboard.
                # Objective: %s_permission_level
                # Levels: 0=none, 1=player, 2=vip, 3=mod, 4=owner

                execute as @s if score @s %s_permission_level matches 4.. run return 1
                execute as @s if score @s %s_permission_level matches 3..3 run return 1
                # return 0 for insufficient perms
                return 0
                """.formatted(ns, fn, purpose, ns, ns, ns));

        // scoreboard add helper
        FileUtil.write(dir.resolve(fn + "_set.mcfunction"), """
                # %s:perm/%s_set — Set a player's permission level (macro)
                # Call:  function %s:perm/%s_set with storage %s:input Args
                # Args:  {target: "@s", level: 2}
                $scoreboard players set $(target) %s_permission_level $(level)
                """.formatted(ns, fn, ns, fn, ns, ns));
    }

    // ── math ─────────────────────────────────────────────────────────────────

    private static void templateMath(Path fnDir, String ns, String fn, String purpose) throws IOException {
        Path dir = fnDir.resolve("math");
        FileUtil.mkdirs(dir);

        FileUtil.write(dir.resolve(fn + ".mcfunction"), """
                # %s:math/%s — %s
                # Macro-driven arithmetic operation.
                # Call:  function %s:math/%s with storage %s:input Args
                # Args:  {a: 10, b: 3, op: "add"}  (ops: add sub mul div mod)
                # Result written to %s:output Result

                $execute if data storage %s:input {op:"add"} run data modify storage %s:output Result set value 0
                # Use scoreboard as intermediary for actual math:
                $scoreboard players set .math_a %s_cooldown $(a)
                $scoreboard players set .math_b %s_cooldown $(b)
                scoreboard players operation .math_a %s_cooldown += .math_b %s_cooldown
                execute store result storage %s:output Result int 1 run scoreboard players get .math_a %s_cooldown
                """.formatted(ns, fn, purpose, ns, fn, ns, ns, ns, ns, ns, ns, ns, ns, ns, ns));
    }

    // ── string ───────────────────────────────────────────────────────────────

    private static void templateString(Path fnDir, String ns, String fn, String purpose) throws IOException {
        Path dir = fnDir.resolve("string");
        FileUtil.mkdirs(dir);

        FileUtil.write(dir.resolve(fn + ".mcfunction"), """
                # %s:string/%s — %s
                # String processing via macro substitution.
                # Call:  function %s:string/%s with storage %s:input Args
                # Args:  {prefix: "[TAG]", body: "hello world", target: "@a"}
                #
                # Sends a formatted message.
                $tellraw $(target) [{"text":"$(prefix) ","color":"gold"},{"text":"$(body)","color":"white"}]
                """.formatted(ns, fn, purpose, ns, fn, ns));
    }

    // ── nbt ──────────────────────────────────────────────────────────────────

    private static void templateNbt(Path fnDir, String ns, String fn, String purpose) throws IOException {
        Path dir = fnDir.resolve("nbt");
        FileUtil.mkdirs(dir);

        FileUtil.write(dir.resolve(fn + ".mcfunction"), """
                # %s:nbt/%s — %s
                # Copy NBT data between storages using macro path injection.
                # Call:  function %s:nbt/%s with storage %s:input Args
                # Args:  {src_storage: "%s:input", src_path: "Src.Key",
                #         dst_storage: "%s:output", dst_path: "Dst.Key"}
                $data modify storage $(dst_storage) $(dst_path) set from storage $(src_storage) $(src_path)
                """.formatted(ns, fn, purpose, ns, fn, ns, ns, ns));
    }

    // ── player ────────────────────────────────────────────────────────────────

    private static void templatePlayer(Path fnDir, String ns, String fn, String purpose) throws IOException {
        Path dir = fnDir.resolve("player");
        FileUtil.mkdirs(dir);

        FileUtil.write(dir.resolve(fn + ".mcfunction"), """
                # %s:player/%s — %s
                # Player data utility.
                # Call:  function %s:player/%s with storage %s:input Args
                # Args:  {target: "@s", action: "heal"}
                #
                # Supported actions: heal, feed, glow, unglow

                $execute if data storage %s:input {action:"heal"} as $(target) run effect give @s instant_health 1 3 true
                $execute if data storage %s:input {action:"feed"} as $(target) run food add @s 20 0.0
                $execute if data storage %s:input {action:"glow"} as $(target) run effect give @s glowing 999999 0 true
                $execute if data storage %s:input {action:"unglow"} as $(target) run effect clear @s glowing
                """.formatted(ns, fn, purpose, ns, fn, ns, ns, ns, ns, ns));
    }

    // ── entity ────────────────────────────────────────────────────────────────

    private static void templateEntity(Path fnDir, String ns, String fn, String purpose) throws IOException {
        Path dir = fnDir.resolve("entity");
        FileUtil.mkdirs(dir);

        FileUtil.write(dir.resolve(fn + ".mcfunction"), """
                # %s:entity/%s — %s
                # Entity management macro.
                # Call:  function %s:entity/%s with storage %s:input Args
                # Args:  {type: "minecraft:zombie", x: 0.0, y: 64.0, z: 0.0, nbt: "{}"}
                $summon $(type) $(x) $(y) $(z) $(nbt)
                """.formatted(ns, fn, purpose, ns, fn, ns));
    }

    // ── geo ───────────────────────────────────────────────────────────────────

    private static void templateGeo(Path fnDir, String ns, String fn, String purpose) throws IOException {
        Path dir = fnDir.resolve("geo");
        FileUtil.mkdirs(dir);

        FileUtil.write(dir.resolve(fn + ".mcfunction"), """
                # %s:geo/%s — %s
                # Geometric / positional operations.
                # Reads/writes coordinates from storage.
                #
                # Example: teleport entity to stored position
                # Call:  function %s:geo/%s with storage %s:input Args
                # Args:  {target: "@s", x: 0, y: 64, z: 0}
                $tp $(target) $(x) $(y) $(z)
                """.formatted(ns, fn, purpose, ns, fn, ns));
    }

    // ── dialog ────────────────────────────────────────────────────────────────

    private static void templateDialog(Path fnDir, String ns, String fn, String purpose) throws IOException {
        Path dir = fnDir.resolve("dialog");
        FileUtil.mkdirs(dir);

        // dialog JSON tag file
        Path dialogDir = fnDir.getParent().getParent().resolve("dialog");
        FileUtil.mkdirs(dialogDir.resolve(ns));
        FileUtil.writeJson(dialogDir.resolve(ns).resolve(fn + ".json"), dialogJson(ns, fn, purpose));

        FileUtil.write(dir.resolve(fn + ".mcfunction"), """
                # %s:dialog/%s — %s
                # Show a dialog to the target player (1.21.6+ / pack_format 80+).
                # Call:  function %s:dialog/%s with storage %s:input Args
                # Args:  {target: "@s"}
                $dialog show $(target) %s:%s
                """.formatted(ns, fn, purpose, ns, fn, ns, ns, fn));

        FileUtil.write(dir.resolve(fn + "_inline.mcfunction"), """
                # %s:dialog/%s_inline — Show a dialog from storage (dynamic)
                # Write full dialog JSON to %s:input DialogData then call this.
                # Call:  function %s:dialog/%s_inline with storage %s:input Args
                # Args:  {target: "@s", dialog_data: "<inline JSON string>"}
                $dialog show $(target) $(dialog_data)
                """.formatted(ns, fn, ns, ns, fn, ns));
    }

    private static String dialogJson(String ns, String fn, String purpose) {
        return """
                {
                  "type": "minecraft:notice",
                  "title": {"text": "%s", "color": "gold"},
                  "body": {
                    "type": "plain_message",
                    "contents": {"text": "%s"}
                  },
                  "action": {
                    "type": "run_command",
                    "label": {"text": "OK"},
                    "command": "function %s:dialog/%s_ok"
                  },
                  "can_close_with_escape": true,
                  "pause": false
                }
                """.formatted(fn, purpose, ns, fn);
    }

    // ── inv ───────────────────────────────────────────────────────────────────

    private static void templateInv(Path fnDir, String ns, String fn, String purpose) throws IOException {
        Path dir = fnDir.resolve("inv");
        FileUtil.mkdirs(dir);

        FileUtil.write(dir.resolve(fn + ".mcfunction"), """
                # %s:inv/%s — %s
                # Inventory manipulation macro.
                # Call:  function %s:inv/%s with storage %s:input Args
                # Args:  {target: "@s", slot: "weapon.mainhand",
                #         item_id: "minecraft:diamond_sword", count: 1, components: "{}"}
                $item replace entity $(target) $(slot) with $(item_id)[$(components)] $(count)
                """.formatted(ns, fn, purpose, ns, fn, ns));
    }

    // ── wand ──────────────────────────────────────────────────────────────────

    private static void templateWand(Path fnDir, String ns, String fn, String purpose) throws IOException {
        Path dir = fnDir.resolve("wand");
        FileUtil.mkdirs(dir);

        FileUtil.write(dir.resolve(fn + ".mcfunction"), """
                # %s:wand/%s — %s
                # Wand / tool right-click detection (1.21.11+ post_piercing_attack enchantment).
                # Add post_piercing_attack enchantment to detect left-click (26_1 overlay method).
                # Right-click: use carrot_on_a_stick + trigger scoreboard.
                #
                # Example: right-click handler for a custom wand
                execute as @a \
                    if entity @s[nbt={SelectedItem:{id:"minecraft:carrot_on_a_stick"}}] \
                    if entity @s[scores={%s_trigger=1..}] \
                    at @s \
                    run function %s:wand/%s_use

                scoreboard players set @a %s_trigger 0
                """.formatted(ns, fn, purpose, ns, ns, fn, ns));

        FileUtil.write(dir.resolve(fn + "_use.mcfunction"), """
                # %s:wand/%s_use — Called when wand is right-clicked
                say [Wand] %s used at @s
                """.formatted(ns, fn, fn));
    }

    // ── interaction ───────────────────────────────────────────────────────────

    private static void templateInteraction(Path fnDir, String ns, String fn, String purpose) throws IOException {
        Path dir = fnDir.resolve("interaction");
        FileUtil.mkdirs(dir);

        FileUtil.write(dir.resolve(fn + ".mcfunction"), """
                # %s:interaction/%s — %s
                # Interaction entity sensor for left-click detection (< pack_format 88).
                # Summon an interaction entity, check its attack NBT each tick.
                #
                # Spawn:  function %s:interaction/%s_spawn
                # Tick:   include %s:interaction/%s in your tick function

                execute as @e[type=minecraft:interaction,tag=%s.sensor] \
                    if entity @s[nbt={attack:{player:{}}}] \
                    at @s \
                    run function %s:interaction/%s_fire

                execute as @e[type=minecraft:interaction,tag=%s.sensor] \
                    run data remove entity @s attack
                """.formatted(ns, fn, purpose, ns, fn, ns, fn, ns, ns, fn, ns));

        FileUtil.write(dir.resolve(fn + "_spawn.mcfunction"), """
                # %s:interaction/%s_spawn — Summon the interaction sensor entity
                summon minecraft:interaction ~ ~ ~ {Tags:["%s.sensor"],width:1.0f,height:1.0f,response:1b}
                """.formatted(ns, fn, ns));

        FileUtil.write(dir.resolve(fn + "_fire.mcfunction"), """
                # %s:interaction/%s_fire — Called when interaction entity is left-clicked
                say [Interaction] %s clicked!
                """.formatted(ns, fn, fn));
    }

    // ── particle ──────────────────────────────────────────────────────────────

    private static void templateParticle(Path fnDir, String ns, String fn, String purpose) throws IOException {
        Path dir = fnDir.resolve("particle");
        FileUtil.mkdirs(dir);

        FileUtil.write(dir.resolve(fn + ".mcfunction"), """
                # %s:particle/%s — %s
                # Particle effect macro.
                # Call:  function %s:particle/%s with storage %s:input Args
                # Args:  {type: "minecraft:dust", x: 0.0, y: 64.0, z: 0.0,
                #         dx: 0.5, dy: 0.5, dz: 0.5, speed: 0.1, count: 20}
                $particle $(type) $(x) $(y) $(z) $(dx) $(dy) $(dz) $(speed) $(count)
                """.formatted(ns, fn, purpose, ns, fn, ns));
    }

    // ── uuid ──────────────────────────────────────────────────────────────────

    private static void templateUuid(Path fnDir, String ns, String fn, String purpose) throws IOException {
        Path dir = fnDir.resolve("uuid");
        FileUtil.mkdirs(dir);

        FileUtil.write(dir.resolve(fn + ".mcfunction"), """
                # %s:uuid/%s — %s
                # Store and retrieve entity UUID from/to storage.
                # Call:  function %s:uuid/%s with storage %s:input Args
                # Args:  {storage_path: "Entities.Primary"}
                # Writes UUID array of @s to %s:engine at the given path.
                $execute store result storage %s:engine $(storage_path)[0] int 1 run data get entity @s UUID[0]
                $execute store result storage %s:engine $(storage_path)[1] int 1 run data get entity @s UUID[1]
                $execute store result storage %s:engine $(storage_path)[2] int 1 run data get entity @s UUID[2]
                $execute store result storage %s:engine $(storage_path)[3] int 1 run data get entity @s UUID[3]
                """.formatted(ns, fn, purpose, ns, fn, ns, ns, ns, ns, ns, ns));
    }
}
