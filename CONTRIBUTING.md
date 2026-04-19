# Contributing to MacroEngine Fabric

Thank you for your interest in contributing! This is an internal ToolkitMC developer tool, but contributions from trusted collaborators are welcome.

## Development Setup

1. **Prerequisites**: Java 21, Git
2. **Clone**: `git clone https://github.com/ToolkitMC/macroEngine-fabric.git`
3. **Bootstrap Gradle** (first time only, if wrapper jar is missing):
   ```sh
   ./bootstrap.sh
   ```
4. **Build**:
   ```sh
   ./gradlew build
   ```
5. **Test**: Copy the resulting JAR from `build/libs/` into your Fabric server's `mods/` folder.

## Code Style

- Java 21, idiomatic style
- No external dependencies beyond Fabric API
- All commands must require `permissionLevel(2)` (operator)
- Function template strings use Java text blocks (`"""..."""`)
- Generated `.mcfunction` files follow macroEngine conventions:
  - Comments explain purpose and call signature
  - Macro arguments documented as `# Args: {key: value}`
  - Storage namespacing: `<ns>:engine`, `<ns>:input`, `<ns>:output`

## Adding a New Module

1. Add a private static method `templateMyModule(Path, String, String, String)` in `ModuleRegistry.java`
2. Register it in the `static {}` block: `register("my_module", ModuleRegistry::templateMyModule);`
3. Update the `README.md` module table

## Pull Request Guidelines

- Keep PRs focused — one feature or fix per PR
- Describe the change and why it's needed
- Test with a live Fabric server before submitting
- Fill in the PR template

## Commit Style

Follow conventional commits:

```
feat: add <module> module template
fix: correct namespace detection in executeAdd
chore: update Fabric API to 0.111.x
docs: update README module table
```
