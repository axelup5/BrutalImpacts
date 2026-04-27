package dev.hytalemodding.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Persists the runtime tuning sliders used by the Brutal Impacts UI.
 */
public final class BrutalImpactsTuningStore {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path path;
    private volatile BrutalImpactsTuningSettings settings = BrutalImpactsTuningSettings.NORMAL;

    public BrutalImpactsTuningStore(@Nonnull Path path) {
        this.path = Objects.requireNonNull(path, "path");
    }

    @Nonnull
    public BrutalImpactsTuningSettings get() {
        return this.settings;
    }

    @Nonnull
    public BrutalImpactsTuningSettings load() throws IOException {
        if (!Files.exists(this.path)) {
            this.settings = BrutalImpactsTuningSettings.NORMAL;
            return this.settings;
        }

        try (Reader reader = Files.newBufferedReader(this.path)) {
            SettingsFile file = this.gson.fromJson(reader, SettingsFile.class);
            this.settings = toSettings(file).normalized();
            return this.settings;
        }
    }

    public void save(@Nonnull BrutalImpactsTuningSettings settings) throws IOException {
        BrutalImpactsTuningSettings normalized = settings.normalized();
        Files.createDirectories(this.path.getParent());

        try (Writer writer = Files.newBufferedWriter(this.path)) {
            this.gson.toJson(fromSettings(normalized), writer);
        }

        this.settings = normalized;
    }

    @Nonnull
    private static BrutalImpactsTuningSettings toSettings(SettingsFile file) {
        if (file == null) {
            return BrutalImpactsTuningSettings.NORMAL;
        }

        return new BrutalImpactsTuningSettings(
            file.minScale == null ? BrutalImpactsTuningSettings.DEFAULT_MIN_SCALE : file.minScale,
            file.maxScale == null ? BrutalImpactsTuningSettings.DEFAULT_MAX_SCALE : file.maxScale,
            file.scaleMultiplier == null ? BrutalImpactsTuningSettings.DEFAULT_SCALE_MULTIPLIER : file.scaleMultiplier,
            file.particleMultiplier == null ? BrutalImpactsTuningSettings.DEFAULT_PARTICLE_MULTIPLIER : file.particleMultiplier
        );
    }

    @Nonnull
    private static SettingsFile fromSettings(@Nonnull BrutalImpactsTuningSettings settings) {
        SettingsFile file = new SettingsFile();
        file.minScale = settings.minScale();
        file.maxScale = settings.maxScale();
        file.scaleMultiplier = settings.scaleMultiplier();
        file.particleMultiplier = settings.particleMultiplier();
        return file;
    }

    private static final class SettingsFile {
        private Float minScale;
        private Float maxScale;
        private Float scaleMultiplier;
        private Float particleMultiplier;
    }
}
