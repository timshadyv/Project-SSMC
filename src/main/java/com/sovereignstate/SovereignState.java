package com.sovereignstate;

import com.sovereignstate.registry.ModBlocks;
import com.sovereignstate.registry.ModItems;
import com.sovereignstate.registry.ModSounds;
import com.sovereignstate.data.WorldStateData;
import com.sovereignstate.data.CultureData;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SovereignState implements ModInitializer {

	public static final String MOD_ID = "sovereignstate";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	// Tick counter to throttle systems
	private static int tickCounter = 0;
	private static final int TICK_INTERVAL = 200;

	@Override
	public void onInitialize() {
		LOGGER.info("Sovereign State initializing...");

		// Register all content
		ModItems.register();
		ModBlocks.register();
		ModSounds.register();

		// On world load: initialize world state
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			server.getWorlds().forEach(world -> {
				WorldStateData worldState = WorldStateData.get(world);

				// Set defaults if not already set
				if (!worldState.hasTag("isPureRoleplayMode")) {
					worldState.setBooleanTag("isPureRoleplayMode", false);
				}
				if (!worldState.hasTag("sessionMode")) {
					worldState.setTag("sessionMode", "multiplayer_open");
				}
				if (!worldState.hasTag("npcMode")) {
					worldState.setTag("npcMode", "full_simulation");
				}

				// Load culture presets
				CultureData cultureData = CultureData.get(world);
				cultureData.loadPresetsIfNeeded();

				LOGGER.info("Sovereign State world state initialized.");
			});
		});

		// Server tick: run all systems every 200 ticks
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			tickCounter++;
			if (tickCounter < TICK_INTERVAL) return;
			tickCounter = 0;

			server.getWorlds().forEach(world -> {
				// Systems will be called here as we build them
				// Each system gets added here when its file is complete
			});
		});

		LOGGER.info("Sovereign State initialized successfully.");
	}
}