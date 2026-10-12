package com.sandevistanmod.data;

import com.sandevistanmod.registry.ModRegistries;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.util.INBTSerializable;

/** Estado do Sandevistan de um jogador (salvo no jogador e copiado ao morrer). */
public class SandevistanData implements INBTSerializable<CompoundTag> {
    public static final float MAX_MIND = 100f;
    /** Tempo, em segundos reais, que a barra de Mente cheia dura com o Sandevistan ligado. */
    public static final float USE_SECONDS = 60f;
    /** Tempo, em segundos reais, para a barra encher do zero (1 de Mente a cada 2 segundos) com o Sandevistan desligado. */
    public static final float REGEN_SECONDS = 200f;
    /** Mente minima para conseguir ligar o Sandevistan. */
    public static final float MIN_TO_START = 10f;
    /** Tick rate do jogo inteiro enquanto o Sandevistan esta ligado. */
    public static final int SLOW_TICK_RATE = 2;

    public boolean implanted;
    public boolean active;
    public float mind = MAX_MIND;

    public static SandevistanData get(Player p) {
        return p.getData(ModRegistries.SANDEVISTAN);
    }

    /** Segundos reais restantes de uso com a Mente atual. */
    public int secondsLeft() {
        return Math.max(0, (int) Math.ceil(mind / (MAX_MIND / USE_SECONDS)));
    }

    /** Codigo enviado aos outros jogadores: bit 1 implante nas costas, bit 2 ligado. */
    public int stateCode() {
        return (implanted ? 1 : 0) | (active && implanted ? 2 : 0);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag t = new CompoundTag();
        t.putBoolean("implanted", implanted);
        t.putBoolean("active", active);
        t.putFloat("mind", mind);
        return t;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag t) {
        implanted = t.getBoolean("implanted");
        active = t.getBoolean("active");
        mind = t.contains("mind") ? t.getFloat("mind") : MAX_MIND;
    }
}
