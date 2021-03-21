package dtb.terminalIO.components.plot.shape;

import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.geom.Rectangle;

public class RectangleShape implements Shape, Rectangle, Fillable<RectangleShape> {

    Rectangle rectangle;
    Color color;
    DrawRectangleAlgorithm algorithm;

    /**
     * Specifies how to draw the rectangle,
     *    outline, solid
     */
    @FunctionalInterface
    public static interface DrawRectangleAlgorithm {
        void draw(Rectangle rectangle, Color color, PointPlotter plotter);
    }
    
    static DrawRectangleAlgorithm drawOutline = new DrawRectangleAlgorithm() {
        @Override
        public void draw(Rectangle rectangle, Color color, PointPlotter plotter) {
            Polygon polygon = new Polygon();
            polygon.addPoint(rectangle.getPostion_TopLeft(), color);
            polygon.addPoint(rectangle.getPostion_TopRight(), color);
            polygon.addPoint(rectangle.getPostion_BottomRight(), color);
            polygon.addPoint(rectangle.getPostion_BottomLeft(), color);
            polygon.draw(plotter);
        }
    };
    
    static DrawRectangleAlgorithm drawFilled = new DrawRectangleAlgorithm() {
        @Override
        public void draw(Rectangle rectangle, Color color, PointPlotter plotter) {
            drawOutline.draw(rectangle, color, plotter);
            int x1 = rectangle.getPostion_TopLeft().getCol();
            int y1 = rectangle.getPostion_TopLeft().getRow();
            int x2 = rectangle.getPostion_BottomRight().getCol();
            int y2 = rectangle.getPostion_BottomRight().getRow();
            // set color of plotter
            Color lastColor = null;
            if (color != null) {
                lastColor = plotter.setColor(color);
            }
            // now fill
            for (int x = x1+1; x<x2; x++) {
                for (int y = y1+1; y<y2; y++) {
                    plotter.plot(x, y);
                }
            }
            if (color != null) {
                plotter.setColor(lastColor);
            }

            
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
    public void draw(PointPlotter pointPlotter) {
        this.algorithm.draw(rectangle, color, pointPlotter);
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
