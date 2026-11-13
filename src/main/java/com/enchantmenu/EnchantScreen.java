package com.enchantmenu;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;

public class EnchantScreen extends Screen {
    private static final int PER_PAGE = 8;

    private final List<RegistryEntry<Enchantment>> all = new ArrayList<>();
    private final Map<String, Integer> levels = new LinkedHashMap<>();
    private int page = 0;

    public EnchantScreen() {
        super(Text.literal("Enchant"));
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world != null) {
            mc.world.getRegistryManager().get(RegistryKeys.ENCHANTMENT)
                .streamEntries().forEach(all::add);
        }
        if (mc.player != null) {
            ItemStack stack = mc.player.getMainHandStack();
            if (!stack.isEmpty()) {
                ItemEnchantmentsComponent current = EnchantmentHelper.getEnchantments(stack);
                for (RegistryEntry<Enchantment> entry : current.getEnchantments()) {
                    levels.put(idOf(entry), current.getLevel(entry));
                }
            }
        }
    }

    private static String idOf(RegistryEntry<Enchantment> entry) {
        return entry.getKey().map(key -> key.getValue().toString()).orElse("");
    }

    private int pageCount() {
        return Math.max(1, (all.size() + PER_PAGE - 1) / PER_PAGE);
    }

    private Text label(RegistryEntry<Enchantment> entry) {
        int level = levels.getOrDefault(idOf(entry), 0);
        int max = entry.value().getMaxLevel();
        return entry.value().description().copy().append(Text.literal(": " + level + "/" + max));
    }

    private void changePage(int delta) {
        page = Math.floorMod(page + delta, pageCount());
        this.clearAndInit();
    }

    private void apply() {
        MinecraftClient mc = this.client;
        if (mc == null || mc.player == null || mc.player.getMainHandStack().isEmpty()) {
            return;
        }
        StringBuilder data = new StringBuilder();
        for (Map.Entry<String, Integer> entry : levels.entrySet()) {
            if (entry.getValue() > 0 && !entry.getKey().isEmpty()) {
                if (data.length() > 0) {
                    data.append(';');
                }
                data.append(entry.getKey()).append('=').append(entry.getValue());
            }
        }
        ClientPlayNetworking.send(new ApplyPayload(data.toString()));
        mc.setScreen(null);
    }

    @Override
    protected void init() {
        int colWidth = 150;
        int center = this.width / 2;
        int left = center - colWidth - 5;
        int right = center + 5;
        int start = page * PER_PAGE;

        for (int i = 0; i < PER_PAGE; i++) {
            int index = start + i;
            if (index >= all.size()) {
                break;
            }
            RegistryEntry<Enchantment> entry = all.get(index);
            int x = (i % 2 == 0) ? left : right;
            int y = 36 + (i / 2) * 24;
            this.addDrawableChild(ButtonWidget.builder(label(entry), button -> {
                int max = entry.value().getMaxLevel();
                int next = (levels.getOrDefault(idOf(entry), 0) + 1) % (max + 1);
                levels.put(idOf(entry), next);
                button.setMessage(label(entry));
            }).dimensions(x, y, colWidth, 20).build());
        }

        int bottom = this.height - 28;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("<"), button -> changePage(-1))
            .dimensions(center - 155, bottom, 30, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal(">"), button -> changePage(1))
            .dimensions(center - 120, bottom, 30, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Terapkan"), button -> apply())
            .dimensions(center - 80, bottom, 120, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Tutup"), button -> this.client.setScreen(null))
            .dimensions(center + 45, bottom, 110, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        ItemStack held = this.client != null && this.client.player != null
            ? this.client.player.getMainHandStack() : ItemStack.EMPTY;
        Text header = held.isEmpty() ? Text.literal("Pegang alat di tangan dulu") : held.getName();
        context.drawCenteredTextWithShadow(this.textRenderer, header, this.width / 2, 10, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer,
            Text.literal("Halaman " + (page + 1) + "/" + pageCount()), this.width / 2, 22, 0xAAAAAA);
    }
}
