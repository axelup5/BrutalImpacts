package dev.hytalemodding.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

/**
 * JSON loader for Brutal Impacts hit particle rules.
 *
 * <p>The JSON format is intentionally simple and is used by the mod's on-disk configuration files. Rules are
 * appended into a {@link HitParticleRegistry} using one of three match modes:</p>
 *
 * <ul>
 *   <li>{@code exact}: model id must match exactly</li>
 *   <li>{@code prefix}: model id must start with the configured value</li>
 *   <li>{@code contains}: model id must contain the configured value</li>
 * </ul>
 *
 * <p>Each rule can define either a single effect via {@code particleSystemId/color/scale} or multiple effects
 * via {@code effects: [{ particleSystemId, color, scale }, ...]}.</p>
 */
public final class HitParticleRulesJson {

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
     * Loads rules from a JSON resource on the classpath (does NOT clear the registry).
     *
     * @return number of loaded rules
     */
    public static int loadIntoFromResource(
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
            return loadInto(registry, new InputStreamReader(in, StandardCharsets.UTF_8));
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
     * Loads rules from a JSON file (does NOT clear the registry).
     *
     * @return number of loaded rules
     */
    public static int loadIntoFromFile(@Nonnull HitParticleRegistry registry, @Nonnull Path path) throws IOException {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(path, "path");

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return loadInto(registry, reader);
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

        registry.clear();
        return loadInto(registry, reader);
    }

    /**
     * Loads rules from a Reader (does NOT clear the registry).
     *
     * @return number of loaded rules
     */
    public static int loadInto(@Nonnull HitParticleRegistry registry, @Nonnull Reader reader) {
        return loadIntoReport(registry, reader).entriesLoaded();
    }

    @Nonnull
    public static LoadResult loadIntoFromFileReport(@Nonnull HitParticleRegistry registry, @Nonnull Path path) throws IOException {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(path, "path");

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return loadIntoReport(registry, reader);
        }
    }

    @Nonnull
    public static LoadResult loadIntoReport(@Nonnull HitParticleRegistry registry, @Nonnull Reader reader) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(reader, "reader");

        JsonObject root;
        try {
            JsonReader jsonReader = new JsonReader(reader);
            jsonReader.setLenient(true);
            JsonElement rootEl = JsonParser.parseReader(jsonReader);
            root = rootEl != null && rootEl.isJsonObject() ? rootEl.getAsJsonObject() : new JsonObject();
        } catch (JsonParseException e) {
            throw new IllegalArgumentException("Invalid hit-particles JSON", e);
        }

        boolean hadSection = root.has("rules") && root.get("rules").isJsonArray();
        JsonArray rules = hadSection ? root.getAsJsonArray("rules") : new JsonArray();

        int loaded = 0;
        for (JsonElement ruleEl : rules) {
            if (ruleEl == null || !ruleEl.isJsonObject()) {
                continue;
            }

            JsonObject rule = ruleEl.getAsJsonObject();
            JsonObject match = getObj(rule, "match");
            if (match == null) {
                continue;
            }

            String type = getString(match, "type");
            type = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
            String value = getString(match, "value");
            value = value == null ? null : value.trim();
            if (value == null || value.isBlank()) {
                continue;
            }

            JsonArray effects = rule.has("effects") && rule.get("effects").isJsonArray() ? rule.getAsJsonArray("effects") : null;
            ArrayList<HitParticleEffect> compiledEffects = new ArrayList<>(effects == null ? 1 : effects.size());
            if (effects == null || effects.isEmpty()) {
                HitParticleEffect single = compileSingleEffectFromRule(rule);
                if (single == null) {
                    continue;
                }
                compiledEffects.add(single);
            } else {
                for (JsonElement effectEl : effects) {
                    if (effectEl == null || !effectEl.isJsonObject()) {
                        continue;
                    }
                    HitParticleEffect compiled = compileEffect(effectEl.getAsJsonObject());
                    if (compiled != null) {
                        compiledEffects.add(compiled);
                    }
                }
            }

            if (compiledEffects.isEmpty()) {
                continue;
            }

            HitParticleEffect[] compiledArray = compiledEffects.toArray(new HitParticleEffect[0]);
            switch (type) {
                case "exact" -> registry.registerModelExact(value, compiledArray);
                case "prefix" -> registry.registerModelPrefix(value, compiledArray);
                case "contains" -> registry.registerModelContains(value, compiledArray);
                default -> throw new IllegalArgumentException("Unknown match.type: " + getString(match, "type"));
            }
            loaded++;
        }

        return new LoadResult(loaded, hadSection);
    }

    @Nullable
    private static HitParticleEffect compileSingleEffectFromRule(@Nonnull JsonObject rule) {
        String particleSystemId = getString(rule, "particleSystemId");
        if (particleSystemId == null || particleSystemId.isBlank()) {
            return null;
        }

        Float scale = getNumberAsFloat(rule, "scale");
        float scaleValue = scale == null ? 1.0F : scale;
        return compileEffectFields(particleSystemId, scaleValue, rule, "color");
    }

    @Nullable
    private static HitParticleEffect compileEffect(@Nonnull JsonObject effectObj) {
        String particleSystemId = getString(effectObj, "particleSystemId");
        if (particleSystemId == null || particleSystemId.isBlank()) {
            return null;
        }

        Float scale = getNumberAsFloat(effectObj, "scale");
        float scaleValue = scale == null ? 1.0F : scale;
        return compileEffectFields(particleSystemId, scaleValue, effectObj, "color");
    }

    @Nullable
    private static HitParticleEffect compileEffectFields(
        @Nonnull String particleSystemId,
        float scale,
        @Nonnull JsonObject obj,
        @Nonnull String colorField
    ) {
        if (!obj.has(colorField)) {
            return HitParticleEffect.of(particleSystemId, scale);
        }

        JsonElement colorEl = obj.get(colorField);
        if (colorEl == null || colorEl.isJsonNull()) {
            return HitParticleEffect.noTint(particleSystemId, scale);
        }

        if (!colorEl.isJsonObject()) {
            throw new IllegalArgumentException("Invalid color field for particleSystemId=" + particleSystemId + " (expected object or null)");
        }

        JsonObject colorObj = colorEl.getAsJsonObject();
        Integer r = getNumberAsInt(colorObj, "r");
        Integer g = getNumberAsInt(colorObj, "g");
        Integer b = getNumberAsInt(colorObj, "b");
        return HitParticleEffect.tinted(particleSystemId, r == null ? 0 : r, g == null ? 0 : g, b == null ? 0 : b, scale);
    }

    @Nullable
    private static JsonObject getObj(@Nonnull JsonObject obj, @Nonnull String key) {
        if (!obj.has(key)) {
            return null;
        }
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull() || !el.isJsonObject()) {
            return null;
        }
        return el.getAsJsonObject();
    }

    @Nullable
    private static String getString(@Nonnull JsonObject obj, @Nonnull String key) {
        if (!obj.has(key)) {
            return null;
        }
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) {
            return null;
        }
        return el.isJsonPrimitive() ? el.getAsString() : null;
    }

    @Nullable
    private static Float getNumberAsFloat(@Nonnull JsonObject obj, @Nonnull String key) {
        if (!obj.has(key)) {
            return null;
        }
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) {
            return null;
        }
        return el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber() ? el.getAsFloat() : null;
    }

    @Nullable
    private static Integer getNumberAsInt(@Nonnull JsonObject obj, @Nonnull String key) {
        if (!obj.has(key)) {
            return null;
        }
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) {
            return null;
        }
        return el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber() ? el.getAsInt() : null;
    }

    public record LoadResult(int entriesLoaded, boolean hadSection) {
    }
}
