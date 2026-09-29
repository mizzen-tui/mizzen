package dtb.terminalIO.components.plot.shape.threeD.projection;

import dtb.terminalIO.components.plot.shape.threeD.RenderContext;
import dtb.terminalIO.geom.position.Vec2;
import dtb.terminalIO.geom.position.Vec3;

@FunctionalInterface
public interface Plot2D {
    Vec2<?> to2D(Vec3<?> position3D);
    
    // Default context provider; no changes needed to existing simple implementations
    default RenderContext getRenderContext() {
        return RenderContext.DEFAULT;
    }
}