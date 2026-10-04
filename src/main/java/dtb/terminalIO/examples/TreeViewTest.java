package dtb.terminalIO.examples;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.components.input.TextInput;
import dtb.terminalIO.components.input.tree.DefaultTreeNode;
import dtb.terminalIO.components.input.tree.TreeView;
import dtb.terminalIO.components.plot.braille.BrailleGrid;
import dtb.terminalIO.components.plot.shape.Circle;
import dtb.terminalIO.components.plot.shape.DotMatrixText;
import dtb.terminalIO.components.plot.shape.Ellipse;
import dtb.terminalIO.components.plot.shape.Polygon;
import dtb.terminalIO.components.plot.shape.RectangleShape;
import dtb.terminalIO.components.plot.shape.Shape;
import dtb.terminalIO.components.plot.shape.Triangle;
import dtb.terminalIO.components.scroll.ScrollableComponent;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.TimedRequestedController;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.PositionTransform;

/**
 * TreeView: a tree of shapes on the left; the right panel draws whatever is selected (a group: the
 * shapes inside it; the root: the groups' names). The status line shows the tree's three events as they happen: selection
 * (onChange), expansion (onExpansion) and activation (onActivate: ENTER or a click on the selected
 * leaf).
 * <p/>
 * Drawing is debounced with a TimedRequestedController: hold DOWN to cruise through the tree - the
 * status line follows every move, the drawing catches up when you pause.
 * <p/>
 * Keys: UP / DOWN select; RIGHT / LEFT expand / collapse (or go to child / parent); ENTER expands /
 * collapses or activates; HOME / END; TAB to the note field and back. Mouse: click a row; click
 * ▸ / ▾; click the selected leaf to activate it.
 */
public class TreeViewTest {

    public static void main(String[] args) throws IOException {

        new TUIApp(args)
           .setDefaultMainApplicationPanel()
           .setOnReadyStart((tApp) -> {

               DefaultTreeNode<String> shapes = DefaultTreeNode.of("Shapes",
                       DefaultTreeNode.of("Round", DefaultTreeNode.of("Circle"), DefaultTreeNode.of("Ellipse")),
                       DefaultTreeNode.of("Polygons", DefaultTreeNode.of("Square"), DefaultTreeNode.of("Triangle"), DefaultTreeNode.of("Hexagon")),
                       DefaultTreeNode.of("Text", DefaultTreeNode.of("Hello"), DefaultTreeNode.of("TUI")));

               TreeView<DefaultTreeNode<String>> tree = new TreeView<DefaultTreeNode<String>>(shapes.asModel());
               ScrollableComponent treePane = new ScrollableComponent(Colors.BIGre, new Dimension.Simple(16, 30), tree);
               treePane.setTitle(" Shapes ");

               BrailleGrid grid = new BrailleGrid(Colors.BGre, new Dimension.Simple(16, 60));

               TextComponent help = new TextComponent(Colors.ICya,
                       "UP/DOWN select  RIGHT/LEFT expand/collapse  ENTER toggle/activate  click row, ▸/▾  TAB  CTRL-C quits");
               TextComponent status = new TextComponent(Colors.IGre, "");
               TextInput note = new TextInput(Colors.IYel, 30);

               String[] lastExpansion = { "-" };
               String[] activated = { "-" };
               int[] activations = { 0 };
               Runnable showStatus = () -> status.setTextAndResize("selected: " + tree.getValue()
                       + "    last expanded/collapsed: " + lastExpansion[0]
                       + "    activated: " + activated[0] + " (x" + activations[0] + ")");

               // drawing can be slow: draw once the selection stops moving (at least every 750 ms while it moves)
               TimedRequestedController redraw = new TimedRequestedController((te) -> {
                   drawSelected(grid, tree.getValue());
                   te.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(grid));
               });

               tree.onChange((te) -> {
                   showStatus.run();
                   redraw.onRequest(te.getEventBus());
                   te.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(status));
               });
               tree.onExpansion((node, expanded, eventBus) -> {
                   lastExpansion[0] = (expanded ? "expanded " : "collapsed ") + node;
                   showStatus.run();
                   repaint(eventBus, status);
               });
               tree.onActivate((node, eventBus) -> {
                   activated[0] = node.toString();
                   activations[0]++;
                   showStatus.run();
                   repaint(eventBus, status);
               });

               HorizontalPanel panes = new HorizontalPanel().setHorizontalSpacer(2);
               panes.add(treePane);
               panes.add(grid);

               VerticalPanel page = new VerticalPanel();
               page.add(help);
               page.add(new TextComponent(""));
               page.add(panes);
               page.add(new TextComponent(""));
               page.add(status);
               page.add(new TextComponent(""));
               page.add(new TextComponent("Note: "));
               page.add(note);

               showStatus.run();
               drawSelected(grid, tree.getValue());
               tApp.getMainApplicationPanel().add(1, 2, page);

           }).start();
    }

    private static void repaint(EventBus eventBus, Object source) {
        eventBus.raiseEvent(new RequestUpdateCanvasEvent(source));
    }

    /**
     * Draw what node stands for: a leaf - its shape; a group of leaves - their shapes side by side;
     * a group of groups (the root) - the groups' names, one under the other.
     */
    private static void drawSelected(BrailleGrid grid, DefaultTreeNode<String> node) {
        grid.clearPlots();
        grid.setTitle(Colors.BPur, " " + node + " ");
        List<DefaultTreeNode<String>> children = node.getChildren();
        if (!children.isEmpty() && !children.get(0).getChildren().isEmpty()) {
            for (int i = 0; i < children.size(); i++) {
                DotMatrixText name = new DotMatrixText(children.get(i).getValue());
                name.moveTo(4 + i * 12, 4);                  // DotMatrixText is placed with moveTo (no transform)
                name.draw(grid);
            }
        } else {
            List<String> shapes = new ArrayList<String>();
            if (children.isEmpty()) {
                shapes.add(node.getValue());
            }
            children.forEach((child) -> shapes.add(child.getValue()));
            for (int i = 0; i < shapes.size(); i++) {
                Shape<?> shape = shapeFor(shapes.get(i));
                if (shape instanceof DotMatrixText) {
                    ((DotMatrixText) shape).moveTo(12, 4 + i * 40);
                } else {
                    shape.setPositionTransform(new PositionTransform.Translate(4, 4 + i * 40));
                }
                shape.draw(grid);
            }
        }
        grid.markAsDirty();
    }

    /** each leaf's shape, drawn in a box about 30 dots high and 36 wide */
    private static Shape<?> shapeFor(String name) {
        switch (name) {
            case "Circle":   return new Circle(15, 15, 14);
            case "Ellipse":  return new Ellipse(18, 14, 34, 22);
            case "Square":   return new RectangleShape(new Rectangle.Simple(0, 0, 28, 28));
            case "Triangle": return new Triangle(0, 28, 16, 0, 32, 28);
            case "Hexagon":  return new Polygon(new int[][] { { 8, 0 }, { 24, 0 }, { 32, 14 }, { 24, 28 }, { 8, 28 }, { 0, 14 }, { 8, 0 } });
            default:         return new DotMatrixText(name);
        }
    }
}
