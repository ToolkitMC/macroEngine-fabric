package mc.toolkitmc.macroengine.module;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Functional interface for module template generators.
 *
 * Implementations receive the target function directory, namespace,
 * desired function name, and a free-form purpose/command string.
 */
@FunctionalInterface
public interface ModuleTemplate {

    /**
     * Generate the module's function file(s).
     *
     * @param fnDir   path to the function directory of the datapack
     *                (e.g. {@code data/<namespace>/function/})
     * @param ns      namespace of the pack
     * @param fn      name of the function to generate
     * @param purpose description or command string for this module instance
     */
    void generate(Path fnDir, String ns, String fn, String purpose) throws IOException;
}
