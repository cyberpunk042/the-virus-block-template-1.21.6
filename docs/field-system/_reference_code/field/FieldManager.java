package net.cyberpunk042.field;

import net.cyberpunk042.field.instance.FieldInstance;
import net.cyberpunk042.field.instance.PersonalFieldInstance;
import net.cyberpunk042.field.instance.AnchoredFieldInstance;
import net.cyberpunk042.log.Logging;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Server-side manager for active field instances.
 */
public final class FieldManager {
    
    private static final Map<ServerWorld, FieldManager> MANAGERS = new WeakHashMap<>();
    private static final AtomicLong ID_COUNTER = new AtomicLong(1);
    
    private final ServerWorld world;
    private final Map<Long, FieldInstance> instances = new ConcurrentHashMap<>();
    private final Map<UUID, Long> playerFields = new ConcurrentHashMap<>();
    
    private Consumer<FieldInstance> onSpawn;
    private Consumer<Long> onRemove;
    private Consumer<FieldInstance> onUpdate;
    
    private FieldManager(ServerWorld world) {
        this.world = world;
    }
    
    public static FieldManager get(ServerWorld world) {
        return MANAGERS.computeIfAbsent(world, FieldManager::new);
    }
    
    public static void remove(ServerWorld world) {
        MANAGERS.remove(world);
    }
    
    public void onSpawn(Consumer<FieldInstance> callback) { this.onSpawn = callback; }
    public void onRemove(Consumer<Long> callback) { this.onRemove = callback; }
    public void onUpdate(Consumer<FieldInstance> callback) { this.onUpdate = callback; }
    
    public AnchoredFieldInstance spawnAt(Identifier definitionId, Vec3d position, float scale, int lifetimeTicks) {
        FieldDefinition def = FieldRegistry.get(definitionId);
        if (def == null) {
            Logging.REGISTRY.topic("field").warn("Unknown definition: {}", definitionId);
            return null;
        }
        
        long id = ID_COUNTER.getAndIncrement();
        AnchoredFieldInstance instance = new AnchoredFieldInstance(
            id, definitionId, def.type(), BlockPos.ofFloored(position));
        instance.setScale(scale);
        if (lifetimeTicks > 0) instance.setMaxLifeTicks(lifetimeTicks);
        instance.setPhase(world.random.nextFloat() * (float) Math.PI * 2);
        
        instances.put(id, instance);
        if (onSpawn != null) onSpawn.accept(instance);
        
        Logging.REGISTRY.topic("field").info(
            "Spawned field {} at {} (lifetime={})", definitionId, position, lifetimeTicks);
        return instance;
    }
    
    public AnchoredFieldInstance spawnAtBlock(Identifier definitionId, BlockPos pos, float scale) {
        FieldDefinition def = FieldRegistry.get(definitionId);
        if (def == null) {
            Logging.REGISTRY.topic("field").warn("Unknown definition: {}", definitionId);
            return null;
        }
        
        long id = ID_COUNTER.getAndIncrement();
        AnchoredFieldInstance instance = new AnchoredFieldInstance(id, definitionId, def.type(), pos);
        instance.setScale(scale);
        instance.setPhase(world.random.nextFloat() * (float) Math.PI * 2);
        
        instances.put(id, instance);
        if (onSpawn != null) onSpawn.accept(instance);
        
        Logging.REGISTRY.topic("field").info(
            "Spawned field {} at block {} (scale={})", definitionId, pos, scale);
        return instance;
    }
    
    public PersonalFieldInstance spawnForPlayer(Identifier definitionId, UUID playerUuid, float scale) {
        Long existing = playerFields.get(playerUuid);
        if (existing != null) remove(existing);
        
        FieldDefinition def = FieldRegistry.get(definitionId);
        if (def == null) return null;
        
        long id = ID_COUNTER.getAndIncrement();
        PersonalFieldInstance instance = new PersonalFieldInstance(
            id, definitionId, def.type(), playerUuid, Vec3d.ZERO);
        instance.setScale(scale);
        instance.setPhase(world.random.nextFloat() * (float) Math.PI * 2);
        
        instances.put(id, instance);
        playerFields.put(playerUuid, id);
        if (onSpawn != null) onSpawn.accept(instance);
        return instance;
    }
    
    public boolean remove(long id) {
        FieldInstance instance = instances.remove(id);
        if (instance == null) return false;
        
        instance.remove();
        if (instance.ownerUuid() != null) playerFields.remove(instance.ownerUuid());
        if (onRemove != null) onRemove.accept(id);
        return true;
    }
    
    public boolean removePlayerField(UUID playerUuid) {
        Long id = playerFields.remove(playerUuid);
        return id != null && remove(id);
    }
    
    public FieldInstance get(long id) { return instances.get(id); }
    
    public FieldInstance getPlayerField(UUID playerUuid) {
        Long id = playerFields.get(playerUuid);
        return id != null ? instances.get(id) : null;
    }
    
    public Collection<FieldInstance> all() {
        return Collections.unmodifiableCollection(instances.values());
    }
    
    /**
     * Gets a specific field instance by ID.
     */
    public FieldInstance getInstance(long id) {
        return instances.get(id);
    }
    
    public int count() { return instances.size(); }
    
    public void tick() {
        Iterator<Map.Entry<Long, FieldInstance>> it = instances.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, FieldInstance> entry = it.next();
            FieldInstance instance = entry.getValue();
            
            if (instance.tick()) {
                it.remove();
                if (instance.ownerUuid() != null) playerFields.remove(instance.ownerUuid());
                if (onRemove != null) onRemove.accept(entry.getKey());
            }
        }
    }
    
    public void clear() {
        instances.clear();
        playerFields.clear();
    }
}
