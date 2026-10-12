package com.sandevistanmod.registry;

import com.sandevistanmod.SandevistanMod;
import com.sandevistanmod.data.SandevistanData;
import com.sandevistanmod.logic.SandevistanItem;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModRegistries {
    private ModRegistries() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, SandevistanMod.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, SandevistanMod.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SandevistanMod.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SandevistanMod.MODID);

    public static final Supplier<AttachmentType<SandevistanData>> SANDEVISTAN =
            ATTACHMENTS.register("sandevistan",
                    () -> AttachmentType.serializable(() -> new SandevistanData()).copyOnDeath().build());

    private static Supplier<SoundEvent> sound(String name) {
        return SOUNDS.register(name,
                () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(SandevistanMod.MODID, name)));
    }

    public static final Supplier<SoundEvent> SND_ON = sound("sandevistan_on");
    public static final Supplier<SoundEvent> SND_OFF = sound("sandevistan_off");

    public static final DeferredItem<SandevistanItem> SANDEVISTAN_ITEM = ITEMS.register("sandevistan",
            () -> new SandevistanItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final DeferredItem<Item> CHIP = ITEMS.registerSimpleItem("chip", new Item.Properties());
    public static final DeferredItem<Item> NEURALINK = ITEMS.registerSimpleItem("neuralink", new Item.Properties());
    public static final DeferredItem<Item> ESPINHA = ITEMS.registerSimpleItem("espinha", new Item.Properties());
    public static final DeferredItem<Item> ACELERADOR = ITEMS.registerSimpleItem("acelerador", new Item.Properties());

    public static final Supplier<CreativeModeTab> TAB = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.sandevistan"))
                    .icon(() -> new ItemStack(SANDEVISTAN_ITEM.get()))
                    .displayItems((params, out) -> {
                        out.accept(CHIP.get());
                        out.accept(NEURALINK.get());
                        out.accept(ESPINHA.get());
                        out.accept(ACELERADOR.get());
                        out.accept(SANDEVISTAN_ITEM.get());
                    })
                    .build());
}
