#version 150

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

// Custom UBO for Fresnel parameters (bound by CustomUniformBinder via Mixin)
// Now includes BOTH Horizon (rim) AND Corona (outer glow) parameters
layout(std140) uniform FresnelParams {
    vec4 RimColorAndPower;      // xyz = RimColor, w = RimPower
    vec4 RimIntensityAndPad;    // x = RimIntensity, yzw = padding
    // Corona parameters (extended glow layer)
    vec4 CoronaColorAndPower;   // xyz = CoronaColor, w = CoronaPower
    vec4 CoronaIntensityFalloff;// x = CoronaIntensity, y = CoronaFalloff, zw = padding
    vec4 CoronaOffsetWidthPad;  // x = Offset (bias), y = Width, zw = padding
};

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;
in vec3 vNormal;
in vec3 vViewDir;

out vec4 fragColor;

void main() {
    // Start with vertex color directly
    vec4 color = vertexColor * ColorModulator;
    
    // Calculate view angle (same for both effects)
    float NdotV = abs(dot(vNormal, vViewDir));
    float edgeFactor = 1.0 - NdotV;  // 1 at edges, 0 at center
    
    // ========================================
    // HORIZON EFFECT (Rim Lighting)
    // ========================================
    vec3 rimColor = RimColorAndPower.xyz;
    float rimPower = RimColorAndPower.w;
    float rimIntensity = RimIntensityAndPad.x;
    
    // Only apply if intensity > 0
    if (rimIntensity > 0.001) {
        float fresnel = pow(edgeFactor, rimPower);
        vec3 rimContribution = rimColor * fresnel * rimIntensity;
        color.rgb += rimContribution;
    }
    
    // ========================================
    // CORONA EFFECT (Outer Glow Layer)
    // ========================================
    vec3 coronaColor = CoronaColorAndPower.xyz;
    float coronaPower = CoronaColorAndPower.w;
    float coronaIntensity = CoronaIntensityFalloff.x;
    float coronaFalloff = CoronaIntensityFalloff.y;
    float coronaOffset = CoronaOffsetWidthPad.x;   // Shifts glow outward (bias)
    float coronaWidth = CoronaOffsetWidthPad.y;    // Controls glow thickness
    
    // Only apply if intensity > 0
    if (coronaIntensity > 0.001) {
        // Apply offset (bias): adds to edge factor to extend glow "beyond" edge
        // Positive offset = glow starts earlier (wider coverage)
        float biasedEdge = clamp(edgeFactor + coronaOffset, 0.0, 1.0);
        
        // Apply power for sharpness
        float corona = pow(biasedEdge, coronaPower);
        
        // Apply width modifier
        corona = pow(corona, 1.0 / max(coronaWidth, 0.1));
        
        // Apply intensity
        float glow = corona * coronaIntensity;
        
        // Apply falloff
        glow *= exp(-coronaFalloff * (1.0 - corona));
        
        // Add corona glow to color (additive)
        color.rgb += coronaColor * glow;
    }

    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
