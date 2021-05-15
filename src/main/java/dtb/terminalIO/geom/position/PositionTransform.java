package dtb.terminalIO.geom.position;

/**
 * Transforms Position objects from one location to another.
 * 
 * <p/>
 * 
 * Useful when moving things about the screen
 * 
 */
public interface PositionTransform {

    Vec2<?> transform(Vec2<?> position);

    default Vec2<?> transform(float x1, float y1) {
        return transform(new Position.Simple(y1,x1));
    }


    public static final PositionTransform NOOP = new PositionTransform() {
        @Override
        public Vec2<?> transform(Vec2<?> position) {
            return position;
        }
    };

    /**
     * Perform transforms in a FofG (F of G ) like manner..
     * 
     * Useful when you want to make complex transforms from simple transforms.
     *
     */
    public static class FofG implements PositionTransform {

        PositionTransform f_transform;
        PositionTransform g_transform;

        public FofG(PositionTransform f_transform, PositionTransform g_transform) {
            super();
            this.f_transform = f_transform;
            this.g_transform = g_transform;
        }

        @Override
        public Vec2<?>  transform(Vec2<?>  position) {
            return f_transform.transform(g_transform.transform(position));
        }

        public PositionTransform getF_transform() {
            return f_transform;
        }

        public PositionTransform getG_transform() {
            return g_transform;
        }
    }

    public static class Translate implements PositionTransform {
        float rowDelta;
        float colDelta;

        public Translate(float rowDelta, float colDelta) {
            super();
            this.rowDelta = rowDelta;
            this.colDelta = colDelta;
        }

        public Translate(int rowDelta, int colDelta) {
            this((float) rowDelta, (float) colDelta);
        }

        public Translate(Vec2<?>  delta) {
            this(delta.getY(), delta.getX());
        }

        public float getRowDelta() {
            return rowDelta;
        }

        public void setRowDelta(float rowDelta) {
            this.rowDelta = rowDelta;
        }

        public float getColDelta() {
            return colDelta;
        }

        public void setColDelta(float colDelta) {
            this.colDelta = colDelta;
        }

        @Override
        public Vec2<?>  transform(Vec2<?>  position) {
            return position.add(colDelta, getRowDelta());
        }

    }

    /**
     * Rotates a position around the origin
     */
    public static class Rotate implements PositionTransform {

        double theta; // in radians

        public Rotate(int thetaInDegrees) {
            super();
            this.theta = Math.toRadians(thetaInDegrees);
        }

        public int getTheta() {
            return (int) Math.toDegrees(theta);
        }

        public void setTheta(int thetaInDegrees) {
            this.theta = Math.toRadians(thetaInDegrees);
            ;
        }

        @Override
        public Vec2<?> transform(Vec2<?> position) {
            if (theta == 0f) {
                return position;
            }
            int rotatedX = (int) (position.getX() * Math.cos(theta) - position.getY() * Math.sin(theta));
            int rotatedY = (int) (position.getY() * Math.cos(theta) + position.getX() * Math.sin(theta));
            return new Position.Simple(rotatedY, rotatedX);
        }
    }

    /**
     * Rotates a position around a fixed point
     */
    public static class RotateAroundFixedPoint implements PositionTransform {

        int theta;
        Vec2<?> fixedPoint;

        PositionTransform positionTransform;

        public RotateAroundFixedPoint(int theta, Vec2<?> fixedPoint) {
            super();
            this.theta = theta;
            this.fixedPoint = fixedPoint;

            makeTransform();
        }

        public RotateAroundFixedPoint(Integer theta, Vec2<?> fixedPoint) {
            // autoboxing issues - that's why this
            this( theta == null ? 0 : theta, fixedPoint);
        }

        private void makeTransform() {
            // This transform will translate position relative to origin,
            // then perform the rotate and then translate back to fixed point
            positionTransform = new FofG(new Translate(fixedPoint),
                                         new FofG(new Rotate(theta), new Translate(fixedPoint.negate())));
        }

        public int getTheta() {
            return theta;
        }

        public void setTheta(int theta) {
            this.theta = theta;
            makeTransform();
        }

        public Vec2<?> getFixedPoint() {
            return fixedPoint;
        }

        public void setFixedPoint(Position fixedPoint) {
            this.fixedPoint = fixedPoint;
            makeTransform();
        }

        @Override
        public Vec2<?> transform(Vec2<?> position) {
            return positionTransform.transform(position);
        }

    }
    
    public static class Scale implements PositionTransform {

        Double scaleFactorRow;
        Double scaleFactorCol;

        public Scale(Double scaleFactorRow, Double scaleFactorCol) {
            super();
            this.scaleFactorRow = scaleFactorRow;
            this.scaleFactorCol = scaleFactorCol;
        }

        public Scale(Double scaleFactor) {
            this(scaleFactor, scaleFactor);
        }

        @Override
        public Vec2<?> transform(Vec2<?> position) {
            return new Position.Simple((float)(position.getY()*this.scaleFactorRow), 
                                       (float)(position.getX()*this.scaleFactorCol));
        }

    }


}
