package dev.hytalemodding.config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

/**
 * In-memory registry for weapon/source tuning rules.
 *
 * <p>Resolution is split into two independent layers:</p>
 * <ul>
 *   <li>Source kind rules ({@code projectile}, {@code melee}, {@code unarmed}, {@code other})</li>
 *   <li>Weapon item-id match rules (exact/prefix/contains)</li>
 * </ul>
 *
 * <p>The final tuning profile is the multiplication of both first-match results.</p>
 */
public final class WeaponTuningRegistry {

    private final Object lock = new Object();
    private final ArrayList<SourceRule> sourceRules = new ArrayList<>();
    private final ArrayList<WeaponRule> weaponRules = new ArrayList<>();

    private volatile SourceRule[] sourceRulesSnapshot = new SourceRule[0];
    private volatile WeaponRule[] weaponRulesSnapshot = new WeaponRule[0];

    public void clear() {
        synchronized (this.lock) {
            this.sourceRules.clear();
            this.weaponRules.clear();
            this.sourceRulesSnapshot = new SourceRule[0];
            this.weaponRulesSnapshot = new WeaponRule[0];
        }
    }

    public void registerSourceKind(@Nonnull String kind, @Nonnull WeaponTuningProfile profile) {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(profile, "profile");

        synchronized (this.lock) {
            this.sourceRules.add(new SourceRule(normalize(kind), profile));
            this.sourceRulesSnapshot = this.sourceRules.toArray(new SourceRule[0]);
        }
    }

    public void registerWeaponExact(@Nonnull String value, @Nonnull WeaponTuningProfile profile) {
        registerWeaponRule(MatchType.EXACT, value, profile);
    }

    public void registerWeaponPrefix(@Nonnull String value, @Nonnull WeaponTuningProfile profile) {
        registerWeaponRule(MatchType.PREFIX, value, profile);
    }

    public void registerWeaponContains(@Nonnull String value, @Nonnull WeaponTuningProfile profile) {
        registerWeaponRule(MatchType.CONTAINS, value, profile);
    }

    @Nonnull
    public WeaponTuningProfile resolve(@Nonnull String sourceKind, @Nullable String weaponItemId) {
        Objects.requireNonNull(sourceKind, "sourceKind");

        WeaponTuningProfile sourceProfile = resolveSourceProfile(sourceKind);
        WeaponTuningProfile weaponProfile = resolveWeaponProfile(weaponItemId);
        return sourceProfile.multiply(weaponProfile);
    }

    @Nonnull
    private WeaponTuningProfile resolveSourceProfile(@Nonnull String sourceKind) {
        String normalized = normalize(sourceKind);
        for (SourceRule rule : this.sourceRulesSnapshot) {
            if (rule.kind.equals(normalized)) {
                return rule.profile;
            }
        }
        return WeaponTuningProfile.IDENTITY;
    }

    @Nonnull
    private WeaponTuningProfile resolveWeaponProfile(@Nullable String weaponItemId) {
        if (weaponItemId == null || weaponItemId.isBlank()) {
            return WeaponTuningProfile.IDENTITY;
        }

        String normalized = normalize(weaponItemId);
        for (WeaponRule rule : this.weaponRulesSnapshot) {
            if (rule.matches(normalized)) {
                return rule.profile;
            }
        }
        return WeaponTuningProfile.IDENTITY;
    }

    private void registerWeaponRule(@Nonnull MatchType type, @Nonnull String value, @Nonnull WeaponTuningProfile profile) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(profile, "profile");

        synchronized (this.lock) {
            this.weaponRules.add(new WeaponRule(type, normalize(value), profile));
            this.weaponRulesSnapshot = this.weaponRules.toArray(new WeaponRule[0]);
        }
    }

    @Nonnull
    private static String normalize(@Nonnull String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private enum MatchType {
        EXACT,
        PREFIX,
        CONTAINS
    }

    private record SourceRule(@Nonnull String kind, @Nonnull WeaponTuningProfile profile) {
    }

    private record WeaponRule(@Nonnull MatchType type, @Nonnull String value, @Nonnull WeaponTuningProfile profile) {
        private boolean matches(@Nonnull String weaponItemId) {
            return switch (this.type) {
                case EXACT -> weaponItemId.equals(this.value);
                case PREFIX -> weaponItemId.startsWith(this.value);
                case CONTAINS -> weaponItemId.contains(this.value);
            };
        }
    }
}
