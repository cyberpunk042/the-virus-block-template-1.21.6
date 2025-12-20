/**
 * Force Field System - Data-driven physics for entity manipulation.
 * 
 * <p>This package provides a modular, composable system for applying forces
 * to entities within force fields. All behavior is configurable via JSON.
 * 
 * <h2>Package Structure</h2>
 * <pre>
 * field.force/
 * ├── ForceFieldConfig.java        - Master configuration record
 * │
 * ├── core/
 * │   └── ForceContext.java        - Context for force calculation
 * │
 * ├── falloff/
 * │   ├── FalloffFunction.java     - Interface for distance → multiplier
 * │   └── FalloffFunctions.java    - Standard implementations
 * │
 * ├── phase/
 * │   ├── ForcePolarity.java       - PULL, PUSH, HOLD enum
 * │   ├── ForcePhase.java          - Time-based phase config
 * │   └── PhaseNotification.java   - Phase change notifications
 * │
 * ├── zone/
 * │   └── ForceZone.java           - Radius-based zone config
 * │
 * ├── field/
 * │   ├── ForceField.java          - Core force calculation interface
 * │   └── RadialForceField.java    - Pull/push implementation
 * │
 * └── service/
 *     └── ForceFieldService.java   - Active field management
 * </pre>
 * 
 * <h2>Usage Example</h2>
 * <pre>
 * // Define in JSON (field_profiles/gravity_well.json):
 * {
 *   "id": "gravity_well",
 *   "type": "force",
 *   "forceConfig": {
 *     "zones": [
 *       { "radius": 15, "strength": 0.1, "falloff": "quadratic" }
 *     ],
 *     "phases": [
 *       { "start": 0, "end": 100, "polarity": "pull" }
 *     ]
 *   }
 * }
 * 
 * // Spawn in code:
 * ForceFieldService.spawn(world, center, definition, 200);
 * </pre>
 * 
 * @see net.cyberpunk042.field.force.ForceFieldConfig
 * @see net.cyberpunk042.field.force.service.ForceFieldService
 */
package net.cyberpunk042.field.force;
