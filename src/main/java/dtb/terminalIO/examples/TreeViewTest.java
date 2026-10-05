package dtb.terminalIO.examples;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.components.input.TextInput;
import dtb.terminalIO.components.input.tree.DefaultTreeNode;
import dtb.terminalIO.components.input.tree.TreeView;
import dtb.terminalIO.components.plot.braille.BrailleGrid;
import dtb.terminalIO.components.plot.image.BufferedImageProvider;
import dtb.terminalIO.components.plot.image.ImageViewerFactory;
import dtb.terminalIO.components.plot.image.ImageViewerFactory.ImageRenderRequest;
import dtb.terminalIO.components.plot.image.MovingPicturesViewer;
import dtb.terminalIO.components.plot.image.ResizableImageViewer;
import dtb.terminalIO.components.plot.image.animate.AnimatedGifImageIterator;
import dtb.terminalIO.components.plot.shape.Circle;
import dtb.terminalIO.components.plot.shape.DotMatrixText;
import dtb.terminalIO.components.plot.shape.Ellipse;
import dtb.terminalIO.components.plot.shape.Polygon;
import dtb.terminalIO.components.plot.shape.RectangleShape;
import dtb.terminalIO.components.plot.shape.Shape;
import dtb.terminalIO.components.plot.shape.Triangle;
import dtb.terminalIO.components.scroll.ScrollableComponent;
import dtb.terminalIO.components.scroll.SearchFunctionForScrollableComponent;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.TimedRequestedController;
import dtb.terminalIO.examples.plot.image.FractalAnimation;
import dtb.terminalIO.examples.sampledata.ReadSampleFiles;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.PositionTransform;
import dtb.terminalIO.util.StringUtils;

import javax.imageio.ImageIO;

/**
 * TreeView: a tree on the left - shapes and images, in groups; the right panel draws whatever is
 * selected, a different view for each kind of node - like a tab panel's tabs: a shape or group is
 * drawn in Braille (a group of leaves: the leaves side by side; a group of groups: the groups'
 * names); an image gets its own panel. Each node's value is a DemoNode saying what it stands for;
 * viewFor picks the NodeView. The status line shows the tree's three
 * events as they happen: selection (onChange), expansion (onExpansion) and activation (onActivate:
 * ENTER or a click on the selected leaf).
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

               DefaultTreeNode<DemoNode> sampleTree = node(DemoNode.group("Sample Tree"),
                       node(DemoNode.group("Shapes"),
                               node(DemoNode.group("Round"), node(DemoNode.shape("Circle")), node(DemoNode.shape("Ellipse"))),
                               node(DemoNode.group("Polygons"), node(DemoNode.shape("Square")), node(DemoNode.shape("Triangle")),
                                                                node(DemoNode.shape("Hexagon"))),
                               node(DemoNode.group("Text"), node(DemoNode.shape("Hello")), node(DemoNode.shape("TUI")))),
                       node(DemoNode.group("Images"),
                               node(DemoNode.image("Rocket launch", "rocket-launch.gif")),
                               node(DemoNode.image("Sailboat", "ketch.gif")),
                               node(DemoNode.image("Solar system", "solar-system.gif")),
                               node(DemoNode.image("Spinning shapes", "shapes.gif")),
                               node(DemoNode.image("Earth", "earth-messenger.jpg"))),
                       node(DemoNode.fractal("Fractal")),
                       node(DemoNode.text("Lorem ipsum", "LoremIpsumLarge.txt")));

               TreeView<DefaultTreeNode<DemoNode>> tree = new TreeView<DefaultTreeNode<DemoNode>>(sampleTree.asModel());
               Dimension viewSize = viewSize(tApp.getCanvas());
               ScrollableComponent treePane = new ScrollableComponent(Colors.BIGre, new Dimension.Simple(viewSize.getHeight(), TREE_WIDTH), tree);
               treePane.setTitle(" Sample Tree ");

               // the right-hand side: shows the selected node's view - like a tab panel's content
               Composite detail = new Composite(viewSize);
               NodeView[] shown = { null };

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

               // building a view can be slow: switch once the selection stops moving (at least every 750 ms while it moves)
               TimedRequestedController redraw = new TimedRequestedController((te) -> {
                   show(detail, shown, viewFor(tree.getValue(), detail), te.getEventBus());
                   te.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(detail));
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
                   show(detail, shown, viewFor(node, detail), eventBus);      // a fresh view: e.g. an animation from its start
                   repaint(eventBus, detail);
                   repaint(eventBus, status);
               });

               HorizontalPanel panes = new HorizontalPanel().setHorizontalSpacer(2);
               panes.add(treePane);
               panes.add(detail);

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
               show(detail, shown, viewFor(tree.getValue(), detail), tApp.getEventBus());
               tApp.getMainApplicationPanel().add(1, 2, page);

               // the panes follow the terminal's size; the view is rebuilt to fit
               tApp.appendOnResizeAction((app, size) -> {
                   Dimension newSize = viewSize(size);
                   treePane.resize(new Dimension.Simple(newSize.getHeight(), TREE_WIDTH));
                   detail.resize(newSize);
                   show(detail, shown, viewFor(tree.getValue(), newSize), app.getEventBus());
                   panes.fullrecalcDim();
                   page.fullrecalcDim();
               });

           }).start();
    }

    private static void repaint(EventBus eventBus, Object source) {
        eventBus.raiseEvent(new RequestUpdateCanvasEvent(source));
    }

    /** what a tree node stands for: a group of nodes, a shape to draw, or an image (a bundled one, by name) */
    private static class DemoNode {

        enum Kind { GROUP, SHAPE, IMAGE, FRACTAL, TEXT }

        final Kind kind;
        final String name;
        final String file;      // IMAGE: the bundled image, e.g. "ketch.gif" (a .gif animates); TEXT: the sample text file; otherwise null

        private DemoNode(Kind kind, String name, String file) {
            this.kind = kind;
            this.name = name;
            this.file = file;
        }

        static DemoNode group(String name) {
            return new DemoNode(Kind.GROUP, name, null);
        }

        static DemoNode shape(String name) {
            return new DemoNode(Kind.SHAPE, name, null);
        }

        static DemoNode image(String name, String image) {
            return new DemoNode(Kind.IMAGE, name, image);
        }

        static DemoNode fractal(String name) {
            return new DemoNode(Kind.FRACTAL, name, null);
        }

        static DemoNode text(String name, String file) {
            return new DemoNode(Kind.TEXT, name, file);
        }

        /** what the tree shows on the node's row */
        @Override
        public String toString() {
            return name;
        }
    }

    @SafeVarargs
    private static DefaultTreeNode<DemoNode> node(DemoNode value, DefaultTreeNode<DemoNode>... children) {
        return DefaultTreeNode.of(value, children);
    }

    static final int TREE_WIDTH = 30;

    /** the view's size in a terminal (or canvas) of this size: what the tree, the help and status lines leave */
    static Dimension viewSize(Dimension terminal) {
        return new Dimension.Simple(Math.max(8, terminal.getHeight() - 10), Math.max(20, terminal.getWidth() - TREE_WIDTH - 6));
    }

    /**
     * What the right-hand side shows for a node - built when the node is selected, like a tab's
     * content (see TabAction). shown / hidden: it came on screen / went off it - e.g. to start and
     * stop an animation's timer.
     */
    interface NodeView {
        Component<?> component();

        default void shown(EventBus eventBus) {
        }

        default void hidden(EventBus eventBus) {
        }
    }

    /** the view for a node, by what it stands for, of the given size */
    private static NodeView viewFor(DefaultTreeNode<DemoNode> node, Dimension size) {
        DemoNode value = node.getValue();
        switch (value.kind) {
            case IMAGE:   return value.file.endsWith(".gif") ? animatedView(value, size) : stillView(value, size);
            case FRACTAL: return fractalView(value, size);
            case TEXT:    return textView(value, size);
            default:      return drawingView(node, size);          // groups and shapes: drawn in Braille
        }
    }

    /** replace the view on the right with view */
    private static void show(Composite detail, NodeView[] shown, NodeView view, EventBus eventBus) {
        if (shown[0] != null) {
            shown[0].hidden(eventBus);
            detail.remove(shown[0].component());
        }
        shown[0] = view;
        detail.add(view.component());
        view.shown(eventBus);
    }

    /**
     * Draw what node stands for: a leaf - its shape; a group of leaves - their shapes side by side;
     * a group of groups (the root) - the groups' names, one under the other.
     */
    private static NodeView drawingView(DefaultTreeNode<DemoNode> node, Dimension size) {
        BrailleGrid grid = new BrailleGrid(Colors.BGre, inside(size));      // the size given is inside its border
        grid.setTitle(Colors.BPur, " " + node + " ");
        List<DefaultTreeNode<DemoNode>> children = node.getChildren();
        if (!children.isEmpty() && !children.get(0).getChildren().isEmpty()) {
            for (int i = 0; i < children.size(); i++) {
                DotMatrixText name = new DotMatrixText(children.get(i).getValue().name);
                name.moveTo(4 + i * 12, 4);                  // DotMatrixText is placed with moveTo (no transform)
                name.draw(grid);
            }
        } else {
            List<DemoNode> leaves = new ArrayList<DemoNode>();
            if (children.isEmpty()) {
                leaves.add(node.getValue());
            }
            children.forEach((child) -> leaves.add(child.getValue()));
            for (int i = 0; i < leaves.size(); i++) {
                Shape<?> shape = drawingFor(leaves.get(i));
                if (shape instanceof DotMatrixText) {
                    ((DotMatrixText) shape).moveTo(12, 4 + i * 40);
                } else {
                    shape.setPositionTransform(new PositionTransform.Translate(4, 4 + i * 40));
                }
                shape.draw(grid);
            }
        }
        return () -> grid;
    }

    /** an animated GIF, playing while it is shown; its Options button changes how it is drawn */
    private static NodeView animatedView(DemoNode image, Dimension size) {
        File gif = ReadSampleFiles.resourceAsFile(image.file);
        try {
            BufferedImage firstFrame = ImageIO.read(gif);
            ImageRenderRequest request = colourBlocks(firstFrame, fit(firstFrame, inside(size)));
            MovingPicturesViewer viewer = new MovingPicturesViewer(Colors.DEFAULT_COLOR, new AnimatedGifImageIterator(gif.getPath()), request);
            viewer.setBorder(true);
            viewer.setTitle(Colors.BPur, " " + image.name + " ");
            return new NodeView() {
                @Override
                public Component<?> component() {
                    return viewer;
                }

                @Override
                public void shown(EventBus eventBus) {
                    viewer.start(eventBus, 60);
                }

                @Override
                public void hidden(EventBus eventBus) {
                    viewer.close();             // stops its timer
                }
            };
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** a still image (a bundled one) */
    private static NodeView stillView(DemoNode image, Dimension size) {
        try {
            BufferedImage picture = ImageIO.read(ReadSampleFiles.resourceAsFile(image.file));
            return pictureView(" " + image.name + " ", picture, size);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** fractals, a new one every second, with their render style and ASCII gradient controls (FractalAnimation) */
    private static NodeView fractalView(DemoNode fractal, Dimension size) {
        Composite frame = new Composite(new Dimension.Simple(size));
        frame.setBorder(true);
        frame.setTitle(Colors.BPur, " " + fractal.name + " ");
        FractalAnimation fractals = new FractalAnimation(inside(size), 1000);
        frame.add(fractals.component());
        return new NodeView() {
            @Override
            public Component<?> component() {
                return frame;
            }

            @Override
            public void shown(EventBus eventBus) {
                fractals.start(eventBus);
            }

            @Override
            public void hidden(EventBus eventBus) {
                fractals.stop();
            }
        };
    }

    /** a long text: scrolls, and CTRL-F finds words in it (Prev / Next: CTRL-P / CTRL-N) */
    private static NodeView textView(DemoNode text, Dimension size) {
        try {
            String content = ReadSampleFiles.readSampleFile(text.file);
            content = StringUtils.addLineNumbers(StringUtils.wrap(content, size.getWidth() - 9));     // room for the border, line numbers, scroll bar
            ScrollableComponent pane = new ScrollableComponent(Colors.Gre, new Dimension.Simple(size), new TextComponent(Colors.Gre, content));
            pane.setTitle(" " + text.name + " ");
            SearchFunctionForScrollableComponent.addFinderToScrollable(pane.asSearchableView());
            return () -> pane;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** a picture in full colour blocks, in a titled frame of the view's size */
    private static NodeView pictureView(String title, BufferedImage picture, Dimension size) {
        Dimension imageSize = fit(picture, inside(size));
        Composite frame = new Composite(new Dimension.Simple(size));
        frame.setBorder(true);
        frame.setTitle(Colors.BPur, title);
        ResizableImageViewer viewer = new ResizableImageViewer(Colors.DEFAULT_COLOR, imageSize, new BufferedImageProvider.Simple(picture));
        viewer.setImageRenderType(ImageViewerFactory.IMAGE_RENDER_TYPE.BLOCK);
        viewer.setColorMapping(ImageViewerFactory.rgbColorMapDIRECT);
        viewer.setPlotPredicate(ImageViewerFactory.PLOT_ALWAYS);
        frame.add(viewer);
        return new NodeView() {
            @Override
            public Component<?> component() {
                return frame;
            }

            @Override
            public void shown(EventBus eventBus) {
                viewer.resizeImageAsynch(eventBus, imageSize);      // draws it
            }
        };
    }

    private static ImageRenderRequest colourBlocks(BufferedImage image, Dimension sizeInCharacters) {
        ImageRenderRequest request = new ImageRenderRequest();
        request.setDesiredImgSizeInCharacters(sizeInCharacters);
        request.setColorMap(ImageViewerFactory.rgbColorMapDIRECT);
        request.setImageRenderType(ImageViewerFactory.IMAGE_RENDER_TYPE.BLOCK);
        request.setToPlotOrNot(ImageViewerFactory.PLOT_ALWAYS);
        request.setImageToView(image);
        return request;
    }

    /** the area inside a view's border */
    private static Dimension inside(Dimension size) {
        return new Dimension.Simple(size.getHeight() - 2, size.getWidth() - 2);
    }

    /**
     * The largest size in characters that shows image undistorted within box: in colour blocks a
     * character holds 1 x 2 pixels (two half blocks), so a row of characters covers two pixel rows.
     */
    private static Dimension fit(BufferedImage image, Dimension box) {
        double charsPerRow = 2.0 * image.getWidth() / image.getHeight();     // columns per row of characters
        int rows = box.getHeight();
        int cols = (int) (rows * charsPerRow);
        if (cols > box.getWidth()) {
            cols = box.getWidth();
            rows = (int) (cols / charsPerRow);
        }
        return new Dimension.Simple(Math.max(1, rows), Math.max(1, cols));
    }

    /** what to draw for a leaf in a group: its shape; an image or a group - its name */
    private static Shape<?> drawingFor(DemoNode leaf) {
        return leaf.kind == DemoNode.Kind.SHAPE ? shapeFor(leaf.name) : new DotMatrixText(leaf.name);
    }

    /** each shape, drawn in a box about 30 dots high and 36 wide; a name with no shape: the name as text */
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
