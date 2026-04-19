package mc.toolkitmc.macroengine.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import mc.toolkitmc.macroengine.MacroEngineMod;
import mc.toolkitmc.macroengine.generator.DatapackGenerator;
import mc.toolkitmc.macroengine.generator.ResourcePackGenerator;
import mc.toolkitmc.macroengine.module.ModuleRegistry;
import mc.toolkitmc.macroengine.util.FeedbackUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Registers the /macro-engine command tree.
 *
 * Usage:
 *   /macro-engine create <dp|rp> <name> <namespace> <tick_fn> <load_fn>
 *   /macro-engine add    <module> <pack_name> <function> <purpose>
 *   /macro-engine run    <namespace:function> [with <storage_id> <path>]
 *   /macro-engine list   [packs|modules]
 *   /macro-engine info
 *   /macro-engine help
 */
public final class MacroEngineCommand {

    private MacroEngineCommand() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("macro-engine")
                .requires(src -> src.hasPermissionLevel(2))

                // ── create ─────────────────────────────────────────────────────────
                .then(CommandManager.literal("create")
                    .then(CommandManager.argument("type", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            builder.suggest("dp", () -> Text.literal("Datapack"));
                            builder.suggest("rp", () -> Text.literal("Resource Pack"));
                            return builder.buildFuture();
                        })
                        .then(CommandManager.argument("name", StringArgumentType.word())
                            .then(CommandManager.argument("namespace", StringArgumentType.word())
                                .then(CommandManager.argument("tick_fn", StringArgumentType.word())
                                    .then(CommandManager.argument("load_fn", StringArgumentType.word())
                                        .executes(MacroEngineCommand::executeCreate)))))))

                // ── add ────────────────────────────────────────────────────────────
                .then(CommandManager.literal("add")
                    .then(CommandManager.argument("module", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            ModuleRegistry.moduleNames().forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .then(CommandManager.argument("pack_name", StringArgumentType.word())
                            .then(CommandManager.argument("function", StringArgumentType.word())
                                .then(CommandManager.argument("purpose", StringArgumentType.greedyString())
                                    .executes(MacroEngineCommand::executeAdd))))))

                // ── run ────────────────────────────────────────────────────────────
                .then(CommandManager.literal("run")
                    .then(CommandManager.argument("fn_id", StringArgumentType.word())
                        // run without 'with'
                        .executes(ctx -> executeRun(ctx, null, null))
                        // run with storage
                        .then(CommandManager.literal("with")
                            .then(CommandManager.argument("storage_id", StringArgumentType.word())
                                .then(CommandManager.argument("storage_path", StringArgumentType.word())
                                    .executes(ctx -> executeRun(
                                        ctx,
                                        StringArgumentType.getString(ctx, "storage_id"),
                                        StringArgumentType.getString(ctx, "storage_path"))))))))

                // ── list ───────────────────────────────────────────────────────────
                .then(CommandManager.literal("list")
                    .executes(ctx -> executeList(ctx, "all"))
                    .then(CommandManager.literal("packs").executes(ctx -> executeList(ctx, "packs")))
                    .then(CommandManager.literal("modules").executes(ctx -> executeList(ctx, "modules"))))

                // ── info ───────────────────────────────────────────────────────────
                .then(CommandManager.literal("info")
                    .executes(MacroEngineCommand::executeInfo))

                // ── help ───────────────────────────────────────────────────────────
                .then(CommandManager.literal("help")
                    .executes(MacroEngineCommand::executeHelp))
        );

        MacroEngineMod.LOGGER.info("[MacroEngine] /macro-engine command registered.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // create
    // ─────────────────────────────────────────────────────────────────────────

    private static int executeCreate(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        String type      = StringArgumentType.getString(ctx, "type");
        String name      = StringArgumentType.getString(ctx, "name");
        String namespace = StringArgumentType.getString(ctx, "namespace");
        String tickFn    = StringArgumentType.getString(ctx, "tick_fn");
        String loadFn    = StringArgumentType.getString(ctx, "load_fn");

        ServerCommandSource source = ctx.getSource();
        MinecraftServer      server = source.getServer();

        // Basic validation
        if (!name.matches("[a-z0-9_.-]+")) {
            source.sendError(Text.literal("[MacroEngine] Pack name must be lowercase alphanumeric (a-z, 0-9, _, -, .)."));
            return 0;
        }
        if (!namespace.matches("[a-z0-9_]+")) {
            source.sendError(Text.literal("[MacroEngine] Namespace must be lowercase alphanumeric (a-z, 0-9, _)."));
            return 0;
        }

        try {
            switch (type.toLowerCase()) {
                case "dp" -> {
                    Path datapacksDir = server.getSavePath(WorldSavePath.DATAPACKS);
                    Files.createDirectories(datapacksDir);
                    DatapackGenerator.generate(datapacksDir, name, namespace, tickFn, loadFn);
                    FeedbackUtil.success(source, "Datapack §e" + name + "§a created at §7datapacks/" + name + "/");
                    FeedbackUtil.info(source, "Tick: §7" + namespace + ":" + tickFn + "  §fLoad: §7" + namespace + ":" + loadFn);
                    FeedbackUtil.hint(source, "Run §f/datapack enable \"file/" + name + "\"§7 to activate it.");
                }
                case "rp" -> {
                    Path rpDir = server.getRunDirectory().resolve("resource_packs").resolve(name);
                    Files.createDirectories(rpDir.getParent());
                    ResourcePackGenerator.generate(rpDir, name, namespace);
                    FeedbackUtil.success(source, "Resource pack §e" + name + "§a created at §7resource_packs/" + name + "/");
                }
                default -> {
                    source.sendError(Text.literal("[MacroEngine] Unknown type '" + type + "'. Use §fdp§c or §frp§c."));
                    return 0;
                }
            }
        } catch (Exception e) {
            MacroEngineMod.LOGGER.error("[MacroEngine] create failed", e);
            source.sendError(Text.literal("[MacroEngine] Error: " + e.getMessage()));
            return 0;
        }

        return 1;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // add
    // ─────────────────────────────────────────────────────────────────────────

    private static int executeAdd(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        String module    = StringArgumentType.getString(ctx, "module");
        String packName  = StringArgumentType.getString(ctx, "pack_name");
        String function  = StringArgumentType.getString(ctx, "function");
        String purpose   = StringArgumentType.getString(ctx, "purpose");

        ServerCommandSource source = ctx.getSource();
        MinecraftServer     server = source.getServer();

        if (!ModuleRegistry.exists(module)) {
            source.sendError(Text.literal("[MacroEngine] Unknown module '" + module + "'. Run §f/macro-engine list modules§c for the full list."));
            return 0;
        }

        Path datapacksDir = server.getSavePath(WorldSavePath.DATAPACKS);
        Path packRoot     = datapacksDir.resolve(packName);

        if (!Files.isDirectory(packRoot)) {
            source.sendError(Text.literal("[MacroEngine] Pack '" + packName + "' not found in datapacks/. Create it first with §f/macro-engine create dp§c."));
            return 0;
        }

        try {
            // Try to detect namespace from pack.mcmeta or infer from first data/ subfolder
            String namespace = detectNamespace(packRoot, packName);
            ModuleRegistry.addModule(packRoot, namespace, module, function, purpose);
            FeedbackUtil.success(source, "Module §e" + module + "§a added to §e" + packName);
            FeedbackUtil.info(source, "Function: §7" + namespace + ":" + function);
            FeedbackUtil.hint(source, "Purpose: §7" + purpose);
        } catch (Exception e) {
            MacroEngineMod.LOGGER.error("[MacroEngine] add failed", e);
            source.sendError(Text.literal("[MacroEngine] Error: " + e.getMessage()));
            return 0;
        }

        return 1;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // run
    // ─────────────────────────────────────────────────────────────────────────

    private static int executeRun(CommandContext<ServerCommandSource> ctx,
                                  String storageId, String storagePath) throws CommandSyntaxException {
        String fnId = StringArgumentType.getString(ctx, "fn_id");
        ServerCommandSource source = ctx.getSource();
        MinecraftServer server = source.getServer();

        String cmd;
        if (storageId != null && storagePath != null) {
            // Macro call: function <id> with storage <storageId> <path>
            cmd = "function " + fnId + " with storage " + storageId + " " + storagePath;
        } else {
            cmd = "function " + fnId;
        }

        try {
            int result = server.getCommandManager()
                .getDispatcher()
                .execute(cmd, source.withLevel(4)); // op level 4 for function execution
            FeedbackUtil.success(source, "Executed §7" + cmd + "§a → result: §f" + result);
            return result;
        } catch (Exception e) {
            source.sendError(Text.literal("[MacroEngine] Run failed: " + e.getMessage()));
            MacroEngineMod.LOGGER.warn("[MacroEngine] run error for '{}': {}", fnId, e.getMessage());
            return 0;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // list
    // ─────────────────────────────────────────────────────────────────────────

    private static int executeList(CommandContext<ServerCommandSource> ctx, String filter) {
        ServerCommandSource source = ctx.getSource();
        MinecraftServer server = source.getServer();

        if (!filter.equals("modules")) {
            // List installed datapacks
            Path datapacksDir = server.getSavePath(WorldSavePath.DATAPACKS);
            source.sendFeedback(() -> Text.literal("§6[MacroEngine] §fDatapacks in §7datapacks/§f:"), false);
            try {
                if (Files.isDirectory(datapacksDir)) {
                    Files.list(datapacksDir).forEach(p -> {
                        String nm = p.getFileName().toString();
                        boolean hasMeta = Files.exists(p.resolve("pack.mcmeta"));
                        source.sendFeedback(() -> Text.literal("  §7• §f" + nm + (hasMeta ? "" : " §c(no pack.mcmeta)")), false);
                    });
                } else {
                    source.sendFeedback(() -> Text.literal("  §7(none)"), false);
                }
            } catch (Exception e) {
                source.sendError(Text.literal("[MacroEngine] Could not read datapacks dir: " + e.getMessage()));
            }
        }

        if (!filter.equals("packs")) {
            // List available modules
            source.sendFeedback(() -> Text.literal("§6[MacroEngine] §fAvailable modules:"), false);
            List<String> names = ModuleRegistry.moduleNames();
            for (int i = 0; i < names.size(); i += 4) {
                int fi = i;
                int end = Math.min(i + 4, names.size());
                String line = "  §7" + String.join("  §7", names.subList(fi, end));
                source.sendFeedback(() -> Text.literal(line), false);
            }
        }

        return 1;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // info
    // ─────────────────────────────────────────────────────────────────────────

    private static int executeInfo(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        source.sendFeedback(() -> Text.literal(
            "§6╔═══════════════════════════════╗\n" +
            "§6║  §fMacroEngine §7Fabric Mod       §6║\n" +
            "§6║  §7Version: §f" + MacroEngineMod.VERSION + "                  §6║\n" +
            "§6║  §7Org:     §fToolkitMC            §6║\n" +
            "§6║  §7Target:  §fMinecraft 1.21.x     §6║\n" +
            "§6╚═══════════════════════════════╝"
        ), false);
        return 1;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // help
    // ─────────────────────────────────────────────────────────────────────────

    private static int executeHelp(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        source.sendFeedback(() -> Text.literal("§6[MacroEngine] §fCommand Reference:"), false);
        String[] lines = {
            "§e/macro-engine create <dp|rp> <name> <namespace> <tick_fn> <load_fn>",
            "  §7Scaffold a new datapack (dp) or resource pack (rp).",
            "",
            "§e/macro-engine add <module> <pack_name> <function> <purpose>",
            "  §7Inject a module template into an existing datapack.",
            "  §7<purpose> may be a description or a command string for cmd-type modules.",
            "",
            "§e/macro-engine run <namespace:function> [with <storage_id> <path>]",
            "  §7Execute a datapack macro function, optionally passing storage args.",
            "",
            "§e/macro-engine list [packs|modules]",
            "  §7List installed datapacks or available module types.",
            "",
            "§e/macro-engine info   §7— Show mod version info.",
            "§e/macro-engine help   §7— Show this message."
        };
        for (String l : lines) {
            source.sendFeedback(() -> Text.literal(l), false);
        }
        return 1;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Detect the primary namespace of an existing datapack. */
    private static String detectNamespace(Path packRoot, String packName) {
        Path dataDir = packRoot.resolve("data");
        if (Files.isDirectory(dataDir)) {
            try {
                return Files.list(dataDir)
                    .filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .filter(s -> !s.equals("minecraft"))
                    .findFirst()
                    .orElse(packName.toLowerCase().replace("-", "_"));
            } catch (Exception ignored) {}
        }
        return packName.toLowerCase().replace("-", "_");
    }
}
