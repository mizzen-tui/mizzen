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
        boolean yCoordinateInverted = false;

        public Rotate(double thetaInDegrees) {
            super();
            this.theta = Math.toRadians(thetaInDegrees);
        }

        public int getTheta() {
            return (int) Math.toDegrees(theta);
        }

        public void setTheta(int thetaInDegrees) {
            this.theta = Math.toRadians(thetaInDegrees);
        }

        @Override
        public Vec2<?> transform(Vec2<?> position) {
            if (theta == 0f) {
                return position;
            }
            if (yCoordinateInverted) {
                return transformForInvertedYCoordinate(position);
            }
            float rotatedX = (float)(position.getX() * Math.cos(theta) - position.getY() * Math.sin(theta));
            float rotatedY = (float)(position.getY() * Math.cos(theta) + position.getX() * Math.sin(theta));
            return new Position.Simple(rotatedY, rotatedX);
        }

        public Vec2<?> transformForInvertedYCoordinate(Vec2<?> position) {
         //   System.out.println("USING CHATGPT NEW transform() METHOD");
            // Get the original X and Y positions
            float x = position.getX();
            float y = position.getY();

            // Apply standard rotation formula with the correct theta
            float rotatedX = (float) (x * Math.cos(theta) - y * Math.sin(theta));
            float rotatedY = (float) (x * Math.sin(theta) + y * Math.cos(theta));

            // Return the rotated point
            return new Position.Simple(rotatedY, rotatedX);
        }     
    }

    /**
     * Rotates a position around a fixed point
     */
    public static class RotateAroundFixedPoint implements PositionTransform {

        double theta;
        Vec2<?> fixedPoint;

        PositionTransform positionTransform;

        public RotateAroundFixedPoint(double theta, Vec2<?> fixedPoint) {
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

        public double getTheta() {
            return theta;
        }

        public void setTheta(double theta) {
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
