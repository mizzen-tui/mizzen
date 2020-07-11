package dtb.terminalIO.components.plot.shape;

import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.components.plot.PointPlotter.PointPlot;
import dtb.terminalIO.controlcodes.ControlCode.Color;

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
public class Circle implements Shape {

    /**
     * Specifies how to draw the circle
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

    int x;
    int y;
    int radius;
    Color color;
    DrawCircleAlgorithm algorithm;

    public Circle(int x, int y, int radius) {
        super();
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.algorithm = new CircleAlgorithm();
    }
    
    public Circle(PointPlot middle, int radius) {
        this(middle.x, middle.y, radius);
    }

    public Circle(PointPlot middle, int radius, Color color) {
        this(middle.x, middle.y, radius);
        this.color = color;
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
    
    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getRadius() {
        return radius;
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

    public void draw(PointPlotter pointPlotter) {
        Color lastColor = null;
        if (color != null) {
            lastColor = pointPlotter.setColor(color);
        }

        this.algorithm.draw(x, y, radius, pointPlotter);

        if (color != null) {
            pointPlotter.setColor(lastColor);
        }
    }

}
