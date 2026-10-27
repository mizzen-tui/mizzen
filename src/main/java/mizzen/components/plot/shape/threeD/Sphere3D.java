package mizzen.components.plot.shape.threeD;

import mizzen.components.plot.shape.CompositeShape;
import mizzen.components.plot.shape.Line;
import mizzen.components.plot.shape.Shape;
import mizzen.components.plot.shape.threeD.projection.Plot2D;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.geom.position.Vec2;
import mizzen.geom.position.Vec3;

public class Sphere3D extends AbstractShape3D<Sphere3D> {
    private final float radius;
    private final int slices, stacks;
    private final Color color;

    public Sphere3D(float radius, Vec3<?> center, int resolution, Color color) {
        this.radius = radius;
        this.center = center;
        this.slices = resolution; // Horizontal divisions
        this.stacks = resolution; // Vertical divisions
        this.color = color;
    }

    @Override
    public Shape<?> as2DShape(Plot2D projection) {
        CompositeShape sphere2D = new CompositeShape();

        // 1. Get the lighting context from the projection (The "Lens" properties)
        RenderContext ctx = projection.getRenderContext();
        Vec3<?> lightDir = ctx.getLightDir();

        Vec2<?>[][] pts = new Vec2[slices + 1][stacks + 1];
        float[][] intensity = new float[slices + 1][stacks + 1];

        for (int i = 0; i <= slices; i++) {
            double lon = 2 * Math.PI * i / slices;
            for (int j = 0; j <= stacks; j++) {
                double lat = Math.PI * j / stacks - Math.PI / 2;

                // 2. Base unit vector (This is the surface normal of a unit sphere)
                float nx = (float) (Math.cos(lat) * Math.cos(lon));
                float ny = (float) (Math.cos(lat) * Math.sin(lon));
                float nz = (float) (Math.sin(lat));
                Vec3.BaseVec3 normal = new Vec3.BaseVec3(nx, ny, nz);

                // 3. Rotate the normal so the lighting stays attached to the sphere as it spins
                Vec3<?> rotatedNormal = Vec3Utils.rotateVector(normal, (float) rotX, (float) rotY, (float) rotZ);

                // 4. Calculate Intensity for this point
                intensity[i][j] = Vec3Utils.calculateIntensity(rotatedNormal, lightDir);

                // 5. Calculate world position of the point
                // We scale the unit normal by radius to get the actual surface point
                Vec3.BaseVec3 localPoint = new Vec3.BaseVec3(nx * radius, ny * radius, nz * radius);
                Vec3<?> rotatedLocal = Vec3Utils.rotateVector(localPoint, (float) rotX, (float) rotY, (float) rotZ);

                Vec3.BaseVec3 worldPoint = new Vec3.BaseVec3(
                        rotatedLocal.getX() + center.getX(),
                        rotatedLocal.getY() + center.getY(),
                        rotatedLocal.getZ() + center.getZ());

                pts[i][j] = projection.to2D(worldPoint);
            }
        }

        // 6. Draw segments using the intensity values
        for (int i = 0; i < slices; i++) {
            for (int j = 0; j < stacks; j++) {
                int nextI = (i + 1) % slices;
                int nextJ = j + 1;

                drawLitSegment(sphere2D, pts[i][j], pts[nextI][j], intensity[i][j], intensity[nextI][j]);
                if (nextJ <= stacks) {
                    drawLitSegment(sphere2D, pts[i][j], pts[i][nextJ], intensity[i][j], intensity[i][nextJ]);
                }
            }
        }

        return sphere2D;
    }

    private void drawLitSegment(CompositeShape shape, Vec2<?> p1, Vec2<?> p2, float i1, float i2) {
        // Average the intensity of the two vertices
        float avgIntensity = (i1 + i2) / 2.0f;

        // Choose color: If lit, use primary color; if in shadow, use a dimmed color
        // Threshold (0.3f) can be tuned to change how much of the sphere is "dark"
        Color segmentColor = (avgIntensity > 0.3f) ? this.color : Colors.IBla;

        shape.addShape(new Line(p1, p2, segmentColor));
    }

    @Override
    public String getName() {
        return "Sphere3D";
    }
}