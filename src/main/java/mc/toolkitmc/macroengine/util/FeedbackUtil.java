package mc.toolkitmc.macroengine.util;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

/**
 * Convenience helpers for sending styled feedback to command sources.
 *
 * Colour scheme:
 *   success → §a (green)
 *   info    → §b (aqua)
 *   hint    → §7 (gray)
 *   error   → §c (red)   — use source.sendError() directly
 */
public final class FeedbackUtil {

    private static final String PREFIX = "§6[MacroEngine]§r ";

    private FeedbackUtil() {}

    public static void success(ServerCommandSource source, String message) {
        source.sendFeedback(() -> Text.literal(PREFIX + "§a" + message), false);
    }

    public static void info(ServerCommandSource source, String message) {
        source.sendFeedback(() -> Text.literal(PREFIX + "§b" + message), false);
    }

    public static void hint(ServerCommandSource source, String message) {
        source.sendFeedback(() -> Text.literal(PREFIX + "§7" + message), false);
    }

    public static void warn(ServerCommandSource source, String message) {
        source.sendFeedback(() -> Text.literal(PREFIX + "§e" + message), false);
    }
}
