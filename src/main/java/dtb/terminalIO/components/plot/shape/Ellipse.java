package dtb.terminalIO.components.plot.shape;

import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.components.plot.shape.Shape.AbstractShape;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.geom.position.PositionTransform;
import dtb.terminalIO.geom.position.Vec2;

public class Ellipse extends AbstractShape<Ellipse> {

    public static interface DrawEllipseAlgorithm {
        void draw(float x, float y, float width, float height, PointPlotter plot);
    }

    float x;
    float y;
    float width;
    float height;
    Color color;
    DrawEllipseAlgorithm algorithm;

    public Ellipse(float x, float y, float width, float height, Color color, DrawEllipseAlgorithm algorithm) {
        super();
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.color = color;
        this.algorithm = algorithm;
    }

    public Ellipse(float x, float y, float width, float height, Color color) {
        super();
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.color = color;
        this.algorithm = new EllipseAlgorithm();
    }

    public Ellipse(float x, float y, float width, float height) {
        super();
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.algorithm = new EllipseAlgorithm();
    }

    public Ellipse(Position centerPosition, float width, float height, Color color) {
        this(centerPosition.getX(), centerPosition.getY(), width, height, color);
    }

    protected Position getTransformedHeightWidth(float currentHeight, float currentWidth, PositionTransform positionTransform) {

        Vec2<?> center00 = new Vec2.BaseVec2(0,0);
        Vec2<?> currentHeightWidth = new Vec2.BaseVec2(currentWidth,currentHeight);

        Vec2<?> transFormedCenter00 = positionTransform.transform(center00);
        Vec2<?> transFormedRadiusVec = positionTransform.transform(currentHeightWidth);

        Vec2<?> newHeightWidth =  transFormedRadiusVec.sub(transFormedCenter00);

        return new Position.Simple(newHeightWidth);
    }
    
    

    @Override
    protected void drawWithTransform(PointPlotter pointPlotter, PositionTransform positionTransform) {
        Color lastColor = null;
        if (color != null) {
            lastColor = pointPlotter.setNextPlotColor(color);
        }

        Vec2<?> centerPoint = positionTransform.transform(x, y);

        Position heightWidth = getTransformedHeightWidth(height, width, positionTransform);

        this.algorithm.draw(centerPoint.getX(), centerPoint.getY(), heightWidth.getX(), heightWidth.getY(), pointPlotter);

        if (color != null) {
            pointPlotter.setNextPlotColor(lastColor);
        }

    }


}
