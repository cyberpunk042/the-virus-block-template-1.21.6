package net.cyberpunk042.client.field;

import net.cyberpunk042.client.visual.mesh.SphereTessellator;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.pattern.QuadPattern;
import net.cyberpunk042.visual.shape.SphereAlgorithm;
import net.cyberpunk042.visual.shape.SphereShape;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Smooth loading bar on server join.
 * 
 * Progress is TICK-BASED for smoothness:
 * - 0-50% (0-100 ticks): Loading Effects + Profiles (animated smoothly)
 * - 50-100% (waits): Chunk loading (per-chunk progress)
 */
public final class JoinWarmupManager {
    
    // Timing
    private static final int EFFECTS_TICKS = 50;    // 2.5 sec for effects (0-25%)
    private static final int PROFILES_TICKS = 50;   // 2.5 sec for profiles (25-50%)
    private static final int CHUNK_RADIUS = 2;      // 5x5 chunks
    private static final int TOTAL_CHUNKS = (CHUNK_RADIUS * 2 + 1) * (CHUNK_RADIUS * 2 + 1); // 25
    private static final int MAX_CHUNK_WAIT = 600;  // 30 sec max wait for chunks
    
    // State
    private static final AtomicBoolean warmupComplete = new AtomicBoolean(true);
    private static final AtomicInteger tickCount = new AtomicInteger(0);
    
    private static volatile boolean effectsStarted = false;
    private static volatile boolean effectsDone = false;
    private static volatile boolean profilesDone = false;
    private static volatile int chunksLoaded = 0;
    private static volatile int chunkWaitTicks = 0;
    
    private static CompletableFuture<Void> asyncTask = null;
    
    private JoinWarmupManager() {}
    
    public static void init() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            Logging.RENDER.topic("warmup").info("Player joined - starting smooth warmup");
            startWarmup();
        });
        Logging.RENDER.topic("warmup").info("JoinWarmupManager initialized");
    }
    
    private static void startWarmup() {
        // Reset all state
        warmupComplete.set(false);
        tickCount.set(0);
        effectsStarted = false;
        effectsDone = false;
        profilesDone = false;
        chunksLoaded = 0;
        chunkWaitTicks = 0;
        
        // Start async warmup (runs in background while bar animates)
        asyncTask = CompletableFuture.runAsync(() -> {
            try {
                // Warmup effects (may complete faster than the animation)
                warmupSphere(15.0f, 24, 32);
                warmupSphere(8.0f, 16, 24);
                warmupSphere(3.0f, 10, 14);
                warmupPatterns();
                effectsDone = true;
                Logging.RENDER.topic("warmup").debug("Effects tessellation done");
                
                // Load profiles
                net.cyberpunk042.client.profile.ProfileManager.getInstance().loadAll();
                profilesDone = true;
                Logging.RENDER.topic("warmup").debug("Profiles loaded");
                
            } catch (Exception e) {
                Logging.RENDER.topic("warmup").error("Warmup error: {}", e.getMessage());
                effectsDone = true;
                profilesDone = true;
            }
        });
        
        effectsStarted = true;
    }
    
    private static void warmupSphere(float radius, int lat, int lon) {
        SphereShape shape = SphereShape.builder()
            .radius(radius)
            .latSteps(lat)
            .lonSteps(lon)
            .algorithm(SphereAlgorithm.LAT_LON)
            .build();
        SphereTessellator.tessellate(shape);
        SphereTessellator.tessellate(shape, QuadPattern.STRIPE_1, null, null, 0);
    }
    
    private static void warmupPatterns() {
        for (QuadPattern p : QuadPattern.values()) {
            p.getVertexOrder();
        }
    }
    
    /**
     * Called every tick. This drives the smooth progress animation.
     */
    public static void tick() {
        if (warmupComplete.get()) {
            return;
        }
        
        int ticks = tickCount.incrementAndGet();
        
        // Phase 1: Effects (0-25%) - 50 ticks
        // Phase 2: Profiles (25-50%) - 50 ticks
        // Phase 3: Chunks (50-100%) - wait for actual chunks
        
        if (ticks <= EFFECTS_TICKS + PROFILES_TICKS) {
            // First 100 ticks: smooth animation from 0-50%
            // The async work runs in parallel but we animate smoothly regardless
            return;
        }
        
        // After 100 ticks, we're at 50%+ and now we honestly wait for chunks
        chunkWaitTicks++;
        chunksLoaded = countLoadedChunks();
        
        // Check completion
        boolean allChunksLoaded = chunksLoaded >= TOTAL_CHUNKS;
        boolean chunkTimeout = chunkWaitTicks >= MAX_CHUNK_WAIT;
        
        if (allChunksLoaded) {
            Logging.RENDER.topic("warmup").info("All {} chunks loaded!", TOTAL_CHUNKS);
            completeWarmup();
        } else if (chunkTimeout) {
            Logging.RENDER.topic("warmup").warn("Chunk timeout - {} of {} loaded", chunksLoaded, TOTAL_CHUNKS);
            completeWarmup();
        }
    }
    
    /**
     * Counts FULLY loaded chunks in 5x5 around player.
     * Uses ChunkStatus.FULL to ensure chunk has terrain data.
     */
    private static int countLoadedChunks() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return 0;
        }
        
        ChunkPos center = client.player.getChunkPos();
        int count = 0;
        
        for (int dx = -CHUNK_RADIUS; dx <= CHUNK_RADIUS; dx++) {
            for (int dz = -CHUNK_RADIUS; dz <= CHUNK_RADIUS; dz++) {
                int cx = center.x + dx;
                int cz = center.z + dz;
                
                // Use getChunk with ChunkStatus.FULL to check for fully loaded chunks
                // This returns null if chunk is not at FULL status
                var chunk = client.world.getChunkManager().getChunk(
                    cx, cz, 
                    net.minecraft.world.chunk.ChunkStatus.FULL, 
                    false  // Don't create if missing
                );
                
                if (chunk != null) {
                    count++;
                }
            }
        }
        
        return count;
    }
    
    private static void completeWarmup() {
        warmupComplete.set(true);
        asyncTask = null;
        Logging.RENDER.topic("warmup").info("Warmup complete - fields will render");
        
        MinecraftClient.getInstance().execute(() -> {
            net.cyberpunk042.client.gui.widget.ToastNotification.success("Ready!");
        });
    }
    
    public static boolean shouldRenderFields() {
        return warmupComplete.get();
    }
    
    /**
     * Returns smooth progress 0.0 to 1.0.
     * 
     * Progress is TICK-BASED:
     * - Ticks 0-50: 0% to 25% (effects)
     * - Ticks 50-100: 25% to 50% (profiles)
     * - After 100: 50% + chunk progress to 100%
     */
    public static float getWarmupProgress() {
        if (warmupComplete.get()) {
            return 1.0f;
        }
        
        int ticks = tickCount.get();
        
        // Phase 1: Effects (0-25%)
        if (ticks <= EFFECTS_TICKS) {
            return (float) ticks / EFFECTS_TICKS * 0.25f;
        }
        
        // Phase 2: Profiles (25-50%)
        if (ticks <= EFFECTS_TICKS + PROFILES_TICKS) {
            int profileTicks = ticks - EFFECTS_TICKS;
            return 0.25f + (float) profileTicks / PROFILES_TICKS * 0.25f;
        }
        
        // Phase 3: Chunks (50-100%)
        float chunkProgress = (float) chunksLoaded / TOTAL_CHUNKS;
        return 0.50f + chunkProgress * 0.50f;
    }
    
    /**
     * Returns the current stage label.
     */
    public static String getCurrentStageLabel() {
        if (warmupComplete.get()) {
            return "Ready!";
        }
        
        int ticks = tickCount.get();
        
        if (ticks <= EFFECTS_TICKS) {
            return "Loading Effects...";
        } else if (ticks <= EFFECTS_TICKS + PROFILES_TICKS) {
            return "Loading Profiles...";
        } else {
            return String.format("Loading Chunks (%d/%d)...", chunksLoaded, TOTAL_CHUNKS);
        }
    }
    
    public static boolean isWarmingUp() {
        return !warmupComplete.get();
    }
}
