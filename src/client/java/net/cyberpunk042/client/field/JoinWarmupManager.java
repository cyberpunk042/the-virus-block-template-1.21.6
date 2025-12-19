package net.cyberpunk042.client.field;

import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual.mesh.SphereTessellator;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.pattern.QuadPattern;
import net.cyberpunk042.visual.shape.SphereAlgorithm;
import net.cyberpunk042.visual.shape.SphereShape;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages warmup on server join to prevent first-render lag.
 * 
 * <p>When a player joins a server, this manager:
 * <ol>
 *   <li>Delays rendering of fields for a short period</li>
 *   <li>Triggers warmup tasks (tessellation, profile loading)</li>
 *   <li>Enables rendering once warmup is complete</li>
 * </ol>
 * 
 * <p>This prevents the "lag spike on first shield spawn" issue by
 * doing the expensive JIT compilation during the join transition.
 */
public final class JoinWarmupManager {
    
    // Wait 10 seconds for chunks to load before enabling field rendering
    private static final int WARMUP_TICKS = 200;  // 10 seconds
    private static final AtomicBoolean warmupComplete = new AtomicBoolean(true);
    private static final AtomicInteger ticksSinceStart = new AtomicInteger(0);
    private static CompletableFuture<Void> warmupFuture = null;
    private static boolean asyncWarmupDone = false;
    
    private JoinWarmupManager() {}
    
    /**
     * Registers the join event handler.
     * Call this during client init.
     */
    public static void init() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            Logging.RENDER.topic("warmup").info("Player joined - starting warmup phase");
            startWarmup();
        });
        
        Logging.RENDER.topic("warmup").info("JoinWarmupManager initialized");
    }
    
    /**
     * Starts the warmup phase.
     */
    private static void startWarmup() {
        warmupComplete.set(false);
        ticksSinceStart.set(0);
        asyncWarmupDone = false;
        
        // Show toast to user
        MinecraftClient.getInstance().execute(() -> {
            net.cyberpunk042.client.gui.widget.ToastNotification.info("Loading visual effects...");
        });
        
        // Run warmup tasks async (off main thread)
        warmupFuture = CompletableFuture.runAsync(() -> {
            try {
                long start = System.currentTimeMillis();
                
                // Warm up various sphere configurations
                warmupSphere(12.0f, 19, 23);  // Anti-virus style
                warmupSphere(5.0f, 12, 16);   // Void tear style
                warmupSphere(1.0f, 8, 12);    // Small field style
                
                // Warm up patterns (different code paths)
                warmupPatterns();
                
                long took = System.currentTimeMillis() - start;
                Logging.RENDER.topic("warmup").info("Async warmup complete: {}ms", took);
                
            } catch (Exception e) {
                Logging.RENDER.topic("warmup").warn("Warmup failed: {}", e.getMessage());
            }
        });
    }
    
    /**
     * Pre-tessellates a sphere to warm up the vertex generation code.
     */
    private static void warmupSphere(float radius, int latSteps, int lonSteps) {
        SphereShape shape = SphereShape.builder()
            .radius(radius)
            .latSteps(latSteps)
            .lonSteps(lonSteps)
            .algorithm(SphereAlgorithm.LAT_LON)
            .build();
        
        // Tessellate with and without patterns
        Mesh mesh1 = SphereTessellator.tessellate(shape);
        Mesh mesh2 = SphereTessellator.tessellate(shape, QuadPattern.STRIPE_1, null, null, 0);
        
        Logging.RENDER.topic("warmup").debug(
            "Warmed sphere: r={}, verts={}/{}", radius,
            mesh1 != null ? mesh1.vertexCount() : 0,
            mesh2 != null ? mesh2.vertexCount() : 0);
    }
    
    /**
     * Warms up pattern resolution code paths.
     */
    private static void warmupPatterns() {
        // Touch various patterns to trigger class loading
        for (QuadPattern pattern : QuadPattern.values()) {
            pattern.getVertexOrder();  // Force pattern to compute vertex order
        }
    }
    
    /**
     * Called every client tick to update warmup state.
     */
    public static void tick() {
        if (warmupComplete.get()) {
            return;
        }
        
        int ticks = ticksSinceStart.incrementAndGet();
        
        // Track when async tessellation completes
        if (!asyncWarmupDone && warmupFuture != null && warmupFuture.isDone()) {
            asyncWarmupDone = true;
            Logging.RENDER.topic("warmup").info("Tessellation warmup done at tick {}", ticks);
        }
        
        // Complete when full warmup time is done (waiting for chunks)
        if (ticks >= WARMUP_TICKS) {
            completeWarmup();
        }
    }
    
    private static void completeWarmup() {
        warmupComplete.set(true);
        warmupFuture = null;
        Logging.RENDER.topic("warmup").info("Warmup phase complete - fields will now render");
        
        // Show completion toast
        MinecraftClient.getInstance().execute(() -> {
            net.cyberpunk042.client.gui.widget.ToastNotification.success("Effects loaded!");
        });
    }
    
    /**
     * Returns true if fields should be rendered.
     * Returns false during warmup phase.
     */
    public static boolean shouldRenderFields() {
        return warmupComplete.get();
    }
    
    /**
     * Returns the warmup progress (0.0 = just started, 1.0 = complete).
     */
    public static float getWarmupProgress() {
        if (warmupComplete.get()) {
            return 1.0f;
        }
        // Progress based on time elapsed
        int ticks = ticksSinceStart.get();
        return Math.min(1.0f, (float) ticks / WARMUP_TICKS);
    }
    
    /**
     * Returns true if currently in warmup phase.
     */
    public static boolean isWarmingUp() {
        return !warmupComplete.get();
    }
}
