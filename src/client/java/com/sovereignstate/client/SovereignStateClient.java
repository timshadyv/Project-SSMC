package com.sovereignstate.client;

import com.sovereignstate.client.screen.DivisionScreen;
import com.sovereignstate.network.ModPackets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

public class SovereignStateClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {

		// ─── Division Info Screen ─────────────────────────────────────────────

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.OPEN_DIVISION_SCREEN, (client, handler, buf, responseSender) -> {

			boolean hasDivision = buf.readBoolean();

			if (!hasDivision) {
				client.execute(() -> MinecraftClient.getInstance().player
						.sendMessage(net.minecraft.text.Text.literal("§cYou are not in a division.")));
				return;
			}

			String divID        = buf.readString();
			String divName      = buf.readString();
			String govType      = buf.readString();
			String leaderUUID   = buf.readString();
			int population      = buf.readInt();
			int treasury        = buf.readInt();
			String capitalChunk = buf.readString();

			int lawCount = buf.readInt();
			List<String> laws = new ArrayList<>();
			for (int i = 0; i < lawCount; i++) laws.add(buf.readString());

			int allyCount = buf.readInt();
			List<String> allies = new ArrayList<>();
			for (int i = 0; i < allyCount; i++) allies.add(buf.readString());

			int enemyCount = buf.readInt();
			List<String> enemies = new ArrayList<>();
			for (int i = 0; i < enemyCount; i++) enemies.add(buf.readString());

			String overlord = buf.readString();

			int vassalCount = buf.readInt();
			List<String> vassals = new ArrayList<>();
			for (int i = 0; i < vassalCount; i++) vassals.add(buf.readString());

			String myRank = buf.readString();

			client.execute(() -> MinecraftClient.getInstance().setScreen(
					new DivisionScreen(divID, divName, govType, leaderUUID,
							population, treasury, capitalChunk,
							laws, allies, enemies, overlord, vassals, myRank)
			));
		});

		// ─── Diplomacy Screen (placeholder — uses DivisionScreen diplomacy tab) ──

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.OPEN_DIPLOMACY_SCREEN, (client, handler, buf, responseSender) -> {
			boolean has = buf.readBoolean();
			if (!has) return;

			String divID = buf.readString();

			int allyCount = buf.readInt();
			List<String> allies = new ArrayList<>();
			for (int i = 0; i < allyCount; i++) allies.add(buf.readString());

			int enemyCount = buf.readInt();
			List<String> enemies = new ArrayList<>();
			for (int i = 0; i < enemyCount; i++) enemies.add(buf.readString());

			// Incoming proposals — read and discard for now (full screen coming later)
			int ap = buf.readInt(); for (int i = 0; i < ap; i++) buf.readString();
			int pp = buf.readInt(); for (int i = 0; i < pp; i++) buf.readString();
			int vp = buf.readInt(); for (int i = 0; i < vp; i++) buf.readString();

			String overlord = buf.readString();

			int vassalCount = buf.readInt();
			List<String> vassals = new ArrayList<>();
			for (int i = 0; i < vassalCount; i++) vassals.add(buf.readString());

			// Open division screen on diplomacy tab
			client.execute(() -> {
				DivisionScreen screen = new DivisionScreen(
						divID, divID, "", "", 0, 0, "",
						new ArrayList<>(), allies, enemies, overlord, vassals, "");
				MinecraftClient.getInstance().setScreen(screen);
			});
		});

		// ─── Military Screen (placeholder) ───────────────────────────────────

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.OPEN_MILITARY_SCREEN, (client, handler, buf, responseSender) -> {
			boolean has = buf.readBoolean();
			if (!has) return;
			// Read and discard — dedicated screen coming in next session
			String divID = buf.readString();
			int count = buf.readInt();
			for (int i = 0; i < count; i++) {
				buf.readString(); buf.readString(); buf.readString();
				buf.readInt(); buf.readBoolean(); buf.readInt(); buf.readInt();
			}
			buf.readString();
			client.execute(() -> MinecraftClient.getInstance().player
					.sendMessage(net.minecraft.text.Text.literal("§eMilitary screen coming soon! Use §f/ss military list")));
		});

		// ─── Court Screen (placeholder) ───────────────────────────────────────

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.OPEN_COURT_SCREEN, (client, handler, buf, responseSender) -> {
			boolean has = buf.readBoolean();
			if (!has) return;
			// Read and discard — dedicated screen coming in next session
			String divID = buf.readString();
			int count = buf.readInt();
			for (int i = 0; i < count; i++) {
				buf.readString(); buf.readString(); buf.readString();
				buf.readString(); buf.readString(); buf.readString();
			}
			client.execute(() -> MinecraftClient.getInstance().player
					.sendMessage(net.minecraft.text.Text.literal("§eCourt screen coming soon! Use §f/ss court list")));
		});
	}
}