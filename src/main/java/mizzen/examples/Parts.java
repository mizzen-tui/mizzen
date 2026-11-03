package mizzen.examples;

import java.util.function.Supplier;

import mizzen.components.input.tree.DefaultTreeNode;
import mizzen.components.plot.shape.Shape;
import mizzen.components.plot.shape.catalog.Gear;
import mizzen.components.plot.shape.catalog.House;
import mizzen.components.plot.shape.catalog.RegularPolygon;
import mizzen.components.plot.shape.catalog.Ring;
import mizzen.components.plot.shape.catalog.Spring;
import mizzen.components.plot.shape.catalog.Star;

/**
 * The parts the examples sell and draw - PartsShopDemo's sales, FormTry's catalogue: one list for both.
 */
public final class Parts {

    private Parts() {
    }

    // which of the theme's drawing colours (Theme.drawingColors) - the same kind of colour in every theme
    private static final int CYAN = 0, YELLOW = 1, PURPLE = 2, GREEN = 3, RED = 4, BLUE = 5, ORANGE = 6, GREY = 7;

    /** a part the shop sells, and how it looks (drawn round 0, 0, about 1 across, y up) */
    public static class Part {
        public final String name;
        public final float price;
        public final int drawingColor;                         // which of the theme's drawing colours: pen.drawingColor(part.drawingColor)
        public final Supplier<Shape<?>> drawing;

        Part(String name, float price, int drawingColor, Supplier<Shape<?>> drawing) {
            this.name = name;
            this.price = price;
            this.drawingColor = drawingColor;
            this.drawing = drawing;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static final Part GEAR_12 = new Part("Gear, 12 teeth", 12.50f, YELLOW, () -> new Gear(0, 0, 12, 1).setHoleRadius(0.35f).setYUp(true).setFilled(true));
    public static final Part GEAR_20 = new Part("Gear, 20 teeth", 18.00f, ORANGE, () -> new Gear(0, 0, 20, 1).setRootRadius(0.86f).setYUp(true).setFilled(true));
    public static final Part HEX_NUT = new Part("Hex nut M8", 0.30f, GREY, () -> new RegularPolygon(0, 0, 1, 6).setHoleRadius(0.5f).setYUp(true).setFilled(true));
    public static final Part WASHER = new Part("Washer M8", 0.10f, CYAN, () -> new Ring(0, 0, 1, 0.45f).setYUp(true).setFilled(true));
    public static final Part SPRING = new Part("Spring, 40 mm", 1.20f, GREEN, () -> new Spring(0, 0, 2, 0.7f, 5).setYUp(true));
    public static final Part KNOB = new Part("Star knob", 3.75f, PURPLE, () -> new Star(0, 0, 1, 0.45f).setYUp(true).setFilled(true));
    public static final Part BRACE = new Part("Corner brace", 2.40f, BLUE, () -> new RegularPolygon(0, 0, 1, 3).setHoleRadius(0.25f).setYUp(true).setFilled(true));
    public static final Part SHED = new Part("Garden shed kit", 899.00f, RED, () -> new House(0, 0, 2, 1.9f).setYUp(true));

    /** the parts by kind, as a tree: groups are names, leaves are parts */
    @SuppressWarnings("unchecked")
    public static final DefaultTreeNode<Object> CATALOGUE = DefaultTreeNode.<Object>of("Parts",
            DefaultTreeNode.<Object>of("Gears", leaf(GEAR_12), leaf(GEAR_20)),
            DefaultTreeNode.<Object>of("Fasteners", leaf(HEX_NUT), leaf(WASHER), leaf(KNOB)),
            DefaultTreeNode.<Object>of("Springs and braces", leaf(SPRING), leaf(BRACE)),
            DefaultTreeNode.<Object>of("Kits", leaf(SHED)));

    private static DefaultTreeNode<Object> leaf(Part part) {
        return new DefaultTreeNode<Object>(part);
    }
}
