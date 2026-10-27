package mizzen.components.plot.shape;

import mizzen.components.plot.PointPlotter;
import mizzen.components.plot.shape.Shape.AbstractShape;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.ColorProvider;
import mizzen.geom.Rectangle;
import mizzen.geom.position.PositionTransform;
import mizzen.geom.position.Vec2;

public class RectangleShape extends AbstractShape<RectangleShape> implements Rectangle, Fillable<RectangleShape>, ColorProvider {

    Rectangle rectangle;
    Color color;
    DrawRectangleAlgorithm algorithm;

    /**
     * Specifies how to draw the rectangle,
     *    outline, solid
     */
    @FunctionalInterface
    public static interface DrawRectangleAlgorithm {
        void draw(Vec2<?> topLeft, Vec2<?> topRight, Vec2<?> bottomRight, Vec2<?> bottomLeft, Color color, PointPlotter plotter);
    }

    static DrawRectangleAlgorithm drawOutline = new DrawRectangleAlgorithm() {
        @Override
        public void draw(Vec2<?> topLeft, Vec2<?> topRight, Vec2<?> bottomRight, Vec2<?> bottomLeft, Color color, PointPlotter plotter) {
            Polygon polygon = new Polygon();
            polygon.addPoint(topLeft, color);
            polygon.addPoint(topRight, color);
            polygon.addPoint(bottomRight, color);
            polygon.addPoint(bottomLeft, color);
            polygon.draw(plotter);
        }
    };

    static DrawRectangleAlgorithm drawFilled = new DrawRectangleAlgorithm() {
        @Override
        public void draw(Vec2<?> topLeft, Vec2<?> topRight, Vec2<?> bottomRight, Vec2<?> bottomLeft, Color color, PointPlotter plotter) {

            // A filled rectangle is really just two filled triangles,
            Triangle.drawFilled.draw(topLeft, topRight, bottomLeft, color, plotter);
            Triangle.drawFilled.draw(topRight, bottomRight, bottomLeft, color, plotter);
        }
    };

    public RectangleShape(Rectangle rect, Color color) {
        this.rectangle = rect;
        this.color = color;
        this.algorithm = drawOutline;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public RectangleShape(Rectangle rect) {
        this(rect, null);
    }
    

    public Rectangle getRectangle() {
        return rectangle;
    }

    @Override
    public RectangleShape setFilled(boolean filled) {
        this.algorithm = (filled) ? drawFilled : drawOutline;
        return this;
    }

    @Override
    protected void drawWithTransform(PointPlotter pointPlotter, PositionTransform positionTransform) {
        //new Rectangle.Simple(row, col, height, width)
        this.algorithm.draw(positionTransform.transform(rectangle.getPostion_TopLeft()), 
                            positionTransform.transform(rectangle.getPostion_TopRight()), 
                            positionTransform.transform(rectangle.getPostion_BottomRight()), 
                            positionTransform.transform(rectangle.getPostion_BottomLeft()), 
                            color, 
                            pointPlotter);
    }

    @Override
    public int getRow() {
        return rectangle.getRow();
    }

    @Override
    public int getCol() {
        return rectangle.getCol();
    }

    @Override
    public int getWidth() {
        return rectangle.getWidth();
    }

    @Override
    public int getHeight() {
        return rectangle.getHeight();
    }

    @Override
    public void moveTo(int row, int col) {
        rectangle.moveTo(row, col);
    }


}
