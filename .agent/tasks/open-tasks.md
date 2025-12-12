# Open Tasks

## 1. Menu State Resets on Reopen (NEW - INVESTIGATING)
When closing and reopening the menu, all settings reset to defaults.
Need to find where state persistence should happen.

## 2. Shape Renderers Broken (NEW)
Several shapes are not rendering correctly:

### Completely Broken:
- Prism
- Cylinder  
- Cube
- Dodecahedron
- Icosahedron

### Possibly Regressed:
- Tetrahedron (was working before)
- Octahedron (was working before)

### Incomplete Implementation:
- Torus
- Capsule
- Cone

## Next Steps
1. Investigate menu state reset issue - find where state is created/restored
2. Check shape renderers for missing implementations or renderer registration
