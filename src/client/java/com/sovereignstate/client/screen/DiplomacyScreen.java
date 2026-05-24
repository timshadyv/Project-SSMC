package com.sovereignstate.client.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

public class DiplomacyScreen extends Screen {

    // ─── Data passed from packet ──────────────────────────────────────────────
    private final String divID;
    private final List<String> allies;
    private final List<String> enemies;
    private final List<String> incomingAlliances;
    private final List<String> incomingPeace;
    private final List<String> incomingVassal;
    private final String overlord;
    private final List<String> vassals;

    // ─── Tab state ────────────────────────────────────────────────────────────
    private int tab = 0; // 0=Relations 1=Proposals 2=Vassalage

    public DiplomacyScreen(String divID, List<String> allies, List<String> enemies,
                           List<String> incomingAlliances, List<String> incomingPeace,
                           List<String> incomingVassal, String overlord, List<String> vassals) {
        super(Text.literal("Diplomacy"));
        this.divID            = divID;
        this.allies           = allies;
        this.enemies          = enemies;
        this.incomingAlliances = incomingAlliances;
        this.incomingPeace    = incomingPeace;
        this.incomingVassal   = incomingVassal;
        this.overlord         = overlord;
        this.vassals          = vassals;
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    protected void init() {
        super.init();
        int cx = width / 2;
        int panelTop    = height / 2 - 90;
        int panelBottom = height / 2 + 90;

        // Tab buttons
        addDrawableChild(ButtonWidget.builder(Text.literal("Relations"), b -> { tab = 0; clearAndInit(); })
                .dimensions(cx - 110, panelTop + 22, 70, 14).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Proposals"), b -> { tab = 1; clearAndInit(); })
                .dimensions(cx - 35, panelTop + 22, 70, 14).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Vassalage"), b -> { tab = 2; clearAndInit(); })
                .dimensions(cx + 40, panelTop + 22, 70, 14).build());

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
        int panelRight = cx + 150;

        // Panel background + border
        ctx.fill(panelLeft, panelTop + 20, panelRight, cy + 90, 0xCC111111);
        ctx.drawBorder(panelLeft, panelTop + 20, 300, 170, 0xFF555555);

        // Header
        ctx.drawCenteredTextWithShadow(textRenderer, "§6§l🌐 Diplomacy", cx, panelTop + 6, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, "§7" + divID, cx, panelTop + 16, 0xAAAAAA);

        // Active tab highlight
        String[] tabNames = {"Relations", "Proposals", "Vassalage"};
        ctx.drawCenteredTextWithShadow(textRenderer,
                "§e[ " + tabNames[tab] + " ]", cx, panelTop + 39, 0xFFFFAA);

        // Proposal notification badge
        int pending = incomingAlliances.size() + incomingPeace.size() + incomingVassal.size();
        if (pending > 0) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    "§c" + pending + " pending proposal" + (pending == 1 ? "" : "s"),
                    cx, panelTop + 50, 0xFF6666);
        }

        int lineY = panelTop + (pending > 0 ? 62 : 54);

        // Divider
        ctx.fill(panelLeft + 8, lineY - 2, panelRight - 8, lineY - 1, 0xFF444444);

        switch (tab) {
            case 0 -> renderRelations(ctx, panelLeft + 10, lineY);
            case 1 -> renderProposals(ctx, panelLeft + 10, lineY);
            case 2 -> renderVassalage(ctx, panelLeft + 10, lineY);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    // ─── Tab: Relations ───────────────────────────────────────────────────────

    private void renderRelations(DrawContext ctx, int x, int y) {
        // Allies
        ctx.drawTextWithShadow(textRenderer, "§aAllies §7(" + allies.size() + ")", x, y, 0xFFFFFF);
        if (allies.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, "§7None", x + 8, y + 11, 0xAAAAAA);
        } else {
            for (int i = 0; i < Math.min(allies.size(), 5); i++)
                ctx.drawTextWithShadow(textRenderer, "§a✦ §f" + allies.get(i), x + 8, y + 11 + i * 11, 0xFFFFFF);
            if (allies.size() > 5)
                ctx.drawTextWithShadow(textRenderer, "§7...+" + (allies.size() - 5) + " more", x + 8, y + 11 + 5 * 11, 0xAAAAAA);
        }

        int enemyY = y + 11 + Math.min(allies.isEmpty() ? 1 : allies.size(), 5) * 11 + 14;

        // Enemies
        ctx.drawTextWithShadow(textRenderer, "§cAt War §7(" + enemies.size() + ")", x, enemyY, 0xFFFFFF);
        if (enemies.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, "§7None", x + 8, enemyY + 11, 0xAAAAAA);
        } else {
            for (int i = 0; i < Math.min(enemies.size(), 4); i++)
                ctx.drawTextWithShadow(textRenderer, "§c⚔ §f" + enemies.get(i), x + 8, enemyY + 11 + i * 11, 0xFFFFFF);
            if (enemies.size() > 4)
                ctx.drawTextWithShadow(textRenderer, "§7...+" + (enemies.size() - 4) + " more", x + 8, enemyY + 11 + 4 * 11, 0xAAAAAA);
        }
    }

    // ─── Tab: Proposals ───────────────────────────────────────────────────────

    private void renderProposals(DrawContext ctx, int x, int y) {
        int lineY = y;

        // Alliance proposals
        ctx.drawTextWithShadow(textRenderer, "§aAlliance Proposals:", x, lineY, 0xFFFFFF);
        lineY += 11;
        if (incomingAlliances.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, "§7None", x + 8, lineY, 0xAAAAAA);
            lineY += 11;
        } else {
            for (String from : incomingAlliances) {
                ctx.drawTextWithShadow(textRenderer, "§a✦ §f" + from +
                        " §7— /ss diplomacy accept alliance " + from, x + 8, lineY, 0xFFFFFF);
                lineY += 11;
            }
        }

        lineY += 4;

        // Peace proposals
        ctx.drawTextWithShadow(textRenderer, "§ePeace Proposals:", x, lineY, 0xFFFFFF);
        lineY += 11;
        if (incomingPeace.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, "§7None", x + 8, lineY, 0xAAAAAA);
            lineY += 11;
        } else {
            for (String from : incomingPeace) {
                ctx.drawTextWithShadow(textRenderer, "§e🕊 §f" + from +
                        " §7— /ss diplomacy accept peace " + from, x + 8, lineY, 0xFFFFFF);
                lineY += 11;
            }
        }

        lineY += 4;

        // Vassal proposals
        ctx.drawTextWithShadow(textRenderer, "§6Vassal Proposals:", x, lineY, 0xFFFFFF);
        lineY += 11;
        if (incomingVassal.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, "§7None", x + 8, lineY, 0xAAAAAA);
        } else {
            for (String from : incomingVassal) {
                ctx.drawTextWithShadow(textRenderer, "§6👑 §f" + from +
                        " §7— /ss diplomacy accept vassal " + from, x + 8, lineY, 0xFFFFFF);
                lineY += 11;
            }
        }
    }

    // ─── Tab: Vassalage ───────────────────────────────────────────────────────

    private void renderVassalage(DrawContext ctx, int x, int y) {
        // Overlord
        ctx.drawTextWithShadow(textRenderer, "§eOverlord:", x, y, 0xFFFFFF);
        if (overlord == null || overlord.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, "§7Independent", x + 8, y + 11, 0xAAAAAA);
        } else {
            ctx.drawTextWithShadow(textRenderer, "§6👑 §f" + overlord, x + 8, y + 11, 0xFFFFFF);
            ctx.drawTextWithShadow(textRenderer, "§7/ss diplomacy independence §8to break free",
                    x + 8, y + 22, 0xAAAAAA);
        }

        int vassalY = y + 40;

        // Vassals
        ctx.drawTextWithShadow(textRenderer, "§eYour Vassals §7(" + vassals.size() + "):", x, vassalY, 0xFFFFFF);
        vassalY += 11;
        if (vassals.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, "§7None", x + 8, vassalY, 0xAAAAAA);
        } else {
            for (int i = 0; i < Math.min(vassals.size(), 6); i++) {
                ctx.drawTextWithShadow(textRenderer, "§6• §f" + vassals.get(i),
                        x + 8, vassalY + i * 11, 0xFFFFFF);
            }
            if (vassals.size() > 6)
                ctx.drawTextWithShadow(textRenderer, "§7...+" + (vassals.size() - 6) + " more",
                        x + 8, vassalY + 6 * 11, 0xAAAAAA);
        }
    }
}