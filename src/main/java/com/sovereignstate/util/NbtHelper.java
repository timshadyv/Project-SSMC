package com.sovereignstate.util;

import java.util.Random;

public class NbtHelper {

    private static final Random random = new Random();
    private static final String CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";

    // --- Key Builders ---

    public static String buildChunkKey(int chunkX, int chunkZ, String property) {
        return "chunk_" + chunkX + "_" + chunkZ + "_" + property;
    }

    public static String buildDivisionKey(String divisionID, String property) {
        return "division_" + divisionID + "_" + property;
    }

    public static String buildPlayerKey(String uuid, String property) {
        return "player_" + uuid + "_" + property;
    }

    public static String buildCultureKey(String cultureID, String property) {
        return "culture_" + cultureID + "_" + property;
    }

    public static String buildCurrencyKey(String currencyID, String property) {
        return "currency_" + currencyID + "_" + property;
    }

    public static String buildNpcKey(String npcID, String property) {
        return "npc_" + npcID + "_" + property;
    }

    public static String buildNationRelationKey(String id1, String relation, String id2) {
        return "nation_" + id1 + "_" + relation + "_" + id2;
    }

    // --- ID Generator ---

    public static String generateUniqueID() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}