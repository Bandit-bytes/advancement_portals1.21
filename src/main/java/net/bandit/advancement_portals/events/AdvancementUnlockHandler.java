package net.bandit.advancement_portals.events;

import net.bandit.advancement_portals.AdvPortalsMod;
import net.bandit.advancement_portals.config.Config;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = AdvPortalsMod.MODID, bus = EventBusSubscriber.Bus.GAME)
public class AdvancementUnlockHandler {

    private static final Set<String> notifiedPlayers = ConcurrentHashMap.newKeySet();

    @SubscribeEvent
    public static void onAdvancementUnlocked(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ResourceLocation earnedId = event.getAdvancement().id();
        if (!isAdvancementRelevant(earnedId)) return;

        Map<String, List<String>> dimensionAdvancements = Config.getDimensionAdvancements();

        for (Map.Entry<String, List<String>> entry : dimensionAdvancements.entrySet()) {
            String dimensionId = entry.getKey();
            List<String> required = entry.getValue();
            if (required == null || required.isEmpty()) continue;

            boolean allCompleted = required.stream().allMatch(advStr -> {
                ResourceLocation id = ResourceLocation.parse(advStr);

                AdvancementHolder holder = player.server.getAdvancements().get(id);
                return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
            });

            String key = player.getUUID() + "::" + dimensionId;

            if (allCompleted && notifiedPlayers.add(key)) {
                String niceName = formatDimensionName(dimensionId);

                player.displayClientMessage(
                        Component.literal("§aYou have unlocked access to §e" + niceName + "§a!"),
                        false
                );

                player.sendSystemMessage(Component.literal("§e[✓] You can now enter " + niceName), true);
            }
        }
    }

    private static boolean isAdvancementRelevant(ResourceLocation earned) {
        Map<String, List<String>> map = Config.getDimensionAdvancements();
        String earnedStr = earned.toString();
        for (List<String> reqs : map.values()) {
            if (reqs == null) continue;
            for (String s : reqs) {
                if (earnedStr.equals(s)) return true;
            }
        }
        return false;
    }

    private static String formatDimensionName(String rawId) {
        return switch (rawId) {
            case "minecraft:the_nether" -> "The Nether";
            case "minecraft:the_end" -> "The End";
            case "minecraft:overworld" -> "The Overworld";
            default -> {
                String[] parts = rawId.split(":");
                if (parts.length == 2) {
                    String path = parts[1].replace("_", " ");
                    yield capitalizeWords(path);
                }
                yield rawId;
            }
        };
    }

    private static String capitalizeWords(String input) {
        String[] words = input.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                builder.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }
        return builder.toString().trim();
    }
}
