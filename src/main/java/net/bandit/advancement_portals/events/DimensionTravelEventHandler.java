package net.bandit.advancement_portals.events;

import net.bandit.advancement_portals.AdvPortalsMod;
import net.bandit.advancement_portals.config.Config;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;

import java.util.*;

@EventBusSubscriber(modid = AdvPortalsMod.MODID, bus = EventBusSubscriber.Bus.GAME)
public class DimensionTravelEventHandler {

    private static final Map<UUID, Long> recentWarnings = new HashMap<>();
    private static final long WARNING_COOLDOWN_MS = 2000;

    @SubscribeEvent
    public static void onPlayerChangeDimension(EntityTravelToDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;
        if (serverPlayer.isCreative()) return;

        Map<String, List<String>> dimensionAdvancements = Config.getDimensionAdvancements();
        String dimensionId = event.getDimension().location().toString();

        List<String> requiredAdvancements = dimensionAdvancements.get(dimensionId);
        if (requiredAdvancements == null || requiredAdvancements.isEmpty()) return;

        List<String> missingAdvancements = new ArrayList<>();

        for (String advId : requiredAdvancements) {
            ResourceLocation id = ResourceLocation.parse(advId);

            AdvancementHolder holder = serverPlayer.server.getAdvancements().get(id);
            if (holder == null || !serverPlayer.getAdvancements().getOrStartProgress(holder).isDone()) {
                missingAdvancements.add(advId);
            }
        }

        if (missingAdvancements.isEmpty()) return;

        event.setCanceled(true);

        UUID uuid = serverPlayer.getUUID();
        long now = System.currentTimeMillis();
        if (recentWarnings.containsKey(uuid) && now - recentWarnings.get(uuid) <= WARNING_COOLDOWN_MS) return;
        recentWarnings.put(uuid, now);

        String niceName = formatDimensionName(dimensionId);
        serverPlayer.sendSystemMessage(
                Component.literal("§cYou cannot travel to §6" + niceName + " §cuntil you complete:")
        );

        for (String advId : missingAdvancements) {
            ResourceLocation id = ResourceLocation.parse(advId);
            AdvancementHolder holder = serverPlayer.server.getAdvancements().get(id);

            DisplayInfo display = (holder != null) ? holder.value().display().orElse(null) : null;

            if (display != null) {
                Component title = display.getTitle();
                Component description = display.getDescription();

                Component line = Component.literal(" - ")
                        .append(title.copy().withStyle(style -> style
                                .withItalic(false)
                                .withColor(display.getType().getChatColor())
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, description))
                        ))
                        .append(Component.literal(" [hover to read]").withStyle(style -> style
                                .withColor(net.minecraft.ChatFormatting.GRAY)
                                .withItalic(true)
                        ));

                serverPlayer.sendSystemMessage(line);
            } else {
                serverPlayer.sendSystemMessage(Component.literal(" - §7" + advId));
            }
        }
    }

    private static String formatDimensionName(String rawId) {
        return switch (rawId) {
            case "minecraft:overworld" -> "The Overworld";
            case "minecraft:the_nether" -> "The Nether";
            case "minecraft:the_end" -> "The End";
            default -> {
                String[] parts = rawId.split(":");
                String path = parts.length > 1 ? parts[1] : rawId;
                path = path.replace("_", " ");
                yield Arrays.stream(path.split(" "))
                        .filter(s -> !s.isBlank())
                        .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1))
                        .reduce((a, b) -> a + " " + b)
                        .orElse(path);
            }
        };
    }
}
