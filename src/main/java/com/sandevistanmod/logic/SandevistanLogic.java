package com.sandevistanmod.logic;

import com.sandevistanmod.SandevistanMod;
import com.sandevistanmod.data.SandevistanData;
import com.sandevistanmod.data.Tx;
import com.sandevistanmod.net.Net;
import com.sandevistanmod.net.StatePayload;
import com.sandevistanmod.registry.ModRegistries;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/** Toda a logica do Sandevistan no servidor: implante, Mente, tick rate, atributos e efeitos. */
public final class SandevistanLogic {
    private SandevistanLogic() {}

    /** Dano instantaneo ao implantar (6 = 3 coracoes) e vida minima exigida para nao morrer. */
    public static final float IMPLANT_DAMAGE = 6f;
    public static final int IMPLANT_HEARTS = 3;
    /** Tempo, em segundos reais, para confirmar o implante com o segundo clique. */
    public static final long CONFIRM_MS = 10_000L;
    /** Tempo minimo, em tempo real, entre ligar/desligar. */
    public static final long TOGGLE_MS = 2_000L;

    // ---- potencia do Sandevistan (tudo ajustavel aqui) ----
    /**
     * Efeito de Velocidade (o efeito de verdade, aparece no inventario): nivel = valor + 1, cada nivel soma 20%.
     * 54 -> nivel 55 -> movimento 12x (o jogo roda a 2 ticks, entao precisa ser grande, mas controlavel).
     */
    private static final int SPEED_AMP = 54;
    /** Pulo: soma na forca do pulo (padrao 0,42). 1,28 -> pulo de uns 3 blocos (com a gravidade reduzida). */
    private static final double JUMP_ADD = 1.28;
    /** Gravidade: soma (padrao 0,08, o maximo do jogo e 1,0). */
    private static final double GRAVITY_ADD = 0.6;
    /** Velocidade de ataque: soma (padrao 4) para bater a cada clique mesmo com o jogo lento. */
    private static final double ATTACK_SPEED_ADD = 100.0;
    /** Velocidade de quebrar blocos: multiplicador final. */
    private static final double BREAK_MULT = 12.0;
    /** Forca (Strength) e Pressa (Haste): nivel = valor + 1. */
    private static final int STRENGTH_AMP = 3;
    /** Resistencia IV (nivel = valor + 1) enquanto esta ligado. */
    private static final int RESISTANCE_AMP = 3;
    private static final int HASTE_AMP = 2;

    private record Boost(Holder<Attribute> attr, ResourceLocation id, double amount, AttributeModifier.Operation op) {}

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(SandevistanMod.MODID, path);
    }

    private static final Boost[] BOOSTS = {
            new Boost(Attributes.JUMP_STRENGTH, rl("jump"), JUMP_ADD, AttributeModifier.Operation.ADD_VALUE),
            new Boost(Attributes.GRAVITY, rl("gravity"), GRAVITY_ADD, AttributeModifier.Operation.ADD_VALUE),
            new Boost(Attributes.ATTACK_SPEED, rl("attack_speed"), ATTACK_SPEED_ADD,
                    AttributeModifier.Operation.ADD_VALUE),
            new Boost(Attributes.BLOCK_BREAK_SPEED, rl("break_speed"), BREAK_MULT - 1.0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
            // sem dano de queda enquanto esta ligado (os pulos e a gravidade sao absurdos)
            new Boost(Attributes.FALL_DAMAGE_MULTIPLIER, rl("no_fall"), -1.0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
    };

    private static final Map<UUID, Long> ARMED = new HashMap<>();
    private static final Map<UUID, Long> LAST_TOGGLE = new HashMap<>();
    private static final Map<UUID, Integer> LAST_CODE = new HashMap<>();
    private static final Map<UUID, Float> LAST_SENT = new HashMap<>();
    /** Tick rate aplicado por nos (20 = normal). */
    private static float appliedRate = 20f;

    // ---------- utilidades ----------
    private static void snd(ServerPlayer p, Supplier<SoundEvent> ev) {
        // som preso ao jogador: acompanha o jogador enquanto ele anda
        Holder<SoundEvent> holder = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(ev.get());
        p.serverLevel().getChunkSource().broadcastAndSend(p,
                new ClientboundSoundEntityPacket(holder, SoundSource.PLAYERS, p, 1.0f, 1.0f, p.getRandom().nextLong()));
    }

    private static void chat(ServerPlayer p, Component c) {
        p.sendSystemMessage(c);
    }

    private static void bar(ServerPlayer p, Component c) {
        p.displayClientMessage(c, true);
    }

    private static void give(ServerPlayer p, ItemStack s) {
        if (!p.getInventory().add(s)) p.drop(s, false);
    }

    private static void send(ServerPlayer p, SandevistanData d) {
        LAST_SENT.put(p.getUUID(), d.mind);
        Net.sync(p);
    }

    // ---------- implante ----------
    /** Chamado ao usar o item. Retorna true se o implante foi feito (o item e consumido). */
    public static boolean tryImplant(ServerPlayer p) {
        SandevistanData d = SandevistanData.get(p);
        if (d.implanted) {
            chat(p, Tx.t(ChatFormatting.RED, "already"));
            return false;
        }
        boolean creative = p.getAbilities().instabuild;
        if (!creative && p.getHealth() <= IMPLANT_DAMAGE + 1f) {
            chat(p, Tx.t(ChatFormatting.RED, "lowhp", IMPLANT_HEARTS));
            return false;
        }
        long now = System.currentTimeMillis();
        Long armed = ARMED.get(p.getUUID());
        if (armed == null || now - armed > CONFIRM_MS) {
            ARMED.put(p.getUUID(), now);
            chat(p, Tx.t(ChatFormatting.GOLD, "warn", IMPLANT_HEARTS, CONFIRM_MS / 1000));
            return false;
        }
        ARMED.remove(p.getUUID());
        p.hurt(p.damageSources().magic(), IMPLANT_DAMAGE);
        d.implanted = true;
        d.active = false;
        d.mind = SandevistanData.MAX_MIND;
        send(p, d);
        broadcastState(p, d);
        chat(p, Tx.t(ChatFormatting.GREEN, "implanted",
                Component.keybind("key.sandevistan.screen"), Component.keybind("key.sandevistan.toggle")));
        return true;
    }

    private static void removeImplant(ServerPlayer p, SandevistanData d) {
        if (!d.implanted) {
            chat(p, Tx.t(ChatFormatting.RED, "noimplant"));
            return;
        }
        if (d.active) {
            chat(p, Tx.t(ChatFormatting.RED, "remove.active"));
            return;
        }
        d.implanted = false;
        give(p, new ItemStack(ModRegistries.SANDEVISTAN_ITEM.get()));
        send(p, d);
        broadcastState(p, d);
        chat(p, Tx.t(ChatFormatting.YELLOW, "removed"));
    }

    // ---------- ligar / desligar ----------
    public static void handleAction(ServerPlayer p, int action) {
        SandevistanData d = SandevistanData.get(p);
        switch (action) {
            case 0 -> toggle(p, d);
            case 1 -> removeImplant(p, d);
            default -> { }
        }
    }

    private static void toggle(ServerPlayer p, SandevistanData d) {
        if (!d.implanted) {
            bar(p, Tx.t(ChatFormatting.RED, "noimplant"));
            return;
        }
        long now = System.currentTimeMillis();
        Long last = LAST_TOGGLE.get(p.getUUID());
        if (last != null && now - last < TOGGLE_MS) {
            bar(p, Tx.t(ChatFormatting.GRAY, "cooldown"));
            return;
        }
        if (d.active) {
            deactivate(p, d, true);
        } else {
            if (d.mind < SandevistanData.MIN_TO_START) {
                bar(p, Tx.t(ChatFormatting.RED, "nomind"));
                return;
            }
            activate(p, d);
        }
        LAST_TOGGLE.put(p.getUUID(), now);
    }

    private static void activate(ServerPlayer p, SandevistanData d) {
        d.active = true;
        applyBoost(p);
        snd(p, ModRegistries.SND_ON);
        updateTickRate(p.server);
        broadcastState(p, d);
        send(p, d);
    }

    private static void deactivate(ServerPlayer p, SandevistanData d, boolean sound) {
        d.active = false;
        removeBoost(p);
        p.fallDistance = 0;
        // para o jogador no lugar: sem isso ele sairia disparado com a velocidade que tinha
        p.setDeltaMovement(Vec3.ZERO);
        p.hurtMarked = true;
        if (sound) snd(p, ModRegistries.SND_OFF);
        updateTickRate(p.server);
        broadcastState(p, d);
        send(p, d);
    }

    /** A Mente acabou: desliga e o jogador sofre uma sobrecarga. */
    private static void overload(ServerPlayer p, SandevistanData d) {
        deactivate(p, d, true);
        p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
        p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 400, 1));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 1));
        chat(p, Tx.t(ChatFormatting.RED, "overload"));
    }

    // ---------- atributos e efeitos ----------
    private static void applyBoost(ServerPlayer p) {
        for (Boost b : BOOSTS) {
            AttributeInstance inst = p.getAttribute(b.attr());
            if (inst != null && !inst.hasModifier(b.id())) {
                inst.addTransientModifier(new AttributeModifier(b.id(), b.amount(), b.op()));
            }
        }
        ensure(p, MobEffects.MOVEMENT_SPEED, 100, 200, SPEED_AMP, true);
        ensure(p, MobEffects.DAMAGE_RESISTANCE, 100, 200, RESISTANCE_AMP, true);
        ensure(p, MobEffects.NIGHT_VISION, 300, 400, 0);
        ensure(p, MobEffects.DAMAGE_BOOST, 100, 200, STRENGTH_AMP);
        ensure(p, MobEffects.DIG_SPEED, 100, 200, HASTE_AMP);
    }

    private static void ensure(ServerPlayer p, Holder<net.minecraft.world.effect.MobEffect> eff, int minDur, int dur, int amp) {
        ensure(p, eff, minDur, dur, amp, false);
    }

    private static void ensure(ServerPlayer p, Holder<net.minecraft.world.effect.MobEffect> eff, int minDur, int dur,
                               int amp, boolean icon) {
        MobEffectInstance cur = p.getEffect(eff);
        if (cur == null || cur.getAmplifier() != amp || cur.getDuration() < minDur) {
            p.addEffect(new MobEffectInstance(eff, dur, amp, true, false, icon));
        }
    }

    private static void dropEffect(ServerPlayer p, Holder<net.minecraft.world.effect.MobEffect> eff) {
        MobEffectInstance cur = p.getEffect(eff);
        if (cur != null && cur.isAmbient()) p.removeEffect(eff);
    }

    private static void removeBoost(ServerPlayer p) {
        for (Boost b : BOOSTS) {
            AttributeInstance inst = p.getAttribute(b.attr());
            if (inst != null) inst.removeModifier(b.id());
        }
        dropEffect(p, MobEffects.MOVEMENT_SPEED);
        dropEffect(p, MobEffects.DAMAGE_RESISTANCE);
        dropEffect(p, MobEffects.NIGHT_VISION);
        dropEffect(p, MobEffects.DAMAGE_BOOST);
        dropEffect(p, MobEffects.DIG_SPEED);
    }

    // ---------- tick rate ----------
    /**
     * Enquanto algum jogador estiver com o Sandevistan ligado, o jogo inteiro roda a 2 ticks por segundo,
     * exatamente como "/tick rate 2". Ao desligar, volta a 20.
     */
    public static void updateTickRate(MinecraftServer server) {
        boolean any = false;
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
            if (sp.isAlive() && SandevistanData.get(sp).active) {
                any = true;
                break;
            }
        }
        float want = any ? SandevistanData.SLOW_TICK_RATE : 20f;
        if (want != appliedRate) {
            appliedRate = want;
            server.getCommands().performPrefixedCommand(
                    server.createCommandSourceStack().withSuppressedOutput(), "tick rate " + (int) want);
        }
    }

    // ---------- estado visivel para todos ----------
    private static void broadcastState(ServerPlayer p, SandevistanData d) {
        int code = d.stateCode();
        if (LAST_CODE.getOrDefault(p.getUUID(), -1) != code) {
            LAST_CODE.put(p.getUUID(), code);
            PacketDistributor.sendToAllPlayers(new StatePayload(p.getUUID(), code));
        }
    }

    public static void sendStatesTo(ServerPlayer joiner) {
        for (Map.Entry<UUID, Integer> en : LAST_CODE.entrySet()) {
            if (en.getValue() > 0 && !en.getKey().equals(joiner.getUUID())) {
                PacketDistributor.sendToPlayer(joiner, new StatePayload(en.getKey(), en.getValue()));
            }
        }
    }

    // ---------- tick do jogador ----------
    public static void tick(ServerPlayer p) {
        SandevistanData d = SandevistanData.get(p);
        broadcastState(p, d);
        if (!d.implanted) {
            if (d.active) deactivate(p, d, false);
            return;
        }
        // com o jogo mais lento cada tick dura mais: divide pelo tick rate para o tempo real continuar o mesmo
        float rate = Math.max(1f, appliedRate);
        if (d.active) {
            d.mind -= (SandevistanData.MAX_MIND / SandevistanData.USE_SECONDS) / rate;
            if (d.mind <= 0f) {
                d.mind = 0f;
                overload(p, d);
                return;
            }
            applyBoost(p);
        } else if (d.mind < SandevistanData.MAX_MIND) {
            d.mind = Math.min(SandevistanData.MAX_MIND,
                    d.mind + (SandevistanData.MAX_MIND / SandevistanData.REGEN_SECONDS) / rate);
        }
        Float last = LAST_SENT.get(p.getUUID());
        if (last == null || Math.abs(d.mind - last) >= 1f || (d.mind >= SandevistanData.MAX_MIND && last < d.mind)) {
            send(p, d);
        }
    }

    // ---------- morte, login, saida ----------
    public static void resetOnDeath(ServerPlayer p) {
        SandevistanData d = SandevistanData.get(p);
        d.active = false;
        d.mind = SandevistanData.MAX_MIND;
        removeBoost(p);
        ARMED.remove(p.getUUID());
    }

    public static void onRespawn(ServerPlayer p) {
        SandevistanData d = SandevistanData.get(p);
        d.active = false;
        removeBoost(p);
        LAST_CODE.remove(p.getUUID());
        broadcastState(p, d);
        send(p, d);
    }

    public static void onLogin(ServerPlayer p) {
        SandevistanData d = SandevistanData.get(p);
        // os atributos temporarios nao sao salvos: se saiu ligado, volta desligado
        d.active = false;
        LAST_CODE.remove(p.getUUID());
        sendStatesTo(p);
        broadcastState(p, d);
        send(p, d);
    }

    public static void onLogout(ServerPlayer p) {
        SandevistanData d = SandevistanData.get(p);
        if (d.active) {
            d.active = false;
            removeBoost(p);
        }
        updateTickRate(p.server);
        LAST_CODE.remove(p.getUUID());
        LAST_SENT.remove(p.getUUID());
        LAST_TOGGLE.remove(p.getUUID());
        ARMED.remove(p.getUUID());
        PacketDistributor.sendToAllPlayers(new StatePayload(p.getUUID(), 0));
    }

    public static void clearAll() {
        ARMED.clear();
        LAST_TOGGLE.clear();
        LAST_CODE.clear();
        LAST_SENT.clear();
        appliedRate = 20f;
    }
}
