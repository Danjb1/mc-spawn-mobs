package com.example.examplemod;

import com.example.examplemod.server.event.CustomCommands;
import com.example.examplemod.server.event.MobSpawner;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("examplemod")
public class ExampleMod {

    public static final String ID = "examplemod";

    public ExampleMod() {
        subscribeToEvents();
    }

    private void subscribeToEvents() {
        MobSpawner spawner = new MobSpawner();
        MinecraftForge.EVENT_BUS.register(new CustomCommands(spawner));
        MinecraftForge.EVENT_BUS.register(spawner);
    }

}
