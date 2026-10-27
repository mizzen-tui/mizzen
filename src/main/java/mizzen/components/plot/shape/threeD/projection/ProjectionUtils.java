package mizzen.components.plot.shape.threeD.projection;

import mizzen.geom.position.Vec2;

public class ProjectionUtils {
    /**
     * Centralized math for face visibility using the cross-product winding check.
     */
    public static boolean isFaceVisible(Vec2<?>[] pts, int[] face) {
        Vec2<?> a = pts[face[0]];
        Vec2<?> b = pts[face[1]];
        Vec2<?> c = pts[face[2]];

        float abx = b.getX() - a.getX();
        float aby = b.getY() - a.getY();
        float acx = c.getX() - a.getX();
        float acy = c.getY() - a.getY();

        return (abx * acy - aby * acx) > 0;
    }
}