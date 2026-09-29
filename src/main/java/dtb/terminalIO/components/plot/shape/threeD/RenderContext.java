package dtb.terminalIO.components.plot.shape.threeD;

import dtb.terminalIO.geom.position.Vec3;

public class RenderContext {
    // A sensible default for terminal lighting (pointing from top-right-front)
    public static final RenderContext DEFAULT = new RenderContext(new Vec3.BaseVec3(1, 1, 1));
    
    private final Vec3<?> lightDir;
    // You can easily add ambient, fog, etc., here later
    
    public RenderContext(Vec3<?> lightDir) {
        this.lightDir = lightDir;
    }

    public Vec3<?> getLightDir() { return lightDir; }
}