package mizzen.components.plot.shape;

import mizzen.components.plot.PointPlotter;
import mizzen.components.plot.PointPlotter.PointPlot;
import mizzen.components.plot.shape.Shape.AbstractShape;
import mizzen.controlcodes.color.Color;
import mizzen.geom.AsRectangle2D;
import mizzen.geom.Rectangle2D;
import mizzen.geom.position.PositionTransform;
import mizzen.geom.position.Vec2;

/**
 * Draws a circle {@link Shape}
 *
 * <pre>
 * 
 *  ASCII ( non braille ) rendering of a circle
 *
 *             ._/-----__ 
 *         ./r+`         -*c_
 *       *r`                 \\.
 *     .)`                     +\
 *    :+                        '\
 *   .[                          '|
 *   *                            :.
 *   |                             |
 *   |                             |
 *   \                            ::
 *   ::                           /
 *    7:                         j`
 *     :\                      .)
 *      '\c                  ./+
 *         \\c.           _+-`
 *            -\-\--------`
 * </pre>
 * 
 * When filled is on - circle will draw filled in
 * 
 * like this ..
 * <pre>
 * 
 *  ASCII ( non braille ) rendering of a circle with filled=true
 * 
 *             ._******__
 *         .**############**_
 *       **##################**.
 *     .d#######################c
 *    :##########################*
 *   .############################L
 *   *#############################.
 *   ##############################|
 *   ##############################|
 *   ?#############################:
 *   :############################P
 *    7###########################`
 *     :@#######################P
 *      '9#####################+
 *         +9###############"`
 *            -+"9#####""-`
 * 
 * </pre>
 * 
 * 
 * @author dbalme
 *
 */
public class Circle extends AbstractShape<Circle> implements Fillable<Circle>, AsRectangle2D.AsFloat2D {

    /**
     * Specifies how to draw the circle,
     *    outline, solid, donut ..
     */
    @FunctionalInterface
    public static interface DrawCircleAlgorithm {
        void draw(int x, int y, int radius, PointPlotter plot);
    }

    public static class CompositeAlgo implements DrawCircleAlgorithm {

        DrawCircleAlgorithm algo1;
        DrawCircleAlgorithm algo2;

        public CompositeAlgo(DrawCircleAlgorithm algo1, DrawCircleAlgorithm algo2) {
            super();
            this.algo1 = algo1;
            this.algo2 = algo2;
        }

        @Override
        public void draw(int x, int y, int radius, PointPlotter plot) {
            algo1.draw(x, y, radius, plot);
            algo2.draw(x, y, radius, plot);
        }
    }

    float x;
    float y;
    float radius;
    Color color;
    DrawCircleAlgorithm algorithm;

    public Circle(float x, float y, float radius) {
        super();
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.algorithm = new CircleAlgorithm();
    }

    public Circle(int x, int y, int radius) { 
        this((float)x, (float) y, (float) radius);
    }

    public Circle(PointPlot middle, float radius) {
        this(middle.x, middle.y, radius);
    }

    public Circle(PointPlot middle, float radius, Color color) {
        this(middle.x, middle.y, radius);
        this.color = color;
    }

    public Circle(float x, float y, float radius, Color color) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.color = color;
        this.algorithm = new CircleAlgorithm();
    }


    public Circle(int x, int y, int radius, Color color) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.color = color;
        this.algorithm = new CircleAlgorithm();
    }

    public Circle setAlgorithm(DrawCircleAlgorithm algorithm) {
        this.algorithm = algorithm;
        return this;
    }

    public Circle setFilled(boolean filled) {
        // assume simple circle .. might need to fix this to be smarter
        return setAlgorithm(new CircleAlgorithm(filled));
    }
    
    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getRadius() {
        return radius;
    }

    public PointPlot getMidPoint() {
        return new PointPlot(x, y);
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    protected float getTransformedRadius(float currentRadius, PositionTransform positionTransform) {
        float scaledRadius;

        Vec2<?> center00 = new Vec2.BaseVec2(0,0);
        Vec2<?> radiusVec = new Vec2.BaseVec2(0,currentRadius);

        float prevRadiusSquared = currentRadius * currentRadius;

        Vec2<?> transFormedCenter00 = positionTransform.transform(center00);
        Vec2<?> transFormedRadiusVec = positionTransform.transform(radiusVec);

        float newRadiusSquared =  transFormedRadiusVec.sub(transFormedCenter00) .lengthSquared();

        if (prevRadiusSquared != newRadiusSquared) {
            scaledRadius = (float)Math.sqrt( newRadiusSquared );
        } else {
            scaledRadius = currentRadius;
        }

        return scaledRadius;
    }

    protected void drawWithTransform(PointPlotter pointPlotter, PositionTransform positionTransform) {
        Color lastColor = null;
        if (color != null) {
            lastColor = pointPlotter.setNextPlotColor(color);
        }

        Vec2<?> centerPoint = positionTransform.transform(x, y);

        float scaledRadius = getTransformedRadius(radius, positionTransform);

        this.algorithm.draw(Math.round(centerPoint.getX()), 
                            Math.round(centerPoint.getY()), 
                            Math.round(scaledRadius), 
                            pointPlotter);

        if (color != null) {
            pointPlotter.setNextPlotColor(lastColor);
        }
    }

    @Override
    public Rectangle2D.Float2D getRectangle2D() {
        Vec2<?> centerPoint = positionTransform.transform(x, y);
        float scaledRadius = getTransformedRadius(radius, positionTransform);
        
        Vec2<?> topLeft      =  centerPoint.sub(new Vec2.BaseVec2(scaledRadius, 0)).sub(new Vec2.BaseVec2(0, scaledRadius));
        Vec2<?> bottomRight  =  centerPoint.offsetAdd(new Vec2.BaseVec2(scaledRadius, 0)).offsetAdd(new Vec2.BaseVec2(0, scaledRadius));

        return new Rectangle2D.Float2D(topLeft, bottomRight) ;
    }


}
