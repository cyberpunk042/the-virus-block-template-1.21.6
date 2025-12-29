package net.cyberpunk042.client.visual.shader;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import net.cyberpunk042.visual.effect.CoronaEffect;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

/**
 * Manages Corona shader uniforms.
 * 
 * <p>The Corona shader needs these uniforms:</p>
 * <ul>
 *   <li>CoronaColor (vec3): RGB glow color</li>
 *   <li>CoronaPower (float): Edge sharpness</li>
 *   <li>CoronaIntensity (float): Brightness multiplier</li>
 *   <li>CoronaFalloff (float): Glow spread</li>
 * </ul>
 * 
 * <h2>Uniform Buffer Layout (std140)</h2>
 * <pre>
 * offset 0:  vec3 CoronaColor   (12 bytes, padded to 16)
 * offset 16: float CoronaPower  (4 bytes)
 * offset 20: float CoronaIntensity (4 bytes)
 * offset 24: float CoronaFalloff (4 bytes)
 * Total: 32 bytes (with padding)
 * </pre>
 * 
 * @see CoronaEffect
 * @see CoronaPipelines
 */
public final class CoronaUniformManager {
    
    // Buffer size for std140 layout
    private static final int BUFFER_SIZE = 32;
    
    // Current corona parameters
    private static final Vector3f coronaColor = new Vector3f(1f, 1f, 1f);
    private static float coronaPower = 2f;
    private static float coronaIntensity = 1f;
    private static float coronaFalloff = 0.5f;
    private static boolean dirty = true;
    
    /**
     * Sets corona parameters from individual values.
     */
    public static void setParams(float red, float green, float blue, 
                                  float power, float intensity, float falloff) {
        coronaColor.set(red, green, blue);
        coronaPower = power;
        coronaIntensity = intensity;
        coronaFalloff = falloff;
        dirty = true;
    }
    
    /**
     * Sets corona parameters from CoronaEffect record.
     */
    public static void setParams(CoronaEffect effect) {
        if (effect == null || !effect.enabled()) {
            // Disable corona by setting intensity to 0
            setParams(1f, 1f, 1f, 2f, 0f, 0.5f);
        } else {
            setParams(
                effect.red(),
                effect.green(),
                effect.blue(),
                effect.power(),
                effect.intensity(),
                effect.falloff()
            );
        }
    }
    
    /**
     * Gets current corona effect parameters.
     */
    public static CoronaEffect getCurrentEffect() {
        return new CoronaEffect(
            coronaIntensity > 0,
            coronaPower,
            coronaIntensity,
            coronaFalloff,
            coronaColor.x,
            coronaColor.y,
            coronaColor.z,
            0f,  // offset (default)
            1f   // width (default)
        );
    }
    
    /**
     * Resets to default (disabled) corona.
     */
    public static void reset() {
        coronaColor.set(1f, 1f, 1f);
        coronaPower = 2f;
        coronaIntensity = 0f; // disabled
        coronaFalloff = 0.5f;
        dirty = true;
    }
    
    /**
     * Creates a GPU buffer containing the current Corona uniforms.
     */
    public static GpuBuffer createUniformBuffer() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Std140Builder builder = Std140Builder.onStack(stack, BUFFER_SIZE);
            
            // Write uniforms in order (matching GLSL declaration)
            builder.putVec3(coronaColor);    // CoronaColor
            builder.putFloat(coronaPower);    // CoronaPower
            builder.putFloat(coronaIntensity); // CoronaIntensity
            builder.putFloat(coronaFalloff);  // CoronaFalloff
            
            // Create GPU buffer
            return RenderSystem.getDevice().createBuffer(
                () -> "CoronaParams uniform buffer",
                128, // Usage flags (uniform buffer)
                builder.get()
            );
        }
    }
    
    /**
     * Binds the corona uniforms to a RenderPass.
     * 
     * @param renderPass The active render pass
     * @param uniformName The name of the uniform block in GLSL
     * @return The created GpuBuffer (caller should close after rendering)
     */
    public static GpuBuffer bindUniforms(RenderPass renderPass, String uniformName) {
        GpuBuffer buffer = createUniformBuffer();
        renderPass.setUniform(uniformName, buffer);
        return buffer;
    }
    
    /**
     * Returns current corona color.
     */
    public static Vector3f getColor() {
        return new Vector3f(coronaColor);
    }
    
    /**
     * Returns current corona power.
     */
    public static float getPower() {
        return coronaPower;
    }
    
    /**
     * Returns current corona intensity.
     */
    public static float getIntensity() {
        return coronaIntensity;
    }
    
    /**
     * Returns current corona falloff.
     */
    public static float getFalloff() {
        return coronaFalloff;
    }
    
    private CoronaUniformManager() {}
}
