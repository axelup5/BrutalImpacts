package dev.hytalemodding.config;

import dev.hytalemodding.api.HitParticleRegistry;
import dev.hytalemodding.api.HitParticleRulesJson;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * File/layout utilities for Brutal Impacts' on-disk configuration.
 *
 * <p>This mod uses a data directory named {@value #DIR_NAME} and stores a small set of bundled files plus
 * user-editable JSON configuration. On startup (and on reload), the mod:</p>
 *
 * <ul>
 *   <li>Creates the directory if needed</li>
 *   <li>Overwrites bundled docs + defaults (safe to replace on update)</li>
 *   <li>Creates the user file once and never overwrites it</li>
 *   <li>Loads all {@code *.json} rules with a stable priority order</li>
 * </ul>
 *
 * <p>You can override the base directory by setting the JVM system property {@code brutalimpacts.dir}.
 * The final data dir will be {@code <override>}/{@value #DIR_NAME}.</p>
 */
public final class BrutalImpactsFiles {

    public static final String DIR_NAME = "BrutalImpacts";

    public static final String DEFAULT_HIT_PARTICLES_FILE = "DefaultHitParticles_ReadOnly.json";
    public static final String USER_HIT_PARTICLES_FILE = "USER_HitParticles.json";
    public static final String DEFAULT_WEAPONS_FILE = "Weapons_ReadOnly.json";
    public static final String USER_WEAPONS_FILE = "USER_Weapons.json";
    public static final String README_FILE = "README.md";

    private BrutalImpactsFiles() {
    }

    /**
     * Resolves the directory where Brutal Impacts should store/read its JSON files.
     *
     * <p>Resolution order:</p>
     * <ol>
     *   <li>If {@code -Dbrutalimpacts.dir=<path>} is set, returns {@code <path>/BrutalImpacts/}</li>
     *   <li>Otherwise, returns {@code BrutalImpacts/} next to the plugin code source (usually the JAR)</li>
     *   <li>Fallback: {@code ./BrutalImpacts/}</li>
     * </ol>
     */
    @Nonnull
    public static Path resolveDataDir(@Nonnull Class<?> pluginClass) {
        Objects.requireNonNull(pluginClass, "pluginClass");

        String override = System.getProperty("brutalimpacts.dir");
        if (override != null && !override.isBlank()) {
            return Path.of(override).resolve(DIR_NAME);
        }

        try {
            URI uri = pluginClass.getProtectionDomain().getCodeSource().getLocation().toURI();
            Path codeSource = Paths.get(uri);
            Path base = Files.isRegularFile(codeSource) && codeSource.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar")
                ? codeSource.getParent()
                : Path.of(".");
            return base.resolve(DIR_NAME);
        } catch (Exception e) {
            return Path.of(".").resolve(DIR_NAME);
        }
    }

    /**
     * Ensures the on-disk directory layout exists and writes bundled files.
     *
     * <p>Bundled files are refreshed on every run (to reflect mod updates), except
     * {@value #USER_HIT_PARTICLES_FILE} which is created once and never overwritten.</p>
     */
    public static void ensureLayout(@Nonnull Class<?> pluginClass, @Nonnull Path dataDir) throws IOException {
        Objects.requireNonNull(pluginClass, "pluginClass");
        Objects.requireNonNull(dataDir, "dataDir");

        Files.createDirectories(dataDir);

        // Always refresh bundled docs + default rules when the mod updates.
        copyResource(pluginClass, "/brutalimpacts/" + README_FILE, dataDir.resolve(README_FILE), true);
        copyResource(pluginClass, "/brutalimpacts/" + DEFAULT_HIT_PARTICLES_FILE, dataDir.resolve(DEFAULT_HIT_PARTICLES_FILE), true);
        copyResource(pluginClass, "/brutalimpacts/" + DEFAULT_WEAPONS_FILE, dataDir.resolve(DEFAULT_WEAPONS_FILE), true);

        // Never overwrite the user's file once created.
        copyResource(pluginClass, "/brutalimpacts/" + USER_HIT_PARTICLES_FILE, dataDir.resolve(USER_HIT_PARTICLES_FILE), false);
        copyResource(pluginClass, "/brutalimpacts/" + USER_WEAPONS_FILE, dataDir.resolve(USER_WEAPONS_FILE), false);
    }

    /**
     * Loads all JSON files in the data directory with priority:
     * 1) USER_HitParticles.json
     * 2) Any other *.json (sorted by filename)
     * 3) DefaultHitParticles_ReadOnly.json
     */
    public static int clearAndLoadAllHitParticleJson(@Nonnull HitParticleRegistry registry, @Nonnull Path dataDir) throws IOException {
        return clearAndLoadAllHitParticleJsonReport(registry, dataDir).rulesLoaded();
    }

    /**
     * Loads all JSON files in the data directory with priority:
     * 1) USER_HitParticles.json
     * 2) Any other *.json (sorted by filename)
     * 3) DefaultHitParticles_ReadOnly.json
     */
    @Nonnull
    public static JsonLoadReport clearAndLoadAllHitParticleJsonReport(@Nonnull HitParticleRegistry registry, @Nonnull Path dataDir) throws IOException {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(dataDir, "dataDir");

        registry.clear();
        return loadJsonFiles(
            dataDir,
            USER_HIT_PARTICLES_FILE,
            DEFAULT_HIT_PARTICLES_FILE,
            path -> {
                HitParticleRulesJson.LoadResult result = HitParticleRulesJson.loadIntoFromFileReport(registry, path);
                return new SectionLoadResult(result.entriesLoaded(), result.hadSection());
            }
        );
    }

    public static int clearAndLoadAllWeaponTuningJson(@Nonnull WeaponTuningRegistry registry, @Nonnull Path dataDir) throws IOException {
        return clearAndLoadAllWeaponTuningJsonReport(registry, dataDir).rulesLoaded();
    }

    @Nonnull
    public static JsonLoadReport clearAndLoadAllWeaponTuningJsonReport(@Nonnull WeaponTuningRegistry registry, @Nonnull Path dataDir) throws IOException {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(dataDir, "dataDir");

        registry.clear();
        return loadJsonFiles(
            dataDir,
            USER_WEAPONS_FILE,
            DEFAULT_WEAPONS_FILE,
            path -> {
                WeaponTuningRulesJson.LoadResult result = WeaponTuningRulesJson.loadIntoFromFileReport(registry, path);
                return new SectionLoadResult(result.entriesLoaded(), result.hadSection());
            }
        );
    }

    public record JsonLoadReport(int rulesLoaded, int filesLoaded, int filesFailed, @Nonnull List<JsonLoadError> errors) {
        public JsonLoadReport {
            errors = List.copyOf(errors);
        }
    }

    public record JsonLoadError(@Nonnull Path path, @Nonnull String message) {
    }

    @Nonnull
    private static JsonLoadReport loadJsonFiles(
        @Nonnull Path dataDir,
        @Nonnull String userFileName,
        @Nonnull String defaultFileName,
        @Nonnull JsonSectionLoader loader
    ) throws IOException {
        int loadedRules = 0;
        int loadedFiles = 0;
        int failedFiles = 0;
        ArrayList<JsonLoadError> errors = new ArrayList<>();

        Path user = dataDir.resolve(userFileName);
        if (Files.exists(user)) {
            try {
                SectionLoadResult result = loader.load(user);
                if (result.hadSection()) {
                    loadedRules += result.entriesLoaded();
                    loadedFiles++;
                }
            } catch (Exception e) {
                failedFiles++;
                errors.add(new JsonLoadError(user, e.getMessage()));
            }
        }

        ArrayList<Path> modderFiles = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dataDir, "*.json")) {
            for (Path p : stream) {
                if (p == null) {
                    continue;
                }
                String name = p.getFileName().toString();
                if (name.equals(userFileName) || name.equals(defaultFileName)) {
                    continue;
                }
                modderFiles.add(p);
            }
        }

        modderFiles.sort(Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)));
        for (Path p : modderFiles) {
            try {
                SectionLoadResult result = loader.load(p);
                if (result.hadSection()) {
                    loadedRules += result.entriesLoaded();
                    loadedFiles++;
                }
            } catch (Exception e) {
                failedFiles++;
                errors.add(new JsonLoadError(p, e.getMessage()));
            }
        }

        Path defaults = dataDir.resolve(defaultFileName);
        if (Files.exists(defaults)) {
            try {
                SectionLoadResult result = loader.load(defaults);
                if (result.hadSection()) {
                    loadedRules += result.entriesLoaded();
                    loadedFiles++;
                }
            } catch (Exception e) {
                failedFiles++;
                errors.add(new JsonLoadError(defaults, e.getMessage()));
            }
        }

        return new JsonLoadReport(loadedRules, loadedFiles, failedFiles, errors);
    }

    /**
     * Copies a classpath resource to disk.
     *
     * @param overwrite whether an existing file should be replaced
     */
    private static void copyResource(@Nonnull Class<?> pluginClass, @Nonnull String resourcePath, @Nonnull Path out, boolean overwrite) throws IOException {
        Objects.requireNonNull(pluginClass, "pluginClass");
        Objects.requireNonNull(resourcePath, "resourcePath");
        Objects.requireNonNull(out, "out");

        if (!overwrite && Files.exists(out)) {
            return;
        }

        try (InputStream in = pluginClass.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            Files.createDirectories(out.getParent());
            Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @FunctionalInterface
    private interface JsonSectionLoader {
        @Nonnull SectionLoadResult load(@Nonnull Path path) throws Exception;
    }

    private record SectionLoadResult(int entriesLoaded, boolean hadSection) {
    }
}
