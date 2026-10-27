package mizzen.geom;

import mizzen.geom.position.Vec2;

/**
 * A box in floats, corner (x1, y1) to corner (x2, y2) inclusive - e.g. a shape's bounds, a quad
 * tree's area. The float partner of Rectangle (whole character cells, rows and columns); between
 * them: Float2D.of(rectangle) and toRectangle().
 */
public interface Rectangle2D extends Dimension2D {

    float getX1();

    float getY1();

    float getX2();

    float getY2();

    /** the centre of a box of size d placed in the middle of this one; defaultPos if d does not fit */
    default Vec2<?> centered(Dimension2D d, Vec2<?> defaultPos) {
        if (!contains(d)) {
            return defaultPos;
        }
        long x = (long) (this.getWidth() - d.getWidth()) / 2 + (long) this.getX1();
        long y = (long) (this.getHeight() - d.getHeight()) / 2 + (long) this.getY1();
        return Vec2.xy(x, y);
    }

    default Vec2<?> centerPosition() {
        return centered(Dimension2D.ZERO, null);
    }

    /**
     * The character-cell rectangle: corner and size each cut to whole numbers (towards 0) - as the
     * engine's collision boxes have always been.
     */
    default Rectangle toRectangle() {
        return new Rectangle.Simple(this);
    }

    public static class Float2D implements Rectangle2D {

        float x1;
        float y1;
        float x2;
        float y2;

        public Float2D() {
        }

        public Float2D(float x1, float y1, float x2, float y2) {
            super();
            // TODO: should ensure that x1 < x2 and y1 < y2
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        public Float2D(Vec2<?> topLeft , Vec2<?> bottomRight) {
            this(topLeft.getX(), topLeft.getY(), bottomRight.getX(), bottomRight.getY());
        }

        /** the float box of a character-cell rectangle (its cells, corner to corner) */
        public static Float2D of(Rectangle r) {
            return new Float2D(r.getCol(), r.getRow(), r.getCol() + r.getWidth() - 1, r.getRow() + r.getHeight() - 1);
        }

        @Override
        public float getWidth() {
            return getX2() - getX1() + 1;
        }

        @Override
        public float getHeight() {
            return getY2() - getY1() + 1;
        }

        @Override
        public float getX1() {
            return x1;
        }

        @Override
        public float getY1() {
            return y1;
        }

        @Override
        public float getX2() {
            return x2;
        }

        @Override
        public float getY2() {
            return y2;
        }

        public void setX1(float x1) {
            this.x1 = x1;
        }

        public void setY1(float y1) {
            this.y1 = y1;
        }

        public void setX2(float x2) {
            this.x2 = x2;
        }

        public void setY2(float y2) {
            this.y2 = y2;
        }

        public static final Float2D ZERO = new Float2D(0,0,0,0);
    }
}
