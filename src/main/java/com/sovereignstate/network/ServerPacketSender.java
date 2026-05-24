package com.sovereignstate.network;

import com.sovereignstate.data.ArmyData;
import com.sovereignstate.data.CaseData;
import com.sovereignstate.data.DivisionData;
import com.sovereignstate.data.PlayerStateData;
import com.sovereignstate.systems.DiplomacySystem;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.List;

public class ServerPacketSender {

    // ─── Division Info Screen ─────────────────────────────────────────────────

    public static void sendOpenDivisionScreen(ServerPlayerEntity player, ServerWorld world) {
        String uuid = player.getUuid().toString();
        String divID = PlayerStateData.get(world).getDivisionID(uuid);

        PacketByteBuf buf = PacketByteBufs.create();

        if (divID == null || divID.isEmpty()) {
            buf.writeBoolean(false); // no division
            ServerPlayNetworking.send(player, ModPackets.OPEN_DIVISION_SCREEN, buf);
            return;
        }

        NbtCompound div = DivisionData.get(world).getDivisionById(divID);
        if (div == null) {
            buf.writeBoolean(false);
            ServerPlayNetworking.send(player, ModPackets.OPEN_DIVISION_SCREEN, buf);
            return;
        }

        buf.writeBoolean(true);
        buf.writeString(div.getString("id"));
        buf.writeString(div.getString("name"));
        buf.writeString(div.getString("governmentType"));
        buf.writeString(div.getString("leaderUUID"));
        buf.writeInt(div.getInt("population"));
        buf.writeInt(div.getInt("treasury"));
        buf.writeString(div.getString("capitalChunk"));

        // Laws
        List<String> laws = DivisionData.get(world).getLaws(divID);
        buf.writeInt(laws.size());
        for (String law : laws) buf.writeString(law);

        // Allies & enemies
        List<String> allies = DiplomacySystem.getAllies(world, divID);
        buf.writeInt(allies.size());
        for (String a : allies) buf.writeString(a);

        List<String> enemies = DiplomacySystem.getEnemies(world, divID);
        buf.writeInt(enemies.size());
        for (String e : enemies) buf.writeString(e);

        // Vassal / overlord
        String overlord = DiplomacySystem.getOverlord(world, divID);
        buf.writeString(overlord != null ? overlord : "");

        List<String> vassals = DiplomacySystem.getVassals(world, divID);
        buf.writeInt(vassals.size());
        for (String v : vassals) buf.writeString(v);

        // Player's military rank
        String rank = PlayerStateData.get(world).getMilitaryRank(uuid);
        buf.writeString(rank != null ? rank : "CIVILIAN");

        ServerPlayNetworking.send(player, ModPackets.OPEN_DIVISION_SCREEN, buf);
    }

    // ─── Military Screen ──────────────────────────────────────────────────────

    public static void sendOpenMilitaryScreen(ServerPlayerEntity player, ServerWorld world) {
        String uuid = player.getUuid().toString();
        String divID = PlayerStateData.get(world).getDivisionID(uuid);

        PacketByteBuf buf = PacketByteBufs.create();

        if (divID == null || divID.isEmpty()) {
            buf.writeBoolean(false);
            ServerPlayNetworking.send(player, ModPackets.OPEN_MILITARY_SCREEN, buf);
            return;
        }

        buf.writeBoolean(true);
        buf.writeString(divID);

        List<NbtCompound> armies = ArmyData.get(world).getArmiesByDivision(divID);
        buf.writeInt(armies.size());
        for (NbtCompound army : armies) {
            buf.writeString(army.getString("id"));
            buf.writeString(army.getString("name"));
            buf.writeString(army.getString("unitType"));
            buf.writeInt(army.getInt("unitCount"));
            buf.writeBoolean(army.getBoolean("isDeployed"));
            buf.writeInt(army.getInt("chunkX"));
            buf.writeInt(army.getInt("chunkZ"));
        }

        String rank = PlayerStateData.get(world).getMilitaryRank(uuid);
        buf.writeString(rank != null ? rank : "CIVILIAN");

        ServerPlayNetworking.send(player, ModPackets.OPEN_MILITARY_SCREEN, buf);
    }

    // ─── Diplomacy Screen ─────────────────────────────────────────────────────

    public static void sendOpenDiplomacyScreen(ServerPlayerEntity player, ServerWorld world) {
        String uuid = player.getUuid().toString();
        String divID = PlayerStateData.get(world).getDivisionID(uuid);

        PacketByteBuf buf = PacketByteBufs.create();

        if (divID == null || divID.isEmpty()) {
            buf.writeBoolean(false);
            ServerPlayNetworking.send(player, ModPackets.OPEN_DIPLOMACY_SCREEN, buf);
            return;
        }

        buf.writeBoolean(true);
        buf.writeString(divID);

        List<String> allies = DiplomacySystem.getAllies(world, divID);
        buf.writeInt(allies.size());
        for (String a : allies) buf.writeString(a);

        List<String> enemies = DiplomacySystem.getEnemies(world, divID);
        buf.writeInt(enemies.size());
        for (String e : enemies) buf.writeString(e);

        List<String> allianceIn = DiplomacySystem.getIncomingAllianceProposals(world, divID);
        buf.writeInt(allianceIn.size());
        for (String a : allianceIn) buf.writeString(a);

        List<String> peaceIn = DiplomacySystem.getIncomingPeaceProposals(world, divID);
        buf.writeInt(peaceIn.size());
        for (String p : peaceIn) buf.writeString(p);

        List<String> vassalIn = DiplomacySystem.getIncomingVassalProposals(world, divID);
        buf.writeInt(vassalIn.size());
        for (String v : vassalIn) buf.writeString(v);

        String overlord = DiplomacySystem.getOverlord(world, divID);
        buf.writeString(overlord != null ? overlord : "");

        List<String> vassals = DiplomacySystem.getVassals(world, divID);
        buf.writeInt(vassals.size());
        for (String v : vassals) buf.writeString(v);

        ServerPlayNetworking.send(player, ModPackets.OPEN_DIPLOMACY_SCREEN, buf);
    }

    // ─── Court Screen ─────────────────────────────────────────────────────────

    public static void sendOpenCourtScreen(ServerPlayerEntity player, ServerWorld world) {
        String uuid = player.getUuid().toString();
        String divID = PlayerStateData.get(world).getDivisionID(uuid);

        PacketByteBuf buf = PacketByteBufs.create();

        if (divID == null || divID.isEmpty()) {
            buf.writeBoolean(false);
            ServerPlayNetworking.send(player, ModPackets.OPEN_COURT_SCREEN, buf);
            return;
        }

        buf.writeBoolean(true);
        buf.writeString(divID);

        List<NbtCompound> cases = CaseData.get(world).getOpenCasesByDivision(divID);
        buf.writeInt(cases.size());
        for (NbtCompound c : cases) {
            buf.writeString(c.getString("id"));
            buf.writeString(c.getString("status"));
            buf.writeString(c.getString("chargesSummary"));
            buf.writeString(c.getString("defendantUUID"));
            buf.writeString(c.getString("plaintiffUUID"));
            buf.writeString(c.getString("verdict"));
        }

        ServerPlayNetworking.send(player, ModPackets.OPEN_COURT_SCREEN, buf);
    }
}