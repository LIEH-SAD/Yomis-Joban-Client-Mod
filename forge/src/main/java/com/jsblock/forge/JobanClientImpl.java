package com.jsblock.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

public class JobanClientImpl {
    public static void registerParticle(SimpleParticleType particle, ParticleProvider<SimpleParticleType> provider) {
        Minecraft.getInstance().particleEngine.register(particle, provider);
    }

    public static void registerClientTick(Runnable task) {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) {
                task.run();
            }
        });
    }
}
