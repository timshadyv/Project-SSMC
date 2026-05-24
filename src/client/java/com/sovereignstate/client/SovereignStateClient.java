package com.sovereignstate.client;

import com.sovereignstate.client.screen.DivisionScreen;
import com.sovereignstate.client.renderer.GuardEntityRenderer;
import com.sovereignstate.registry.ModEntities;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
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
		EntityRendererRegistry.register(ModEntities.GUARD, GuardEntityRenderer::new);
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

		// ─── Diplomacy Screen ─────────────────────────────────────────────────

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.OPEN_DIPLOMACY_SCREEN, (client, handler, buf, responseSender) -> {
			boolean has = buf.readBoolean();
			if (!has) {
				client.execute(() -> MinecraftClient.getInstance().player
						.sendMessage(net.minecraft.text.Text.literal("§cYou are not in a division.")));
				return;
			}

			String divID = buf.readString();

			int allyCount = buf.readInt();
			List<String> allies = new ArrayList<>();
			for (int i = 0; i < allyCount; i++) allies.add(buf.readString());

			int enemyCount = buf.readInt();
			List<String> enemies = new ArrayList<>();
			for (int i = 0; i < enemyCount; i++) enemies.add(buf.readString());

			int ap = buf.readInt();
			List<String> incomingAlliances = new ArrayList<>();
			for (int i = 0; i < ap; i++) incomingAlliances.add(buf.readString());

			int pp = buf.readInt();
			List<String> incomingPeace = new ArrayList<>();
			for (int i = 0; i < pp; i++) incomingPeace.add(buf.readString());

			int vp = buf.readInt();
			List<String> incomingVassal = new ArrayList<>();
			for (int i = 0; i < vp; i++) incomingVassal.add(buf.readString());

			String overlord = buf.readString();

			int vassalCount = buf.readInt();
			List<String> vassals = new ArrayList<>();
			for (int i = 0; i < vassalCount; i++) vassals.add(buf.readString());

			client.execute(() -> MinecraftClient.getInstance().setScreen(
					new com.sovereignstate.client.screen.DiplomacyScreen(
							divID, allies, enemies, incomingAlliances,
							incomingPeace, incomingVassal, overlord, vassals)));
		});

		// ─── Military Screen ──────────────────────────────────────────────────

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.OPEN_MILITARY_SCREEN, (client, handler, buf, responseSender) -> {
			boolean has = buf.readBoolean();
			if (!has) {
				client.execute(() -> MinecraftClient.getInstance().player
						.sendMessage(net.minecraft.text.Text.literal("§cYou are not in a division.")));
				return;
			}

			String divID = buf.readString();

			int count = buf.readInt();
			java.util.List<com.sovereignstate.client.screen.MilitaryScreen.ArmyEntry> armies = new java.util.ArrayList<>();
			for (int i = 0; i < count; i++) {
				String id        = buf.readString();
				String name      = buf.readString();
				String unitType  = buf.readString();
				int unitCount    = buf.readInt();
				boolean deployed = buf.readBoolean();
				int chunkX       = buf.readInt();
				int chunkZ       = buf.readInt();
				armies.add(new com.sovereignstate.client.screen.MilitaryScreen.ArmyEntry(
						id, name, unitType, unitCount, deployed, chunkX, chunkZ));
			}

			String rank = buf.readString();

			client.execute(() -> MinecraftClient.getInstance().setScreen(
					new com.sovereignstate.client.screen.MilitaryScreen(divID, armies, rank)));
		});

		// ─── Court Screen ─────────────────────────────────────────────────────

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.OPEN_COURT_SCREEN, (client, handler, buf, responseSender) -> {
			boolean has = buf.readBoolean();
			if (!has) {
				client.execute(() -> MinecraftClient.getInstance().player
						.sendMessage(net.minecraft.text.Text.literal("§cYou are not in a division.")));
				return;
			}

			String divID = buf.readString();

			int count = buf.readInt();
			java.util.List<com.sovereignstate.client.screen.CourtScreen.CaseEntry> cases = new java.util.ArrayList<>();
			for (int i = 0; i < count; i++) {
				String id             = buf.readString();
				String status         = buf.readString();
				String chargesSummary = buf.readString();
				String defendantUUID  = buf.readString();
				String plaintiffUUID  = buf.readString();
				String verdict        = buf.readString();
				cases.add(new com.sovereignstate.client.screen.CourtScreen.CaseEntry(
						id, status, chargesSummary, defendantUUID, plaintiffUUID, verdict));
			}

			client.execute(() -> MinecraftClient.getInstance().setScreen(
					new com.sovereignstate.client.screen.CourtScreen(divID, cases)));
		});
	}
}