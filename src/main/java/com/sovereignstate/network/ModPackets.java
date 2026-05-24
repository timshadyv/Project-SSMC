package com.sovereignstate.network;

import net.minecraft.util.Identifier;

public class ModPackets {

    // Server → Client: open the Division Info screen
    public static final Identifier OPEN_DIVISION_SCREEN =
            new Identifier("sovereignstate", "open_division_screen");

    // Server → Client: open the Diplomacy screen
    public static final Identifier OPEN_DIPLOMACY_SCREEN =
            new Identifier("sovereignstate", "open_diplomacy_screen");

    // Server → Client: open the Military screen
    public static final Identifier OPEN_MILITARY_SCREEN =
            new Identifier("sovereignstate", "open_military_screen");

    // Server → Client: open the Court screen
    public static final Identifier OPEN_COURT_SCREEN =
            new Identifier("sovereignstate", "open_court_screen");
}