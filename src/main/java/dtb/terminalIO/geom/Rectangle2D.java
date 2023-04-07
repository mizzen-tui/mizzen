package dtb.terminalIO.geom;

import dtb.terminalIO.geom.position.Vec2;

public interface Rectangle2D<T extends Number> extends Dimension2D<T>{

    T getX1();
    T getY1();
    T getX2();
    T getY2();

    Vec2<?> newFrom(T x, T y);
    T castFromLong(long l);
    default long toLong(T n) {
        return n.longValue();
    }

    public default Vec2<?> centered(Dimension2D<?> d, Vec2<?> defaultPos) {
        if (!contains(d)) {
            return defaultPos;
        }
        
        long x = (this.getWidth().longValue()  - d.getWidth().longValue()  )/2 + this.getX1().longValue();
        long y = (this.getHeight().longValue() - d.getHeight().longValue() )/2 + this.getY1().longValue();
        
        return newFrom(castFromLong(x), castFromLong(y));
   }

    public default Vec2<?> centerPosition() {
        return centered(Dimension2D.ZERO, null);
   }

    public static class Float2D implements Rectangle2D<Float> {

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

        
        @Override
        public Float getWidth() {
            return getX2() - getX1() + 1;
        }

        @Override
        public Float getHeight() {
            return getY2() - getY1() + 1;
        }

        @Override
        public Float getX1() {
            return x1;
        }

        @Override
        public Float getY1() {
            return y1;
        }

        @Override
        public Float getX2() {
            return x2;
        }

        @Override
        public Float getY2() {
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

        @Override
        public Vec2<?> newFrom(Float x, Float y) {
            return new Vec2.BaseVec2(x,y);
        }

        @Override
        public Float castFromLong(long l) {
            return (float) l;
        }

        public static final Float2D ZERO = new Float2D(0,0,0,0);

    }

}
