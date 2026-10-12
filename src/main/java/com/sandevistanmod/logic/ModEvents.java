package com.sandevistanmod.logic;

import com.sandevistanmod.SandevistanMod;
import com.sandevistanmod.data.SandevistanData;
import com.sandevistanmod.net.Net;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = SandevistanMod.MODID)
public final class ModEvents {
    private ModEvents() {}

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post e) {
        if (e.getEntity() instanceof ServerPlayer sp) SandevistanLogic.tick(sp);
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post e) {
        SandevistanLogic.updateTickRate(e.getServer());
    }

    /**
     * Com o Sandevistan o jogo roda a 2 ticks e o "tempo de invulnerabilidade" do alvo duraria varios segundos:
     * zera, para bater rapido continuar funcionando como no tempo normal.
     */
    @SubscribeEvent
    public static void incoming(LivingIncomingDamageEvent e) {
        if (e.getSource().getEntity() instanceof ServerPlayer sp
                && e.getEntity() != sp
                && SandevistanData.get(sp).active) {
            e.getEntity().invulnerableTime = 0;
        }
    }

    @SubscribeEvent
    public static void death(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) SandevistanLogic.resetOnDeath(sp);
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) SandevistanLogic.onLogin(sp);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) SandevistanLogic.onLogout(sp);
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) SandevistanLogic.onRespawn(sp);
    }

    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) Net.sync(sp);
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent e) {
        SandevistanLogic.clearAll();
    }
}
