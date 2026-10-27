package mizzen.components.plot.shape.threeD.projection;

import mizzen.components.plot.shape.threeD.RenderContext;
import mizzen.geom.position.Vec2;
import mizzen.geom.position.Vec3;

@FunctionalInterface
public interface Plot2D {
    Vec2<?> to2D(Vec3<?> position3D);
    
    // Default context provider; no changes needed to existing simple implementations
    default RenderContext getRenderContext() {
        return RenderContext.DEFAULT;
    }
}