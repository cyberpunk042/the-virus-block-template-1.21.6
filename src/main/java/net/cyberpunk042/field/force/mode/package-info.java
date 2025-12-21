/**
 * Mode-specific configuration records for force fields.
 * 
 * <p>Each force mode ({@link net.cyberpunk042.field.force.ForceMode}) has a corresponding
 * configuration record in this package that defines its unique parameters.
 * 
 * <h2>Available Modes</h2>
 * <table>
 *   <tr><th>Mode</th><th>Config Class</th><th>Description</th></tr>
 *   <tr><td>PULL</td><td>{@link PullModeConfig}</td><td>Simple attraction</td></tr>
 *   <tr><td>PUSH</td><td>{@link PushModeConfig}</td><td>Simple repulsion</td></tr>
 *   <tr><td>VORTEX</td><td>{@link VortexModeConfig}</td><td>Spiral inward</td></tr>
 *   <tr><td>ORBIT</td><td>{@link OrbitModeConfig}</td><td>Stable circular motion</td></tr>
 *   <tr><td>TORNADO</td><td>{@link TornadoModeConfig}</td><td>Vertical lift + spin</td></tr>
 *   <tr><td>RING</td><td>{@link RingModeConfig}</td><td>Stable orbit band</td></tr>
 *   <tr><td>IMPLOSION</td><td>{@link ImplosionModeConfig}</td><td>Gravitational collapse</td></tr>
 *   <tr><td>EXPLOSION</td><td>{@link ExplosionModeConfig}</td><td>Radial blast</td></tr>
 * </table>
 * 
 * <h2>Usage</h2>
 * <pre>
 * // Create a vortex configuration
 * VortexModeConfig vortex = VortexModeConfig.builder()
 *     .radialStrength(0.15f)
 *     .tangentialStrength(0.2f)
 *     .spinAxis(ForceAxis.Y)
 *     .clockwise(true)
 *     .tightness(0.6f)
 *     .build();
 * 
 * // Include in ForceFieldConfig
 * ForceFieldConfig config = ForceFieldConfig.builder()
 *     .mode(ForceMode.VORTEX)
 *     .vortex(vortex)
 *     .build();
 * </pre>
 * 
 * @see net.cyberpunk042.field.force.ForceMode
 * @see net.cyberpunk042.field.force.ForceFieldConfig
 */
package net.cyberpunk042.field.force.mode;
