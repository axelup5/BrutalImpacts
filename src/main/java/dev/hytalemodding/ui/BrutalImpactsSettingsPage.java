package dev.hytalemodding.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.commands.BrutalImpactsCommand;
import dev.hytalemodding.config.BrutalImpactsTuningSettings;
import dev.hytalemodding.config.BrutalImpactsTuningStore;
import dev.hytalemodding.systems.BrutalImpactsParticleSystem;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Locale;

public final class BrutalImpactsSettingsPage extends InteractiveCustomUIPage<BrutalImpactsSettingsPage.PageEventData> {

    private static final String PAGE_LAYOUT = "BrutalImpactsSettingsPage.ui";
    private static final String ACTION_APPLY = "apply";
    private static final String ACTION_PRESET = "preset";
    private static final String ACTION_RELOAD = "reload";
    private static final String ACTION_DEBUG = "debug";
    private static final String ACTION_CLOSE = "close";

    private final BrutalImpactsParticleSystem particleSystem;
    private final BrutalImpactsTuningStore tuningStore;
    private final BrutalImpactsCommand reloadDelegate;

    public BrutalImpactsSettingsPage(
        @Nonnull PlayerRef playerRef,
        @Nonnull BrutalImpactsParticleSystem particleSystem,
        @Nonnull BrutalImpactsTuningStore tuningStore,
        @Nonnull BrutalImpactsCommand reloadDelegate
    ) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageEventData.CODEC);
        this.particleSystem = particleSystem;
        this.tuningStore = tuningStore;
        this.reloadDelegate = reloadDelegate;
    }

    @Override
    public void build(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull UICommandBuilder commands,
        @Nonnull UIEventBuilder events,
        @Nonnull Store<EntityStore> store
    ) {
        commands.append(PAGE_LAYOUT);
        this.populate(commands);
        this.bindEvents(events);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageEventData data) {
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        UICommandBuilder update = new UICommandBuilder();

        String action = data.action == null ? "" : data.action;
        switch (action) {
            case ACTION_APPLY -> {
                BrutalImpactsTuningSettings current = this.tuningStore.get().normalized();
                BrutalImpactsTuningSettings next = new BrutalImpactsTuningSettings(
                    valueOrDefault(data.minScale, current.minScale()),
                    valueOrDefault(data.maxScale, current.maxScale()),
                    valueOrDefault(data.scaleMultiplier, current.scaleMultiplier()),
                    valueOrDefault(data.particleMultiplier, current.particleMultiplier())
                ).normalized();

                this.particleSystem.applyTuningSettings(next);
                try {
                    this.tuningStore.save(next);
                    this.populate(update);
                    this.setStatus(update, "Settings saved and applied.");
                } catch (IOException e) {
                    this.populate(update);
                    this.setStatus(update, "Could not save USER_RuntimeSettings.json: " + e.getMessage());
                }
            }
            case ACTION_PRESET -> {
                BrutalImpactsTuningSettings preset = switch ((data.preset == null ? "" : data.preset).toLowerCase(Locale.ROOT)) {
                    case "clean" -> BrutalImpactsTuningSettings.CLEAN;
                    case "brutal" -> BrutalImpactsTuningSettings.BRUTAL;
                    default -> BrutalImpactsTuningSettings.NORMAL;
                };

                this.particleSystem.applyTuningSettings(preset);
                try {
                    this.tuningStore.save(preset);
                    this.populate(update);
                    this.setStatus(update, "Preset " + this.currentPresetName() + " applied.");
                } catch (IOException e) {
                    this.populate(update);
                    this.setStatus(update, "Could not save the preset: " + e.getMessage());
                }
            }
            case ACTION_RELOAD -> {
                String status = this.reloadDelegate.reloadAll();
                try {
                    BrutalImpactsTuningSettings loaded = this.tuningStore.load().normalized();
                    this.particleSystem.applyTuningSettings(loaded);
                } catch (IOException e) {
                    status = status + "Runtime settings: error reloading (" + e.getMessage() + ").";
                }

                this.populate(update);
                this.setStatus(update, status);
                if (playerRef != null) {
                    playerRef.sendMessage(Message.raw("[BrutalImpacts] " + status));
                }
            }
            case ACTION_DEBUG -> {
                boolean enabled = this.particleSystem.toggleDebug();
                this.populate(update);
                this.setStatus(update, "Debug " + (enabled ? "ON" : "OFF") + ".");
            }
            case ACTION_CLOSE -> {
                close();
                return;
            }
            default -> this.populate(update);
        }

        sendUpdate(update);
    }

    private void bindEvents(@Nonnull UIEventBuilder events) {
        events.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#MinScale",
            EventData.of("Action", ACTION_APPLY).append("@MinScale", "#MinScale.Value").append("@MaxScale", "#MaxScale.Value")
                .append("@ScaleMultiplier", "#ScaleMultiplier.Value").append("@ParticleMultiplier", "#ParticleMultiplier.Value"),
            false
        );
        events.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#MaxScale",
            EventData.of("Action", ACTION_APPLY).append("@MinScale", "#MinScale.Value").append("@MaxScale", "#MaxScale.Value")
                .append("@ScaleMultiplier", "#ScaleMultiplier.Value").append("@ParticleMultiplier", "#ParticleMultiplier.Value"),
            false
        );
        events.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#ScaleMultiplier",
            EventData.of("Action", ACTION_APPLY).append("@MinScale", "#MinScale.Value").append("@MaxScale", "#MaxScale.Value")
                .append("@ScaleMultiplier", "#ScaleMultiplier.Value").append("@ParticleMultiplier", "#ParticleMultiplier.Value"),
            false
        );
        events.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#ParticleMultiplier",
            EventData.of("Action", ACTION_APPLY).append("@MinScale", "#MinScale.Value").append("@MaxScale", "#MaxScale.Value")
                .append("@ScaleMultiplier", "#ScaleMultiplier.Value").append("@ParticleMultiplier", "#ParticleMultiplier.Value"),
            false
        );

        events.addEventBinding(CustomUIEventBindingType.Activating, "#CleanPreset", EventData.of("Action", ACTION_PRESET).append("Preset", "clean"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#NormalPreset", EventData.of("Action", ACTION_PRESET).append("Preset", "normal"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BrutalPreset", EventData.of("Action", ACTION_PRESET).append("Preset", "brutal"));
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#ApplyButton",
            EventData.of("Action", ACTION_APPLY).append("@MinScale", "#MinScale.Value").append("@MaxScale", "#MaxScale.Value")
                .append("@ScaleMultiplier", "#ScaleMultiplier.Value").append("@ParticleMultiplier", "#ParticleMultiplier.Value")
        );
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ReloadButton", EventData.of("Action", ACTION_RELOAD));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#DebugButton", EventData.of("Action", ACTION_DEBUG));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", EventData.of("Action", ACTION_CLOSE));
    }

    private void populate(@Nonnull UICommandBuilder commands) {
        BrutalImpactsTuningSettings settings = this.tuningStore.get().normalized();

        commands.set("#MinScale.Value", settings.minScale());
        commands.set("#MaxScale.Value", settings.maxScale());
        commands.set("#ScaleMultiplier.Value", settings.scaleMultiplier());
        commands.set("#ParticleMultiplier.Value", settings.particleMultiplier());

        //commands.set("#MinScaleValue.Text", format(settings.minScale()));
        //commands.set("#MaxScaleValue.Text", format(settings.maxScale()));
        //commands.set("#ScaleMultiplierValue.Text", format(settings.scaleMultiplier()));
        //commands.set("#ParticleMultiplierValue.Text", format(settings.particleMultiplier()));

        commands.set("#PresetValue.Text", this.currentPresetName());
        commands.set("#DebugValue.Text", this.particleSystem.isDebugEnabled() ? "ON" : "OFF");
        commands.set("#StatusText.Text", "Done.");
    }

    private void setStatus(@Nonnull UICommandBuilder commands, @Nonnull String text) {
        commands.set("#StatusText.Text", text);
    }

    @Nonnull
    private String currentPresetName() {
        BrutalImpactsTuningSettings current = this.tuningStore.get().normalized();
        if (current.equals(BrutalImpactsTuningSettings.CLEAN)) {
            return "Clean";
        }
        if (current.equals(BrutalImpactsTuningSettings.BRUTAL)) {
            return "BRUTAL";
        }
        if (current.equals(BrutalImpactsTuningSettings.NORMAL)) {
            return "Normal";
        }
        return "Custom";
    }

    private static float valueOrDefault(Float value, float fallback) {
        return value == null ? fallback : value;
    }

    @Nonnull
    private static String format(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    public static final class PageEventData {
        public static final BuilderCodec<PageEventData> CODEC = BuilderCodec.builder(PageEventData.class, PageEventData::new)
            .append(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action).add()
            .append(new KeyedCodec<>("Preset", Codec.STRING), (d, v) -> d.preset = v, d -> d.preset).add()
            .append(new KeyedCodec<>("@MinScale", Codec.FLOAT), (d, v) -> d.minScale = v, d -> d.minScale).add()
            .append(new KeyedCodec<>("@MaxScale", Codec.FLOAT), (d, v) -> d.maxScale = v, d -> d.maxScale).add()
            .append(new KeyedCodec<>("@ScaleMultiplier", Codec.FLOAT), (d, v) -> d.scaleMultiplier = v, d -> d.scaleMultiplier).add()
            .append(new KeyedCodec<>("@ParticleMultiplier", Codec.FLOAT), (d, v) -> d.particleMultiplier = v, d -> d.particleMultiplier).add()
            .build();

        public String action;
        public String preset;
        public Float minScale;
        public Float maxScale;
        public Float scaleMultiplier;
        public Float particleMultiplier;
    }
}
