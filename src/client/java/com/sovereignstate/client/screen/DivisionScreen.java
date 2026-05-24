package com.sovereignstate.client.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

public class DivisionScreen extends Screen {

    // ─── Data passed from packet ──────────────────────────────────────────────
    private final String divID;
    private final String divName;
    private final String govType;
    private final String leaderUUID;
    private final int population;
    private final int treasury;
    private final String capitalChunk;
    private final List<String> laws;
    private final List<String> allies;
    private final List<String> enemies;
    private final String overlord;
    private final List<String> vassals;
    private final String myRank;

    // ─── UI state ─────────────────────────────────────────────────────────────
    private int tab = 0; // 0=Overview 1=Laws 2=Diplomacy 3=Military

    public DivisionScreen(String divID, String divName, String govType, String leaderUUID,
                          int population, int treasury, String capitalChunk,
                          List<String> laws, List<String> allies, List<String> enemies,
                          String overlord, List<String> vassals, String myRank) {
        super(Text.literal(divName));
        this.divID      = divID;
        this.divName    = divName;
        this.govType    = govType;
        this.leaderUUID = leaderUUID;
        this.population = population;
        this.treasury   = treasury;
        this.capitalChunk = capitalChunk;
        this.laws       = laws;
        this.allies     = allies;
        this.enemies    = enemies;
        this.overlord   = overlord;
        this.vassals    = vassals;
        this.myRank     = myRank;
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    protected void init() {
        super.init();
        int cx = width / 2;
        int panelTop = height / 2 - 90;

        // Tab buttons
        addDrawableChild(ButtonWidget.builder(Text.literal("Overview"), b -> { tab = 0; clearAndInit(); })
                .dimensions(cx - 140, panelTop, 68, 16).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Laws"), b -> { tab = 1; clearAndInit(); })
                .dimensions(cx - 68, panelTop, 68, 16).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Diplomacy"), b -> { tab = 2; clearAndInit(); })
                .dimensions(cx + 4, panelTop, 68, 16).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Military"), b -> { tab = 3; clearAndInit(); })
                .dimensions(cx + 76, panelTop, 68, 16).build());

        // Close button
        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), b -> close())
                .dimensions(cx - 30, height / 2 + 82, 60, 16).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx);

        int cx = width / 2;
        int cy = height / 2;
        int panelTop = cy - 90;
        int panelLeft = cx - 150;
        int panelRight = cx + 150;

        // Panel background
        ctx.fill(panelLeft, panelTop + 20, panelRight, cy + 90, 0xCC111111);
        ctx.drawBorder(panelLeft, panelTop + 20, 300, 170, 0xFF555555);

        // Division name header
        ctx.drawCenteredTextWithShadow(textRenderer, "§6§l" + divName, cx, panelTop + 26, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, "§7" + govType.replace("_", " "), cx, panelTop + 38, 0xAAAAAA);

        int lineY = panelTop + 54;

        switch (tab) {
            case 0 -> renderOverview(ctx, cx, lineY);
            case 1 -> renderLaws(ctx, panelLeft + 10, lineY);
            case 2 -> renderDiplomacy(ctx, panelLeft + 10, lineY);
            case 3 -> renderMilitary(ctx, panelLeft + 10, lineY);
        }

        // Active tab highlight
        String[] tabNames = {"Overview", "Laws", "Diplomacy", "Military"};
        ctx.drawCenteredTextWithShadow(textRenderer,
                "§e[ " + tabNames[tab] + " ]", cx, panelTop + 8, 0xFFFFAA);

        super.render(ctx, mouseX, mouseY, delta);
    }

    // ─── Tab: Overview ────────────────────────────────────────────────────────

    private void renderOverview(DrawContext ctx, int cx, int y) {
        ctx.drawCenteredTextWithShadow(textRenderer, "§ePopulation: §f" + population, cx, y, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, "§eTreasury: §f" + treasury, cx, y + 12, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, "§eCapital: §f" + (capitalChunk.isEmpty() ? "Not set" : capitalChunk), cx, y + 24, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, "§eMy Rank: §f" + myRank, cx, y + 36, 0xFFFFFF);

        if (!overlord.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, "§cVassal of: §f" + overlord, cx, y + 52, 0xFFFFFF);
        }
        if (!vassals.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, "§aVassals: §f" + String.join(", ", vassals), cx, y + 64, 0xFFFFFF);
        }
    }

    // ─── Tab: Laws ────────────────────────────────────────────────────────────

    private void renderLaws(DrawContext ctx, int x, int y) {
        if (laws.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, "§7No active laws.", x, y, 0xFFFFFF);
            return;
        }
        ctx.drawTextWithShadow(textRenderer, "§eActive Laws:", x, y, 0xFFFFFF);
        int i = 0;
        for (String law : laws) {
            if (i >= 10) { ctx.drawTextWithShadow(textRenderer, "§7...and more", x, y + 12 + i * 11, 0xAAAAAA); break; }
            ctx.drawTextWithShadow(textRenderer, "§a✓ §f" + law.replace("_", " "), x, y + 12 + i * 11, 0xFFFFFF);
            i++;
        }
    }

    // ─── Tab: Diplomacy ───────────────────────────────────────────────────────

    private void renderDiplomacy(DrawContext ctx, int x, int y) {
        ctx.drawTextWithShadow(textRenderer, "§aAllies:", x, y, 0xFFFFFF);
        if (allies.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, "§7None", x + 8, y + 11, 0xAAAAAA);
        } else {
            for (int i = 0; i < Math.min(allies.size(), 4); i++)
                ctx.drawTextWithShadow(textRenderer, "§f" + allies.get(i), x + 8, y + 11 + i * 11, 0xFFFFFF);
        }

        int enemyY = y + 11 + Math.min(allies.isEmpty() ? 1 : allies.size(), 4) * 11 + 6;
        ctx.drawTextWithShadow(textRenderer, "§cAt War:", x, enemyY, 0xFFFFFF);
        if (enemies.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, "§7None", x + 8, enemyY + 11, 0xAAAAAA);
        } else {
            for (int i = 0; i < Math.min(enemies.size(), 4); i++)
                ctx.drawTextWithShadow(textRenderer, "§f" + enemies.get(i), x + 8, enemyY + 11 + i * 11, 0xFFFFFF);
        }
    }

    // ─── Tab: Military ────────────────────────────────────────────────────────

    private void renderMilitary(DrawContext ctx, int x, int y) {
        ctx.drawTextWithShadow(textRenderer, "§eYour rank: §f" + myRank, x, y, 0xFFFFFF);
        ctx.drawTextWithShadow(textRenderer, "§7Use /ss military list for armies.", x, y + 14, 0xAAAAAA);
    }
}