package com.sovereignstate.client.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

public class CourtScreen extends Screen {

    // ─── Data passed from packet ──────────────────────────────────────────────
    private final String divID;
    private final List<CaseEntry> cases;

    // ─── Scroll state ─────────────────────────────────────────────────────────
    private int scrollOffset = 0;
    private static final int VISIBLE_ROWS = 5;

    // ─── Inner record for case data ───────────────────────────────────────────
    public record CaseEntry(String id, String status, String chargesSummary,
                            String defendantUUID, String plaintiffUUID, String verdict) {}

    public CourtScreen(String divID, List<CaseEntry> cases) {
        super(Text.literal("Court"));
        this.divID = divID;
        this.cases = cases;
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    protected void init() {
        super.init();
        int cx = width / 2;
        int panelBottom = height / 2 + 90;

        // Scroll buttons
        if (cases.size() > VISIBLE_ROWS) {
            addDrawableChild(ButtonWidget.builder(Text.literal("▲"), b -> {
                if (scrollOffset > 0) scrollOffset--;
            }).dimensions(cx + 120, height / 2 - 30, 16, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("▼"), b -> {
                if (scrollOffset < cases.size() - VISIBLE_ROWS) scrollOffset++;
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
        ctx.drawCenteredTextWithShadow(textRenderer, "§6§l⚖ Court", cx - 5, panelTop + 26, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, "§7Division: §f" + divID, cx - 5, panelTop + 38, 0xAAAAAA);

        int lineY = panelTop + 54;

        // Divider
        ctx.fill(panelLeft + 8, lineY, panelRight - 8, lineY + 1, 0xFF444444);
        lineY += 6;

        // Case list
        if (cases.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    "§7No open cases.", cx - 5, lineY + 10, 0xAAAAAA);
            ctx.drawCenteredTextWithShadow(textRenderer,
                    "§7Use §f/ss court file§7 to open a case.", cx - 5, lineY + 22, 0xAAAAAA);
        } else {
            ctx.drawTextWithShadow(textRenderer, "§eOpen Cases:", panelLeft + 10, lineY, 0xFFFFFF);
            lineY += 12;

            int end = Math.min(scrollOffset + VISIBLE_ROWS, cases.size());
            for (int i = scrollOffset; i < end; i++) {
                CaseEntry c = cases.get(i);

                // Status colour
                String statusColour = switch (c.status().toUpperCase()) {
                    case "OPEN"     -> "§e";
                    case "PENDING"  -> "§6";
                    case "CLOSED"   -> "§7";
                    case "GUILTY"   -> "§c";
                    case "INNOCENT" -> "§a";
                    default         -> "§f";
                };

                // Verdict display
                String verdictLine = c.verdict().isEmpty() ? "§7Awaiting verdict"
                        : "§fVerdict: " + statusColour + c.verdict();

                // Row hover highlight
                if (mouseX >= panelLeft + 8 && mouseX <= panelRight - 8
                        && mouseY >= lineY - 1 && mouseY <= lineY + 21) {
                    ctx.fill(panelLeft + 8, lineY - 1, panelRight - 8, lineY + 21, 0x33FFFFFF);
                }

                // Row: status + charges
                ctx.drawTextWithShadow(textRenderer,
                        statusColour + "● §f" + c.chargesSummary(),
                        panelLeft + 12, lineY, 0xFFFFFF);

                // Row: defendant / plaintiff
                ctx.drawTextWithShadow(textRenderer,
                        "§7Def: §f" + truncateUUID(c.defendantUUID()) +
                                "  §7Pla: §f" + truncateUUID(c.plaintiffUUID()),
                        panelLeft + 20, lineY + 10, 0xAAAAAA);

                // Row: verdict + ID
                ctx.drawTextWithShadow(textRenderer,
                        verdictLine + "  §8ID: " + c.id(),
                        panelLeft + 20, lineY + 20, 0xAAAAAA);

                lineY += 30;
            }

            // Scroll indicator
            if (cases.size() > VISIBLE_ROWS) {
                ctx.drawTextWithShadow(textRenderer,
                        "§7" + (scrollOffset + 1) + "–" + end + " of " + cases.size(),
                        cx + 90, panelTop + 54, 0xAAAAAA);
            }
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (amount < 0 && scrollOffset < cases.size() - VISIBLE_ROWS) scrollOffset++;
        if (amount > 0 && scrollOffset > 0) scrollOffset--;
        return true;
    }

    // Shorten UUID to last 8 chars so it fits on screen
    private String truncateUUID(String uuid) {
        if (uuid == null || uuid.isEmpty()) return "N/A";
        return uuid.length() > 8 ? "..." + uuid.substring(uuid.length() - 8) : uuid;
    }
}