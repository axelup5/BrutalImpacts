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
import java.util.Locale;
import java.util.Objects;

public final class BrutalImpactsFiles {

    public static final String DIR_NAME = "BrutalImpacts";

    public static final String DEFAULT_HIT_PARTICLES_FILE = "DefaultHitParticles_ReadOnly.json";
    public static final String USER_HIT_PARTICLES_FILE = "USER_HitParticles.json";
    public static final String README_FILE = "README.md";

    private BrutalImpactsFiles() {
    }

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

    public static void ensureLayout(@Nonnull Class<?> pluginClass, @Nonnull Path dataDir) throws IOException {
        Objects.requireNonNull(pluginClass, "pluginClass");
        Objects.requireNonNull(dataDir, "dataDir");

        Files.createDirectories(dataDir);

        // Always refresh bundled docs + default rules when the mod updates.
        copyResource(pluginClass, "/brutalimpacts/" + README_FILE, dataDir.resolve(README_FILE), true);
        copyResource(pluginClass, "/brutalimpacts/" + DEFAULT_HIT_PARTICLES_FILE, dataDir.resolve(DEFAULT_HIT_PARTICLES_FILE), true);

        // Never overwrite the user's file once created.
        copyResource(pluginClass, "/brutalimpacts/" + USER_HIT_PARTICLES_FILE, dataDir.resolve(USER_HIT_PARTICLES_FILE), false);
    }

    /**
     * Loads all JSON files in the data directory with priority:
     * 1) USER_HitParticles.json
     * 2) Any other *.json (sorted by filename)
     * 3) DefaultHitParticles_ReadOnly.json
     */
    public static int clearAndLoadAllHitParticleJson(@Nonnull HitParticleRegistry registry, @Nonnull Path dataDir) throws IOException {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(dataDir, "dataDir");

        registry.clear();

        int loadedRules = 0;
        Path user = dataDir.resolve(USER_HIT_PARTICLES_FILE);
        if (Files.exists(user)) {
            loadedRules += HitParticleRulesJson.loadIntoFromFile(registry, user);
        }

        ArrayList<Path> modderFiles = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dataDir, "*.json")) {
            for (Path p : stream) {
                if (p == null) {
                    continue;
                }
                String name = p.getFileName().toString();
                if (name.equals(USER_HIT_PARTICLES_FILE) || name.equals(DEFAULT_HIT_PARTICLES_FILE)) {
                    continue;
                }
                modderFiles.add(p);
            }
        }
        modderFiles.sort(Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)));
        for (Path p : modderFiles) {
            loadedRules += HitParticleRulesJson.loadIntoFromFile(registry, p);
        }

        Path defaults = dataDir.resolve(DEFAULT_HIT_PARTICLES_FILE);
        if (Files.exists(defaults)) {
            loadedRules += HitParticleRulesJson.loadIntoFromFile(registry, defaults);
        }

        return loadedRules;
    }

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
}

