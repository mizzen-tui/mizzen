package mizzen.components.plot.shape.threeD;

import mizzen.controlcodes.color.ColorProvider;
import mizzen.components.plot.shape.CompositeShape;
import mizzen.components.plot.shape.Line;
import mizzen.components.plot.shape.Shape;

import mizzen.controlcodes.color.Color;
import mizzen.geom.position.Vec2;
import mizzen.geom.position.Vec3;
import mizzen.components.plot.shape.threeD.projection.Plot2D;
import mizzen.components.plot.shape.threeD.projection.ProjectionUtils;

public class Cube3D extends AbstractShape3D<Cube3D> {
    private final int size;
    private final Vec3<?> center;
    private final ColorProvider outside, inside;     // null: no colour - the plotter's

    /**
     * the edges in front in out, the hidden ones in in - asked each time it is drawn: fixed colours (a Color is a
     * ColorProvider), or ones a component hands out that follow its theme:
     * <pre>
     *     new Cube3D(50, at, Colors.BYel, Colors.BBlu);
     *     new Cube3D(50, at, grid.drawingColor(YELLOW), grid.themeColor(Theme::dimColor));
     * </pre>
     */
    public Cube3D(int size, Vec3<?> center, ColorProvider out, ColorProvider in) {
        this.size = size;
        this.center = center;
        this.outside = out;
        this.inside = in;
    }

    @Override
    public Shape<?> as2DShape(Plot2D projection) {
        float h = size / 2f;
        float cx = center.getX(), cy = center.getY(), cz = center.getZ();

        // Define local space vertices
        // Vec3<?>[] raw = new Vec3[] {
        //     new Vec3.BaseVec3(cx-h, cy-h, cz-h), // 0: ulf
        //     new Vec3.BaseVec3(cx+h, cy-h, cz-h), // 1: urf
        //     new Vec3.BaseVec3(cx-h, cy+h, cz-h), // 2: llf
        //     new Vec3.BaseVec3(cx+h, cy+h, cz-h), // 3: lrf
        //     new Vec3.BaseVec3(cx-h, cy-h, cz+h), // 4: ulb
        //     new Vec3.BaseVec3(cx+h, cy-h, cz+h), // 5: urb
        //     new Vec3.BaseVec3(cx-h, cy+h, cz+h), // 6: llb
        //     new Vec3.BaseVec3(cx+h, cy+h, cz+h)  // 7: lrb
        // };

        Vec3<?>[] raw = new Vec3[] {
            new Vec3.BaseVec3(cx-h, cy-h, cz+h), // 0: ulb (Back)
            new Vec3.BaseVec3(cx+h, cy-h, cz+h), // 1: urb (Back)
            new Vec3.BaseVec3(cx-h, cy+h, cz+h), // 2: llb (Back)
            new Vec3.BaseVec3(cx+h, cy+h, cz+h), // 3: lrb (Back)
            new Vec3.BaseVec3(cx-h, cy-h, cz-h), // 4: ulf (Front)
            new Vec3.BaseVec3(cx+h, cy-h, cz-h), // 5: urf (Front)
            new Vec3.BaseVec3(cx-h, cy+h, cz-h), // 6: llf (Front)
            new Vec3.BaseVec3(cx+h, cy+h, cz-h)  // 7: lrf (Front)
        };

        // 1. Rotate vertices using Vec3.UTIL
        Vec2<?>[] pts = new Vec2[8];
        for (int i = 0; i < 8; i++) {
            Vec3<?> rotated = Vec3Utils.rotateVector(raw[i], (float)rotX, (float)rotY, (float)rotZ);
            pts[i] = projection.to2D(rotated);
        }

        // 2. Define edges and faces (as in your original logic)
        int[][] edges = {{0,1},{0,2},{1,3},{2,3},{4,5},{4,6},{5,7},{6,7},{4,0},{5,1},{6,2},{7,3}};
        int[][] faces = {{4,5,7,6}, {1,0,2,3}, {0,1,5,4}, {6,7,3,2}, {0,4,6,2}, {5,1,3,7}};

        // 3. Visibility check
        boolean[][] visibleEdge = new boolean[8][8];
        for (int[] face : faces) {
            if (ProjectionUtils.isFaceVisible(pts, face)) {
                for (int i = 0; i < 4; i++) {
                    visibleEdge[face[i]][face[(i + 1) % 4]] = true;
                    visibleEdge[face[(i + 1) % 4]][face[i]] = true;
                }
            }
        }

        CompositeShape cube2D = new CompositeShape();
        Color out = outside == null ? null : outside.getColor();   // once per drawing, not per edge
        Color in = inside == null ? null : inside.getColor();
        for (int[] e : edges) {
            boolean visible = visibleEdge[e[0]][e[1]];
            cube2D.addShape(new Line(pts[e[0]], pts[e[1]], visible ? out : in));
        }
        return cube2D;
    }
}