#version 150

#moj_import <minecraft:fog.glsl>

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec2 texCoord0;
in vec3 normal;

out vec4 fragColor;

void main() {
    // Sample texture (white.png for solid color)
    vec4 color = texture(Sampler0, texCoord0);
    
    // Apply vertex color and modulator
    color *= vertexColor * ColorModulator;
    
    // Apply light map
    color *= lightMapColor;
    
    // Apply fog
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
