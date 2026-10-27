package mizzen.examples.plot;

import mizzen.components.plot.PointPlotter;
import mizzen.components.plot.TransformPointPlotter;
import mizzen.components.plot.XYTranslate;
import mizzen.components.plot.shape.Shape.AbstractShape;
import mizzen.geom.Rectangle;
import mizzen.geom.position.Position;
import mizzen.geom.position.PositionTransform;
import mizzen.geom.position.PositionTransform.RotateAroundFixedPoint;

// TODO: this should not be under examples!
public abstract class RotatingShape<T extends RotatingShape<?>> extends AbstractShape<T> {


    XYTranslate delta = new XYTranslate(null, 0, 0);

    RotateAroundFixedPoint gridPointRotator;
    TransformPointPlotter gridRotatingPlotter;

    int drawCounter = 0;


    @Override
    protected void drawWithTransform(PointPlotter pointPlotter, PositionTransform positionTransform) {
        getRotatingPlotter().setPointPlotter(pointPlotter);
        getPointRotator().setTheta(getTheta());
        drawCounter++;
        drawShape(drawCounter, gridRotatingPlotter);
    }

    abstract public void drawShape(int drawCounter, PointPlotter pointPlotter);

    protected Position spriteMidPoint() {

        Rectangle rect = calcBoundingRect();
        if (rect == null) {
            throw new NullPointerException("BoundingRect not defined.  Implement calcBoundingRect()");
        }

        return new Position() {
                @Override
                public int getCol() {
                    return calcBoundingRect().centerPosition().getCol();
                }
                @Override
                public int getRow() {
                    return calcBoundingRect().centerPosition().getRow();
                }
            };
    }

    public abstract Rectangle calcBoundingRect();

    protected RotateAroundFixedPoint getPointRotator() {
        if (gridPointRotator == null) {
           gridPointRotator = new RotateAroundFixedPoint(0, spriteMidPoint());
           gridRotatingPlotter = new TransformPointPlotter(null, gridPointRotator);
        }
        return gridPointRotator;
    }

    protected TransformPointPlotter getRotatingPlotter() {
        getPointRotator();
        return gridRotatingPlotter;
    }

    public double getTheta() {
        return getPointRotator().getTheta();
    }

    public void setTheta(int theta) {
        this.getPointRotator().setTheta(theta);
    }

}
