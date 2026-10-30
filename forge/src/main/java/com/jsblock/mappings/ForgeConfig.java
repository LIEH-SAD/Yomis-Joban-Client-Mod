package com.jsblock.mappings;

import com.jsblock.client.ClientConfig;

/**
 * Forge-side config bootstrap referenced by {@link com.jsblock.JobanForge}.
 *
 * <p>JCM stores its settings in a plain JSON file handled by {@link ClientConfig},
 * which is loaded during client init, so there is no {@code ForgeConfigSpec} to
 * register with Forge.</p>
 */
public class ForgeConfig {
	public static void registerConfig() {
		// Nothing to register: config values live in config/jsclient.json (see ClientConfig).
	}
}
