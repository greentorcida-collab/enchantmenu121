package com.enchantmenu;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class EnchantMenu implements ModInitializer {
    private static int parseLevel(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(ApplyPayload.ID, ApplyPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ApplyPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            ItemStack stack = player.getMainHandStack();
            if (stack.isEmpty()) {
                return;
            }

            Registry<Enchantment> registry =
                player.getWorld().getRegistryManager().get(RegistryKeys.ENCHANTMENT);
            ItemEnchantmentsComponent.Builder builder =
                new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);

            String data = payload.data();
            if (!data.isEmpty()) {
                for (String part : data.split(";")) {
                    String[] pair = part.split("=");
                    if (pair.length != 2) {
                        continue;
                    }
                    Identifier id = Identifier.tryParse(pair[0]);
                    int level = parseLevel(pair[1]);
                    if (id == null || level <= 0) {
                        continue;
                    }
                    registry.getEntry(RegistryKey.of(RegistryKeys.ENCHANTMENT, id)).ifPresent(entry -> {
                        int capped = Math.min(level, entry.value().getMaxLevel());
                        builder.set(entry, capped);
                    });
                }
            }
            EnchantmentHelper.set(stack, builder.build());
        });
    }
}
