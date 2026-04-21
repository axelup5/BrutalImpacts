package dev.hytalemodding.config;

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
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/**
 * JSON loader for the weapon/source tuning config.
 */
public final class WeaponTuningRulesJson {

    private WeaponTuningRulesJson() {
    }

    @Nonnull
    public static LoadResult loadIntoFromFileReport(@Nonnull WeaponTuningRegistry registry, @Nonnull Path path) throws IOException {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(path, "path");

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return loadIntoReport(registry, reader);
        }
    }

    @Nonnull
    public static LoadResult loadIntoReport(@Nonnull WeaponTuningRegistry registry, @Nonnull Reader reader) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(reader, "reader");

        JsonObject root = parseRoot(reader);
        JsonArray sourceRules = getArray(root, "sources");
        JsonArray weaponRules = getArray(root, "weapons");
        boolean hadSection = sourceRules != null || weaponRules != null;

        int loaded = 0;
        loaded += loadSourceRules(registry, sourceRules);
        loaded += loadWeaponRules(registry, weaponRules);
        return new LoadResult(loaded, hadSection);
    }

    @Nonnull
    private static JsonObject parseRoot(@Nonnull Reader reader) {
        try {
            JsonReader jsonReader = new JsonReader(reader);
            jsonReader.setLenient(true);
            JsonElement rootEl = JsonParser.parseReader(jsonReader);
            return rootEl != null && rootEl.isJsonObject() ? rootEl.getAsJsonObject() : new JsonObject();
        } catch (JsonParseException e) {
            throw new IllegalArgumentException("Invalid weapons JSON", e);
        }
    }

    private static int loadSourceRules(@Nonnull WeaponTuningRegistry registry, @Nullable JsonArray rules) {
        if (rules == null) {
            return 0;
        }

        int loaded = 0;
        for (JsonElement ruleEl : rules) {
            if (ruleEl == null || !ruleEl.isJsonObject()) {
                continue;
            }

            JsonObject rule = ruleEl.getAsJsonObject();
            String kind = getString(rule, "kind");
            if (kind == null || kind.isBlank()) {
                continue;
            }

            registry.registerSourceKind(kind, readProfile(rule));
            loaded++;
        }
        return loaded;
    }

    private static int loadWeaponRules(@Nonnull WeaponTuningRegistry registry, @Nullable JsonArray rules) {
        if (rules == null) {
            return 0;
        }

        int loaded = 0;
        for (JsonElement ruleEl : rules) {
            if (ruleEl == null || !ruleEl.isJsonObject()) {
                continue;
            }

            JsonObject rule = ruleEl.getAsJsonObject();
            JsonObject match = getObject(rule, "match");
            if (match == null) {
                continue;
            }

            String type = getString(match, "type");
            String value = getString(match, "value");
            if (type == null || value == null || value.isBlank()) {
                continue;
            }

            WeaponTuningProfile profile = readProfile(rule);
            switch (type.trim().toLowerCase(Locale.ROOT)) {
                case "exact" -> registry.registerWeaponExact(value, profile);
                case "prefix" -> registry.registerWeaponPrefix(value, profile);
                case "contains" -> registry.registerWeaponContains(value, profile);
                default -> throw new IllegalArgumentException("Unknown weapon match.type: " + type);
            }
            loaded++;
        }
        return loaded;
    }

    @Nonnull
    private static WeaponTuningProfile readProfile(@Nonnull JsonObject obj) {
        Float scaleMultiplier = getFloat(obj, "scaleMultiplier");
        Float particleMultiplier = getFloat(obj, "particleMultiplier");
        return new WeaponTuningProfile(
            scaleMultiplier == null ? 1.0F : scaleMultiplier,
            particleMultiplier == null ? 1.0F : particleMultiplier
        );
    }

    @Nullable
    private static JsonArray getArray(@Nonnull JsonObject obj, @Nonnull String key) {
        if (!obj.has(key)) {
            return null;
        }
        JsonElement el = obj.get(key);
        return el != null && el.isJsonArray() ? el.getAsJsonArray() : null;
    }

    @Nullable
    private static JsonObject getObject(@Nonnull JsonObject obj, @Nonnull String key) {
        if (!obj.has(key)) {
            return null;
        }
        JsonElement el = obj.get(key);
        return el != null && el.isJsonObject() ? el.getAsJsonObject() : null;
    }

    @Nullable
    private static String getString(@Nonnull JsonObject obj, @Nonnull String key) {
        if (!obj.has(key)) {
            return null;
        }
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull() || !el.isJsonPrimitive()) {
            return null;
        }
        return el.getAsString();
    }

    @Nullable
    private static Float getFloat(@Nonnull JsonObject obj, @Nonnull String key) {
        if (!obj.has(key)) {
            return null;
        }
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull() || !el.isJsonPrimitive() || !el.getAsJsonPrimitive().isNumber()) {
            return null;
        }
        return el.getAsFloat();
    }

    public record LoadResult(int entriesLoaded, boolean hadSection) {
    }
}
