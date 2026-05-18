package com.sovereignstate;

import com.sovereignstate.registry.ModBlocks;
import com.sovereignstate.registry.ModItems;
import com.sovereignstate.registry.ModSounds;
import com.sovereignstate.data.WorldStateData;
import com.sovereignstate.data.PlayerStateData;
import com.sovereignstate.data.DivisionData;
import com.sovereignstate.data.CurrencyData;
import com.sovereignstate.data.CultureData;
import com.sovereignstate.systems.CommandSystem;
import com.sovereignstate.systems.LawSystem;
import com.sovereignstate.systems.TaxSystem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SovereignState implements ModInitializer {

	public static final String MOD_ID = "sovereignstate";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static int tickCounter = 0;
	private static final int TICK_INTERVAL = 200;

	@Override
	public void onInitialize() {
		LOGGER.info("Sovereign State initializing...");

		ModItems.register();
		ModBlocks.register();
		ModSounds.register();
		CommandSystem.register();

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			server.getWorlds().forEach(world -> {
				WorldStateData worldState = WorldStateData.get(world);
				PlayerStateData.get(world);
				DivisionData.get(world);
				CurrencyData.get(world);
				CultureData cultureData = CultureData.get(world);

				if (!worldState.hasTag("isPureRoleplayMode")) {
					worldState.setBooleanTag("isPureRoleplayMode", false);
				}
				if (!worldState.hasTag("sessionMode")) {
					worldState.setTag("sessionMode", "multiplayer_open");
				}
				if (!worldState.hasTag("npcMode")) {
					worldState.setTag("npcMode", "full_simulation");
				}

				cultureData.loadPresetsIfNeeded();

				LOGGER.info("Sovereign State world state initialized.");
			});
		});

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			server.getWorlds().forEach(world -> {
				WorldStateData.get(world).markDirty();
				PlayerStateData.get(world).markDirty();
				DivisionData.get(world).markDirty();
				CurrencyData.get(world).markDirty();
				CultureData.get(world).markDirty();
			});
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			tickCounter++;
			if (tickCounter < TICK_INTERVAL) return;
			tickCounter = 0;

			server.getWorlds().forEach(world -> {
				LawSystem.tick(server);
			});
			TaxSystem.tick(server);
		});

		LOGGER.info("Sovereign State initialized successfully.");
	}
}