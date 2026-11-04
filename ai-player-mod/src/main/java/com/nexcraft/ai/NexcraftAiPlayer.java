package com.nexcraft.ai;

import com.mojang.brigadier.arguments.StringArgumentType;
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

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class NexcraftAiPlayer implements ModInitializer {
    private enum Mode { FOLLOW, STAY }

    private static final Map<UUID, UUID> COMPANIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, Mode> MODES = new ConcurrentHashMap<>();
    private static int tickCounter;

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(literal("nexcraftbot")
                .then(literal("spawn").executes(ctx -> spawn(ctx.getSource().getPlayerOrThrow())))
                .then(literal("follow").executes(ctx -> setMode(ctx.getSource().getPlayerOrThrow(), Mode.FOLLOW)))
                .then(literal("stay").executes(ctx -> setMode(ctx.getSource().getPlayerOrThrow(), Mode.STAY)))
                .then(literal("rename").then(argument("name", StringArgumentType.greedyString())
                    .executes(ctx -> rename(ctx.getSource().getPlayerOrThrow(),
                        StringArgumentType.getString(ctx, "name")))))
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
                player.sendMessage(Text.literal("NEXCRAFT BOT is already in this world. Use /nexcraftbot follow or stay."), false);
                return 0;
            }
            COMPANIONS.remove(player.getUuid());
            MODES.remove(player.getUuid());
        }

        VillagerEntity bot = EntityType.VILLAGER.create(world);
        if (bot == null) {
            player.sendMessage(Text.literal("NEXCRAFT BOT could not spawn in this world."), false);
            return 0;
        }
        Vec3d pos = player.getPos().add(1.5, 0, 1.5);
        bot.refreshPositionAndAngles(pos.x, pos.y, pos.z, player.getYaw(), 0);
        bot.setCustomName(Text.literal("NEXCRAFT BOT"));
        bot.setCustomNameVisible(true);
        bot.setPersistent();
        if (!world.spawnEntity(bot)) {
            player.sendMessage(Text.literal("Minecraft refused to spawn NEXCRAFT BOT."), false);
            return 0;
        }
        COMPANIONS.put(player.getUuid(), bot.getUuid());
        MODES.put(player.getUuid(), Mode.FOLLOW);
        player.sendMessage(Text.literal("NEXCRAFT BOT spawned. It is following you. Commands: /nexcraftbot stay, follow, rename <name>, status, stop."), false);
        return 1;
    }

    private static int setMode(ServerPlayerEntity player, Mode mode) {
        VillagerEntity bot = getBot(player);
        if (bot == null) {
            player.sendMessage(Text.literal("Spawn NEXCRAFT BOT first: /nexcraftbot spawn"), false);
            return 0;
        }
        MODES.put(player.getUuid(), mode);
        if (mode == Mode.STAY) {
            bot.getNavigation().stop();
            player.sendMessage(Text.literal("NEXCRAFT BOT will stay here."), false);
        } else {
            player.sendMessage(Text.literal("NEXCRAFT BOT is following you."), false);
        }
        return 1;
    }

    private static int rename(ServerPlayerEntity player, String requestedName) {
        VillagerEntity bot = getBot(player);
        if (bot == null) {
            player.sendMessage(Text.literal("Spawn NEXCRAFT BOT first: /nexcraftbot spawn"), false);
            return 0;
        }
        String name = requestedName.trim();
        if (name.isEmpty() || name.length() > 32) {
            player.sendMessage(Text.literal("Name must contain 1 to 32 characters."), false);
            return 0;
        }
        bot.setCustomName(Text.literal(name));
        bot.setCustomNameVisible(true);
        player.sendMessage(Text.literal("NEXCRAFT BOT renamed to " + name + "."), false);
        return 1;
    }

    private static int stop(ServerPlayerEntity player) {
        UUID id = COMPANIONS.remove(player.getUuid());
        MODES.remove(player.getUuid());
        Entity entity = id == null ? null : player.getServerWorld().getEntity(id);
        if (entity != null) entity.discard();
        player.sendMessage(Text.literal(entity == null ? "No active NEXCRAFT BOT found in this dimension." : "NEXCRAFT BOT removed."), false);
        return entity == null ? 0 : 1;
    }

    private static int status(ServerPlayerEntity player) {
        VillagerEntity bot = getBot(player);
        if (bot == null) {
            player.sendMessage(Text.literal("NEXCRAFT BOT is not spawned in this dimension."), false);
            return 0;
        }
        Mode mode = MODES.getOrDefault(player.getUuid(), Mode.FOLLOW);
        String name = bot.hasCustomName() ? bot.getCustomName().getString() : "NEXCRAFT BOT";
        player.sendMessage(Text.literal(name + " is active. Mode: " + mode.name() + "."), false);
        return 1;
    }

    private static VillagerEntity getBot(ServerPlayerEntity player) {
        UUID id = COMPANIONS.get(player.getUuid());
        Entity entity = id == null ? null : player.getServerWorld().getEntity(id);
        return entity instanceof VillagerEntity villager && villager.isAlive() ? villager : null;
    }

    private void tick(MinecraftServer server) {
        if (++tickCounter % 20 != 0) return;
        COMPANIONS.entrySet().removeIf(entry -> {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            if (player == null) return false;
            Entity entity = player.getServerWorld().getEntity(entry.getValue());
            if (!(entity instanceof VillagerEntity villager) || !villager.isAlive()) {
                MODES.remove(entry.getKey());
                return true;
            }
            if (MODES.getOrDefault(entry.getKey(), Mode.FOLLOW) == Mode.STAY) {
                villager.getNavigation().stop();
            } else if (villager.squaredDistanceTo(player) > 9.0) {
                villager.getNavigation().startMovingTo(player, 1.0);
            }
            return false;
        });
    }
}
