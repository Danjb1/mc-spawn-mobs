package com.example.examplemod.server.event;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntitySummonArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class CustomCommands {

    private MobSpawner spawner;

    public CustomCommands(MobSpawner spawner) {
        this.spawner = spawner;
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        // setSpawnActive [true|false]
        dispatcher.register(Commands.literal("setSpawnActive").requires((cmd) -> {
            return cmd.hasPermission(2);
        }).then(Commands.argument("value", BoolArgumentType.bool()).executes((cmd) -> {
            return setSpawnActive(cmd.getSource(), BoolArgumentType.getBool(cmd, "value"));
         })));

        // setSpawnMob [mobName]
        dispatcher.register(Commands.literal("setSpawnMob").requires((cmd) -> {
           return cmd.hasPermission(2);
        }).then(Commands.argument("entity", EntitySummonArgument.id()).suggests(SuggestionProviders.SUMMONABLE_ENTITIES).executes((cmd) -> {
           return setSpawnMob(cmd.getSource(), EntitySummonArgument.getSummonableEntity(cmd, "entity"));
        })));

        // setSpawnInterval [minSecs] [maxSecs]
        dispatcher.register(Commands.literal("setSpawnInterval").requires((cmd) -> {
            return cmd.hasPermission(2);
        }).then(Commands.argument("minSecs", IntegerArgumentType.integer(1))
                .then(Commands.argument("maxSecs", IntegerArgumentType.integer(1)).executes((cmd) -> {
            return setSpawnInterval(cmd.getSource(),
                    IntegerArgumentType.getInteger(cmd, "minSecs"),
                    IntegerArgumentType.getInteger(cmd, "maxSecs"));
         }))));

        // setSpawnCount [min] [max]
        dispatcher.register(Commands.literal("setSpawnCount").requires((cmd) -> {
            return cmd.hasPermission(2);
        }).then(Commands.argument("min", IntegerArgumentType.integer(1))
                .then(Commands.argument("max", IntegerArgumentType.integer(1)).executes((cmd) -> {
            return setSpawnCount(cmd.getSource(),
                    IntegerArgumentType.getInteger(cmd, "min"),
                    IntegerArgumentType.getInteger(cmd, "max"));
         }))));

        // setSpawnRadius [blocks]
        dispatcher.register(Commands.literal("setSpawnCount").requires((cmd) -> {
            return cmd.hasPermission(2);
        }).then(Commands.argument("dist", IntegerArgumentType.integer(1)).executes((cmd) -> {
            return setSpawnRadius(cmd.getSource(),
                    IntegerArgumentType.getInteger(cmd, "dist"));
         })));
    }

    private int setSpawnActive(CommandSourceStack cmd, boolean active) {
        spawner.setSpawningActive(active);
        if (active) {
            cmd.sendSuccess(new TranslatableComponent("Spawning enabled"), true);
        } else {
            cmd.sendSuccess(new TranslatableComponent("Spawning disabled"), true);
        }
        return 1;
    }

    private int setSpawnMob(CommandSourceStack cmd, ResourceLocation entityType) throws CommandSyntaxException {
        spawner.setMobToSpawn(entityType);
        cmd.sendSuccess(new TranslatableComponent(entityType + " will spawn"), true);
        return 1;
    }

    private int setSpawnInterval(CommandSourceStack cmd, int minSecs,
            int maxSecs) {
        spawner.setInterval(minSecs, maxSecs);
        cmd.sendSuccess(new TranslatableComponent("Mobs will spawn every " + minSecs + "-" + maxSecs + " seconds"), true);
        return 1;
    }

    private int setSpawnCount(CommandSourceStack cmd, int min, int max) {
        spawner.setNumMobsToSpawn(min, max);
        cmd.sendSuccess(new TranslatableComponent(min + "-" + max + " mobs will spawn"), true);
        return 1;
    }

    private int setSpawnRadius(CommandSourceStack cmd, int dist) {
        spawner.setMaxSpawnDist(dist);
        cmd.sendSuccess(new TranslatableComponent("Mobs will spawn up to " + dist + " blocks away"), true);
        return 1;
    }

}
