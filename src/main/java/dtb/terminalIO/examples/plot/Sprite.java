package dtb.terminalIO.examples.plot;

import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.components.plot.TransformPointPlotter;
import dtb.terminalIO.components.plot.XYTranslate;
import dtb.terminalIO.components.plot.shape.Shape;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.geom.position.PositionTransform.RotateAroundFixedPoint;

public abstract class Sprite implements Shape {


    XYTranslate delta = new XYTranslate(null, 0, 0);

    RotateAroundFixedPoint gridPointRotator;
    TransformPointPlotter gridRotatingPlotter;

    int drawCounter = 0;

    abstract protected Shape getCurrentShape();


    @Override
    public void draw(PointPlotter pointPlotter) {
        getRotatingPlotter().setPointPlotter(pointPlotter);
        getPointRotator().setTheta(getTheta());
        drawCounter++;
        drawShape(drawCounter, getCurrentShape(), gridRotatingPlotter);
    }

    abstract public void drawShape(int drawCounter, Shape currentShape, PointPlotter pointPlotter);

    Position spriteMidPoint() {

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

    abstract Rectangle calcBoundingRect();

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

    public int getTheta() {
        return getPointRotator().getTheta();
    }

    public void setTheta(int theta) {
        this.getPointRotator().setTheta(theta);
    }

}
