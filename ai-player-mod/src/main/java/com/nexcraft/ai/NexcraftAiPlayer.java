package com.nexcraft.ai;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static net.minecraft.server.command.CommandManager.literal;

public final class NexcraftAiPlayer implements ModInitializer {
    private static final Map<UUID, UUID> COMPANIONS = new ConcurrentHashMap<>();
    private static int tickCounter;

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(literal("nexcraftbot")
                .then(literal("spawn").executes(ctx -> spawn(ctx.getSource().getPlayerOrThrow())))
                .then(literal("stop").executes(ctx -> stop(ctx.getSource().getPlayerOrThrow())))
                .then(literal("status").executes(ctx -> status(ctx.getSource().getPlayerOrThrow())))
            )
        );
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    private static int spawn(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        UUID oldId = COMPANIONS.get(player.getUuid());
        if (oldId != null) {
            Entity old = world.getEntity(oldId);
            if (old != null && old.isAlive()) {
                player.sendMessage(Text.literal("NEXCRAFT BOT is already in this world."), false);
                return 0;
            }
        }
        VillagerEntity bot = EntityType.VILLAGER.create(world);
        if (bot == null) return 0;
        Vec3d pos = player.getPos().add(1.5, 0, 1.5);
        bot.refreshPositionAndAngles(pos.x, pos.y, pos.z, player.getYaw(), 0);
        bot.setCustomName(Text.literal("NEXCRAFT BOT"));
        bot.setCustomNameVisible(true);
        bot.setPersistent();
        world.spawnEntity(bot);
        COMPANIONS.put(player.getUuid(), bot.getUuid());
        player.sendMessage(Text.literal("NEXCRAFT BOT spawned. Prototype: use /nexcraftbot stop to remove it."), false);
        return 1;
    }

    private static int stop(ServerPlayerEntity player) {
        UUID id = COMPANIONS.remove(player.getUuid());
        Entity entity = id == null ? null : player.getServerWorld().getEntity(id);
        if (entity != null) entity.discard();
        player.sendMessage(Text.literal(entity == null ? "No active NEXCRAFT BOT found." : "NEXCRAFT BOT removed."), false);
        return entity == null ? 0 : 1;
    }

    private static int status(ServerPlayerEntity player) {
        UUID id = COMPANIONS.get(player.getUuid());
        Entity entity = id == null ? null : player.getServerWorld().getEntity(id);
        player.sendMessage(Text.literal(entity != null && entity.isAlive()
            ? "NEXCRAFT BOT is active in this world." : "NEXCRAFT BOT is not spawned."), false);
        return 1;
    }

    private void tick(MinecraftServer server) {
        if (++tickCounter % 20 != 0) return;
        COMPANIONS.entrySet().removeIf(entry -> {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            if (player == null) return false;
            Entity entity = player.getServerWorld().getEntity(entry.getValue());
            if (!(entity instanceof VillagerEntity villager) || !villager.isAlive()) return true;
            if (villager.squaredDistanceTo(player) > 9.0) {
                villager.getNavigation().startMovingTo(player, 1.0);
            }
            return false;
        });
    }
}
