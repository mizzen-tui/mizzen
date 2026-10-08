package dtb.terminalIO.components.plot.shape.catalog;

import dtb.terminalIO.components.plot.shape.CompositeShape;
import dtb.terminalIO.components.plot.shape.Line;
import dtb.terminalIO.components.plot.shape.Polygon;
import dtb.terminalIO.components.plot.shape.Shape;
import dtb.terminalIO.components.plot.shape.Triangle;
import dtb.terminalIO.geom.position.Vec2;

/**
 * A house: walls, a filled roof, a chimney, a filled door and a window with bars - one shape, it
 * all moves together. new House(30, 32, 60, 56) is 60 across the eaves and 56 from the roof's top
 * to the ground.
 */
public class House extends BuiltShape<House> {

    private float width;
    private float height;

    public House(float cx, float cy, float width, float height) {
        super(cx, cy);
        this.width = width;
        this.height = height;
        setName("House");
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public House setSize(float width, float height) {
        this.width = width;
        this.height = height;
        return rebuild();
    }

    /** a point of the drawing below, laid out 60 across (x 0 to 60) and 56 down (y 4 to 60) */
    private Vec2<?> p(double x, double y) {
        return at((x - 30) * width / 60, (y - 32) * height / 56);
    }

    private Polygon box(double x1, double y1, double x2, double y2) {
        return new Polygon().addPoint(p(x1, y1)).addPoint(p(x2, y1)).addPoint(p(x2, y2)).addPoint(p(x1, y2));
    }

    private Triangle roof() {
        Vec2<?> left = p(0, 30), top = p(30, 4), right = p(60, 30);
        return new Triangle(left.getX(), left.getY(), top.getX(), top.getY(), right.getX(), right.getY());
    }

    @Override
    protected Shape<?> build() {
        return new CompositeShape()
                .addShape(box(5, 30, 55, 60))                                       // walls
                .addShape(roof().setFilled(true))
                .addShape(box(42, 8, 48, 20))                                       // chimney
                .addShape(box(22, 42, 33, 60).setFilled(true))                      // door
                .addShape(box(9, 36, 19, 45))                                       // window
                .addShape(new Line(p(14, 36), p(14, 44)))                           // window bars
                .addShape(new Line(p(9, 40), p(18, 40)));
    }
}
