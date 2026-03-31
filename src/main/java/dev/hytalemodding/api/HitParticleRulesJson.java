package dev.hytalemodding.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class HitParticleRulesJson {

    private static final Gson GSON = new GsonBuilder().setLenient().create();

    private HitParticleRulesJson() {
    }

    /**
     * Loads rules from a JSON resource on the classpath. This clears the registry first.
     *
     * @return number of loaded rules
     */
    public static int clearAndLoadFromResource(
        @Nonnull HitParticleRegistry registry,
        @Nonnull Class<?> resourceContext,
        @Nonnull String resourcePath
    ) throws IOException {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(resourceContext, "resourceContext");
        Objects.requireNonNull(resourcePath, "resourcePath");

        try (InputStream in = resourceContext.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            return clearAndLoad(registry, new InputStreamReader(in, StandardCharsets.UTF_8));
        }
    }

    /**
     * Loads rules from a JSON file. This clears the registry first.
     *
     * @return number of loaded rules
     */
    public static int clearAndLoadFromFile(@Nonnull HitParticleRegistry registry, @Nonnull Path path) throws IOException {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(path, "path");

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return clearAndLoad(registry, reader);
        }
    }

    /**
     * Loads rules from a Reader. This clears the registry first.
     *
     * @return number of loaded rules
     */
    public static int clearAndLoad(@Nonnull HitParticleRegistry registry, @Nonnull Reader reader) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(reader, "reader");

        Config config;
        try {
            config = GSON.fromJson(reader, Config.class);
        } catch (JsonParseException e) {
            throw new IllegalArgumentException("Invalid hit-particles JSON", e);
        }

        List<Rule> rules = config == null ? List.of() : safeList(config.rules);

        registry.clear();

        int loaded = 0;
        for (Rule rule : rules) {
            if (rule == null || rule.match == null) {
                continue;
            }

            String type = rule.match.type == null ? "" : rule.match.type.trim().toLowerCase(Locale.ROOT);
            String value = rule.match.value == null ? null : rule.match.value.trim();
            if (value == null || value.isBlank()) {
                continue;
            }

            List<Effect> effects = rule.effects != null ? rule.effects : null;
            if (effects == null || effects.isEmpty()) {
                if (rule.particleSystemId == null || rule.particleSystemId.isBlank()) {
                    continue;
                }
                effects = List.of(effectFromSingle(rule.particleSystemId, rule.color, rule.scale));
            }

            ArrayList<HitParticleEffect> compiledEffects = new ArrayList<>(effects.size());
            for (Effect effect : effects) {
                if (effect == null || effect.particleSystemId == null || effect.particleSystemId.isBlank()) {
                    continue;
                }
                compiledEffects.add(effectToRuntime(effect));
            }

            if (compiledEffects.isEmpty()) {
                continue;
            }

            HitParticleEffect[] compiledArray = compiledEffects.toArray(new HitParticleEffect[0]);
            switch (type) {
                case "exact" -> registry.registerModelExact(value, compiledArray);
                case "prefix" -> registry.registerModelPrefix(value, compiledArray);
                case "contains" -> registry.registerModelContains(value, compiledArray);
                default -> throw new IllegalArgumentException("Unknown match.type: " + rule.match.type);
            }
            loaded++;
        }

        return loaded;
    }

    private static Effect effectFromSingle(@Nonnull String particleSystemId, Color color, Number scale) {
        Effect effect = new Effect();
        effect.particleSystemId = particleSystemId;
        effect.color = color;
        effect.scale = scale;
        return effect;
    }

    private static HitParticleEffect effectToRuntime(@Nonnull Effect effect) {
        float scale = effect.scale == null ? 1.0F : effect.scale.floatValue();
        if (effect.color == null) {
            return HitParticleEffect.of(effect.particleSystemId, scale);
        }

        int r = effect.color.r == null ? 0 : effect.color.r;
        int g = effect.color.g == null ? 0 : effect.color.g;
        int b = effect.color.b == null ? 0 : effect.color.b;
        return HitParticleEffect.tinted(effect.particleSystemId, r, g, b, scale);
    }

    private static <T> List<T> safeList(List<T> list) {
        return list == null ? List.of() : list;
    }

    private static final class Config {
        private List<Rule> rules;
    }

    private static final class Rule {
        private Match match;
        private List<Effect> effects;

        // Convenience: allow a single effect without "effects": []
        private String particleSystemId;
        private Color color;
        private Number scale;
    }

    private static final class Match {
        private String type;
        private String value;
    }

    private static final class Effect {
        private String particleSystemId;
        private Color color;
        private Number scale;
    }

    private static final class Color {
        private Integer r;
        private Integer g;
        private Integer b;
    }
}

