package com.sandevistanmod.client;

import com.sandevistanmod.data.SandevistanData;
import com.sandevistanmod.data.Tx;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class ClientOverlay {
    private ClientOverlay() {}

    /** Tela com um tiquinho de verde enquanto o Sandevistan esta ligado (e piscando em vermelho com a Mente baixa). */
    public static void renderVision(GuiGraphics gg, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        SandevistanData d = ClientData.DATA;
        if (!d.implanted || !d.active) return;
        int w = gg.guiWidth();
        int h = gg.guiHeight();
        gg.fill(0, 0, w, h, 0x1C00FF40);
        if (d.mind < 20f) {
            double pulse = Math.sin(System.currentTimeMillis() / 150.0) * 0.5 + 0.5;
            int a = (int) (pulse * 0x30);
            gg.fill(0, 0, w, h, (a << 24) | 0xFF0000);
        }
    }

    private static String time(int seconds) {
        return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
    }

    public static int mindColor(float mind) {
        if (mind > 40f) return 0xFF39FF14;
        if (mind > 20f) return 0xFFFFCC22;
        return 0xFFFF3333;
    }

    public static void bar(GuiGraphics gg, int x, int y, int w, int h, float frac, int color) {
        gg.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF000000);
        gg.fill(x, y, x + w, y + h, 0xFF16201A);
        int fw = (int) (w * Math.max(0f, Math.min(1f, frac)));
        gg.fill(x, y, x + fw, y + h, color);
    }

    /** HUD: estado do Sandevistan e barra de Mente, no canto escolhido na aba J. */
    public static void renderHud(GuiGraphics gg, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        SandevistanData d = ClientData.DATA;
        if (!d.implanted) return;
        gg.pose().pushPose();
        gg.pose().translate(ClientLayout.x(gg.guiWidth()), ClientLayout.y(gg.guiHeight()), 0);
        gg.pose().scale(ClientLayout.SCALE, ClientLayout.SCALE, 1f);
        drawBlock(gg, mc.font, d);
        gg.pose().popPose();
    }

    private static void drawBlock(GuiGraphics gg, Font f, SandevistanData d) {
        MutableComponent title = Component.literal("[")
                .append(ModKeys.TOGGLE.getTranslatedKeyMessage())
                .append(Component.literal("] "))
                .append(Tx.t("hud.title"))
                .append(Component.literal(": "))
                .append(d.active ? Tx.t("hud.on", time(d.secondsLeft())) : Tx.t("hud.off"));
        gg.drawString(f, title, 0, 0, d.active ? 0x39FF14 : 0xAAAAAA);
        bar(gg, 0, 12, 110, 7, d.mind / SandevistanData.MAX_MIND, mindColor(d.mind));
        gg.drawString(f, Tx.t("hud.mind", (int) d.mind), 0, 22, d.mind < 20f ? 0xFF5555 : 0x88FFAA);
    }
}
