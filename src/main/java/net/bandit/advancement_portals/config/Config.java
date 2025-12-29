package net.bandit.advancement_portals.config;

import net.bandit.advancement_portals.AdvPortalsMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.*;

@EventBusSubscriber(modid = AdvPortalsMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config {

    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.ConfigValue<List<? extends String>> dimensionAdvancements;
    private static volatile Map<String, List<String>> CACHED = Map.of();

    static {
        List<String> defaultAdvancements = List.of(
                "minecraft:the_nether=[minecraft:story/enchant_item,minecraft:story/mine_diamond]",
                "minecraft:the_end=[minecraft:nether/obtain_ancient_debris]"
        );

        dimensionAdvancements = BUILDER
                .comment("Advancement requirements for each dimension (format: dimension=[adv1,adv2,...])")
                .defineList("dimensionRequirements", defaultAdvancements, obj -> obj instanceof String);

        SPEC = BUILDER.build();
    }

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) {
            CACHED = parseDimensionAdvancements(dimensionAdvancements.get());
        }
    }

    @SubscribeEvent
    public static void onReload(final ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) {
            CACHED = parseDimensionAdvancements(dimensionAdvancements.get());
        }
    }

    public static Map<String, List<String>> getDimensionAdvancements() {
        return CACHED;
    }

    private static Map<String, List<String>> parseDimensionAdvancements(List<? extends String> rawEntries) {
        Map<String, List<String>> map = new HashMap<>();

        for (String entry : rawEntries) {
            if (entry == null) continue;

            String[] parts = entry.split("=", 2);
            if (parts.length != 2) continue;

            String dimension = parts[0].trim();
            String rawList = parts[1].trim();

            if (!rawList.startsWith("[") || !rawList.endsWith("]")) continue;

            rawList = rawList.substring(1, rawList.length() - 1).trim();
            if (rawList.isEmpty()) {
                map.put(dimension, List.of());
                continue;
            }

            String[] advancements = rawList.split(",");
            List<String> advList = new ArrayList<>(advancements.length);
            for (String adv : advancements) {
                String a = adv.trim();
                if (!a.isEmpty()) advList.add(a);
            }

            map.put(dimension, List.copyOf(advList));
        }

        return Map.copyOf(map);
    }
}
