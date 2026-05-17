package com.sovereignstate.registry;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {

    public static final SoundEvent LAW_ENACTED = registerSound("law_enacted");
    public static final SoundEvent ARREST_MADE = registerSound("arrest_made");
    public static final SoundEvent ELECTION_RESULT = registerSound("election_result");
    public static final SoundEvent REVOLUTION_START = registerSound("revolution_start");
    public static final SoundEvent CURRENCY_MINTED = registerSound("currency_minted");
    public static final SoundEvent VOTE_CAST = registerSound("vote_cast");
    public static final SoundEvent WAR_DECLARED = registerSound("war_declared");
    public static final SoundEvent TREATY_SIGNED = registerSound("treaty_signed");
    public static final SoundEvent TAX_COLLECTED = registerSound("tax_collected");

    private static SoundEvent registerSound(String name) {
        Identifier id = new Identifier("sovereignstate", name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void register() {
        // Sounds are registered via field initializers above
        // This method just needs to be called to trigger class loading
    }
}