package mizzen.components.plot.shape;

import java.util.Collection;
import java.util.function.Function;

import mizzen.components.HasName;
import mizzen.components.plot.PointPlotter;
import mizzen.components.plot.PointPlotterAsRectangle;
import mizzen.components.theme.Theme;
import mizzen.controlcodes.color.Color;
import mizzen.geom.AsRectangle2D;
import mizzen.geom.Rectangle2D;
import mizzen.geom.Rectangle2D.Float2D;
import mizzen.geom.position.PositionTransform;

/**
 * An object (shape: ie circle, square, line, polygon etc) that can draw itself into a
 * {@link PointPlotter}
 */
public interface Shape<T extends Shape<?>> extends HasName<T> {

    /**
     * Draw the shape given a pointPlotter
     * 
     * @param pointPlotter
     */
    public void draw(PointPlotter pointPlotter);

    PositionTransform getPositionTransform();

    /**
     * The box around the shape as it draws now - its transform applied (a rotated shape's box is
     * generally bigger than its unrotated one). Shapes that know their corners work it out from
     * them; any other is drawn into a box-measuring plotter.
     */
    default Rectangle2D.Float2D bounds() {
        return UTIL.getAsRectangle(this);
    }

    void setPositionTransform(PositionTransform positionTransform);

    public static abstract class AbstractShape<T extends AbstractShape<?>> implements Shape<T> {

        PositionTransform positionTransform = PositionTransform.NOOP; 
        String name;
        Function<Theme, Color> themeColor;              // null: none - its own colour, else the plotter's

        public AbstractShape() {
            this.setName(HasName.UTIL.deriveName(this));
        }

        // given a theme colour: worked out once per draw, from the theme of what it plots into, and set on the plotter
        // around its drawing (a shape's own colour, set by the shape as it draws, still wins). Not per point.
        @Override
        final public void draw(PointPlotter pointPlotter) {
            if (themeColor == null) {                       // nearly every shape: drawn as before, nothing made
                drawWithTransform(pointPlotter, getPositionTransform());
                return;
            }
            UTIL.drawInThemeColor(pointPlotter, themeColor, () -> drawWithTransform(pointPlotter, getPositionTransform()));
        }

        /**
         * Draws in a colour of its theme - the theme of what it is plotted into, worked out as it is drawn, so a theme
         * switch reaches it. Its own colour, if it has one, still wins.
         * <pre>
         *     new Circle(0, 0, 10).setThemeColor((t) -&gt; t.drawingColor(YELLOW));
         *     new Line(a, b, null).setThemeColor(Theme::dimColor);
         * </pre>
         */
        @SuppressWarnings("unchecked")
        public T setThemeColor(Function<Theme, Color> themeColor) {
            this.themeColor = themeColor;
            return (T) this;
        }

        public Function<Theme, Color> getThemeColor() {
            return themeColor;
        }


        abstract protected void drawWithTransform(PointPlotter pointPlotter, PositionTransform positionTransform);

        @Override
        public PositionTransform getPositionTransform() {
            return positionTransform;
        }

        @Override
        public void setPositionTransform(PositionTransform positionTransform) {
            this.positionTransform = positionTransform;
        }


        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public T setName(String name) {
            this.name = name;
            return (T) this;
        }

    }
    
    /** 
     * A useful base class decorator for shapes
     */
    public static class Delegate implements Shape<Delegate> {

        Shape<?> shape;

        public Delegate(Shape<?> shape) {
            super();
            this.shape = shape;
        }

        public Shape<?> getShape() {
            return shape;
        }

        public void setShape(Shape<?> shape) {
            this.shape = shape;
        }

        @Override
        public PositionTransform getPositionTransform() {
            return this.getShape().getPositionTransform();
        }

        @Override
        public void setPositionTransform(PositionTransform positionTransform) {
            this.getShape().setPositionTransform(positionTransform);
        }

        @Override
        public void draw(PointPlotter pointPlotter) {
            this.getShape().draw(pointPlotter);
        }

        @Override
        public String getName() {
            return getShape().getName();
        }

        @Override
        public Delegate setName(String name) {
            getShape().setName(name);
            return this;
        }

    }

    public static class UTIL {

        /**
         * Runs draw with the plotter's next colour set to themeColor in the plotter's theme, then puts back what it was -
         * worked out once per shape drawn, not per point. themeColor null: draw as it is.
         */
        public static void drawInThemeColor(PointPlotter pointPlotter, Function<Theme, Color> themeColor, Runnable draw) {
            if (themeColor == null) {
                draw.run();
                return;
            }
            Color before = pointPlotter.setNextPlotColor(themeColor.apply(pointPlotter.getTheme()));
            try {
                draw.run();
            } finally {
                pointPlotter.setNextPlotColor(before);
            }
        }

        static public Rectangle2D.Float2D getAsRectangle(Shape<?> shape) {

            PointPlotterAsRectangle calculatedRectanglePlotter = new PointPlotterAsRectangle();
            calculateAndGrowRectangleFromShape(calculatedRectanglePlotter, shape);
            return calculatedRectanglePlotter.getRectangle2D();

        }

        static public Rectangle2D.Float2D getAsRectangle(Collection<Shape<?>> shapes) {

            PointPlotterAsRectangle calculatedRectanglePlotter = new PointPlotterAsRectangle();

            for (Shape<?> shape : shapes) {
                calculateAndGrowRectangleFromShape(calculatedRectanglePlotter, shape);
            }

            return calculatedRectanglePlotter.getRectangle2D();

        }

        static private void calculateAndGrowRectangleFromShape(PointPlotterAsRectangle calculatedRectanglePlotter, Shape<?> shape) {

            if (shape instanceof AsRectangle2D.AsFloat2D) {
                Float2D sizeOfShape = (Rectangle2D.Float2D) ((AsRectangle2D.AsFloat2D)shape).getRectangle2D(); 
                calculatedRectanglePlotter.plot(sizeOfShape.getX1(), sizeOfShape.getY1());
                calculatedRectanglePlotter.plot(sizeOfShape.getX2(), sizeOfShape.getY2());
                return;
            }

            shape.draw(calculatedRectanglePlotter);

        }

    }

}
