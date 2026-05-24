package com.sovereignstate.client.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

public class MilitaryScreen extends Screen {

    // ─── Data passed from packet ──────────────────────────────────────────────
    private final String divID;
    private final List<ArmyEntry> armies;
    private final String myRank;

    // ─── Scroll state ─────────────────────────────────────────────────────────
    private int scrollOffset = 0;
    private static final int VISIBLE_ROWS = 7;

    // ─── Inner record for army data ───────────────────────────────────────────
    public record ArmyEntry(String id, String name, String unitType,
                            int unitCount, boolean isDeployed,
                            int chunkX, int chunkZ) {}

    public MilitaryScreen(String divID, List<ArmyEntry> armies, String myRank) {
        super(Text.literal("Military"));
        this.divID   = divID;
        this.armies  = armies;
        this.myRank  = myRank;
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    protected void init() {
        super.init();
        int cx = width / 2;
        int panelBottom = height / 2 + 90;

        // Scroll buttons
        if (armies.size() > VISIBLE_ROWS) {
            addDrawableChild(ButtonWidget.builder(Text.literal("▲"), b -> {
                if (scrollOffset > 0) scrollOffset--;
            }).dimensions(cx + 120, height / 2 - 30, 16, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("▼"), b -> {
                if (scrollOffset < armies.size() - VISIBLE_ROWS) scrollOffset++;
            }).dimensions(cx + 120, height / 2 - 10, 16, 16).build());
        }

        // Close button
        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), b -> close())
                .dimensions(cx - 30, panelBottom - 18, 60, 16).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx);

        int cx = width / 2;
        int cy = height / 2;
        int panelTop   = cy - 90;
        int panelLeft  = cx - 150;
        int panelRight = cx + 140;

        // Panel background + border
        ctx.fill(panelLeft, panelTop + 20, panelRight, cy + 90, 0xCC111111);
        ctx.drawBorder(panelLeft, panelTop + 20, panelRight - panelLeft, 170, 0xFF555555);

        // Header
        ctx.drawCenteredTextWithShadow(textRenderer, "§6§l⚔ Military", cx - 5, panelTop + 26, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, "§7Division: §f" + divID, cx - 5, panelTop + 38, 0xAAAAAA);

        int lineY = panelTop + 54;

        // Rank bar
        ctx.drawTextWithShadow(textRenderer, "§eYour Rank: §f" + myRank, panelLeft + 10, lineY, 0xFFFFFF);
        lineY += 14;

        // Divider
        ctx.fill(panelLeft + 8, lineY, panelRight - 8, lineY + 1, 0xFF444444);
        lineY += 6;

        // Army list
        if (armies.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    "§7No armies raised yet.", cx - 5, lineY + 10, 0xAAAAAA);
            ctx.drawCenteredTextWithShadow(textRenderer,
                    "§7Use §f/ss military raise§7 to create one.", cx - 5, lineY + 22, 0xAAAAAA);
        } else {
            ctx.drawTextWithShadow(textRenderer, "§eArmies:", panelLeft + 10, lineY, 0xFFFFFF);
            lineY += 12;

            int end = Math.min(scrollOffset + VISIBLE_ROWS, armies.size());
            for (int i = scrollOffset; i < end; i++) {
                ArmyEntry a = armies.get(i);
                String status = a.isDeployed() ? "§aDeployed" : "§7Garrison";
                String unitIcon = switch (a.unitType()) {
                    case "INFANTRY" -> "🗡";
                    case "ARCHER"   -> "🏹";
                    case "CAVALRY"  -> "🐴";
                    default         -> "•";
                };

                // Row background on hover
                if (mouseX >= panelLeft + 8 && mouseX <= panelRight - 8
                        && mouseY >= lineY - 1 && mouseY <= lineY + 10) {
                    ctx.fill(panelLeft + 8, lineY - 1, panelRight - 8, lineY + 10, 0x33FFFFFF);
                }

                ctx.drawTextWithShadow(textRenderer,
                        "§f" + unitIcon + " " + a.name() +
                                " §7[" + a.unitType() + "]" +
                                " §fUnits: §e" + a.unitCount() +
                                "  " + status,
                        panelLeft + 12, lineY, 0xFFFFFF);

                ctx.drawTextWithShadow(textRenderer,
                        "§7Chunk: " + a.chunkX() + ", " + a.chunkZ() +
                                "  §8ID: " + a.id(),
                        panelLeft + 20, lineY + 10, 0xAAAAAA);

                lineY += 22;
            }

            // Scroll indicator
            if (armies.size() > VISIBLE_ROWS) {
                ctx.drawTextWithShadow(textRenderer,
                        "§7" + (scrollOffset + 1) + "–" + end + " of " + armies.size(),
                        cx + 90, panelTop + 54, 0xAAAAAA);
            }
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (amount < 0 && scrollOffset < armies.size() - VISIBLE_ROWS) scrollOffset++;
        if (amount > 0 && scrollOffset > 0) scrollOffset--;
        return true;
    }
}