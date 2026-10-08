package dtb.terminalIO.components.plot.shape;

import java.util.ArrayList;
import java.util.List;

import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.components.plot.shape.Shape.AbstractShape;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.geom.position.PositionTransform;
import dtb.terminalIO.geom.position.Vec2;

/**
 * An ellipse: centred at x, y, width across and height up and down - wide or tall. Drawn as a
 * closed outline through points around it, transformed at those points: it rotates, scales and
 * moves like any shape, and can be filled (setFilled).
 * <p/>
 * Given a DrawEllipseAlgorithm, that draws it instead - unrotated (only its centre and size follow
 * the transform), e.g. EllipseAlgorithm, the dot-by-dot one this class used before.
 */
public class Ellipse extends AbstractShape<Ellipse> implements Fillable<Ellipse> {

    public static interface DrawEllipseAlgorithm {
        void draw(float x, float y, float width, float height, PointPlotter plot);
    }

    private static final int POINTS = 96;                   // around the outline: smooth at any size a terminal shows

    float x;
    float y;
    float width;
    float height;
    Color color;
    DrawEllipseAlgorithm algorithm;                         // null: the outline through points
    boolean filled = false;

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
        this(x, y, width, height, color, null);
    }

    public Ellipse(float x, float y, float width, float height) {
        this(x, y, width, height, null, null);
    }

    public Ellipse(Position centerPosition, float width, float height, Color color) {
        this(centerPosition.getX(), centerPosition.getY(), width, height, color);
    }

    @Override
    public Ellipse setFilled(boolean filled) {
        this.filled = filled;
        return this;
    }

    protected Position getTransformedHeightWidth(float currentHeight, float currentWidth, PositionTransform positionTransform) {

        Vec2<?> center00 = new Vec2.BaseVec2(0,0);
        Vec2<?> currentHeightWidth = new Vec2.BaseVec2(currentWidth,currentHeight);

        Vec2<?> transFormedCenter00 = positionTransform.transform(center00);
        Vec2<?> transFormedRadiusVec = positionTransform.transform(currentHeightWidth);

        Vec2<?> newHeightWidth =  transFormedRadiusVec.sub(transFormedCenter00);

        return new Position.Simple(newHeightWidth);
    }

    /** the outline: points around the ellipse, in its own units (before any transform) */
    List<Vec2<?>> outline() {
        List<Vec2<?>> points = new ArrayList<Vec2<?>>(POINTS);
        double a = width / 2.0, b = height / 2.0;
        for (int i = 0; i < POINTS; i++) {
            double t = 2 * Math.PI * i / POINTS;
            points.add(new Vec2.BaseVec2((float) (x + a * Math.cos(t)), (float) (y + b * Math.sin(t))));
        }
        return points;
    }

    @Override
    protected void drawWithTransform(PointPlotter pointPlotter, PositionTransform positionTransform) {
        Color lastColor = null;
        if (color != null) {
            lastColor = pointPlotter.setNextPlotColor(color);
        }

        if (algorithm != null) {
            Vec2<?> centerPoint = positionTransform.transform(x, y);
            Position heightWidth = getTransformedHeightWidth(height, width, positionTransform);
            this.algorithm.draw(centerPoint.getX(), centerPoint.getY(), heightWidth.getX(), heightWidth.getY(), pointPlotter);
        } else {
            Polygon around = new Polygon(outline());
            around.setFilled(filled);
            around.setPositionTransform(positionTransform);
            around.draw(pointPlotter);
        }

        if (color != null) {
            pointPlotter.setNextPlotColor(lastColor);
        }
    }
}
