package net.cyberpunk042.client.render;

public final class VirusSkyClientState {
	private static volatile boolean skyCorrupted;
	private static volatile boolean fluidsCorrupted;

	private VirusSkyClientState() {
	}

	public static void setState(boolean sky, boolean fluids) {
		skyCorrupted = sky;
		fluidsCorrupted = fluids;
	}

	public static void reset() {
		setState(false, false);
	}

	public static boolean isSkyCorrupted() {
		return skyCorrupted;
	}

	public static boolean areFluidsCorrupted() {
		return fluidsCorrupted;
	}
}


