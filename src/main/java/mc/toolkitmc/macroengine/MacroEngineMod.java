package mc.toolkitmc.macroengine;

import mc.toolkitmc.macroengine.command.MacroEngineCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MacroEngine — Fabric mod entry point.
 *
 * Developer-only toolkit. All commands require permission level 2 (op).
 * Not intended for public distribution or Modrinth upload.
 */
public class MacroEngineMod implements ModInitializer {

    public static final String MOD_ID  = "macroengine";
    public static final String VERSION = "1.0.0";
    public static final Logger LOGGER  = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(
            (dispatcher, registryAccess, environment) ->
                MacroEngineCommand.register(dispatcher)
        );
        LOGGER.info("[MacroEngine] Fabric mod v{} initialised.", VERSION);
    }
}
