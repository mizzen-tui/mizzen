package dtb.terminalIO.components.plot;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import dtb.terminalIO.components.AbstractComponentWrappper;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.HasTitleAndBorder;
import dtb.terminalIO.components.input.LeafFocusBehaviour;
import dtb.terminalIO.components.plot.blocks.BlocksGrid;
import dtb.terminalIO.components.plot.braille.BrailleGrid;
import dtb.terminalIO.components.plot.grid.GridContext;
import dtb.terminalIO.components.plot.grid.GridContext.GRID_TYPE;
import dtb.terminalIO.components.plot.shape.Shape;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.focus.ToFocusable;
import dtb.terminalIO.events.keyboard.AsKeyboardHandler;
import dtb.terminalIO.events.keyboard.KeyboardHandler;
import dtb.terminalIO.events.mouse.ApplicationMouseEvent;
import dtb.terminalIO.events.mouse.Clickable;
import dtb.terminalIO.events.mouse.Draggable;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.geom.position.PositionTransform;
import dtb.terminalIO.geom.position.Vec2;

/**
 * A plot in your own numbers: you say which part of your data to show (showing), and draw in it
 * with x to the right and y UP - the plot area works out where that falls on its grid of dots.
 * <pre>
 *     PlotArea plot = new PlotArea(color, new Dimension.Simple(20, 60))
 *             .showing(0, 2 * Math.PI, -1.5, 1.5)          // x from..to, y from..to
 *             .axes(true)
 *             .onDraw((pen) -> {                            // run again on every redraw
 *                 pen.color(Colors.BCya).function(Math::sin);
 *                 pen.draw(new Circle(3, 0, 0.3f));           // any library shape, in your numbers
 *                 pen.text(Math.PI, 1.2, "peak");
 *             });
 * </pre>
 * Three spaces, each with its own owner (so the same call never means two things):
 * <pre>
 *   cells   terminal characters       components, layout, the mouse
 *   dots    grid dots, y down         grids (BrailleGrid, BlocksGrid, Plotter), shapes drawn on them
 *   data    your numbers, y up        a PlotArea (and its Pen); toGrid / toData convert
 * </pre>
 * Scaling: STRETCH fills the area (a circle may come out oval), EQUAL keeps proportions (centred),
 * NONE draws one data unit per dot from the window's top-left (the rest is cut off).
 * <p/>
 * The mouse: onClick tells you which shape you clicked (the last drawn one under the mouse - tested
 * against the dots it drew, so what you see is what you click) and where, in your numbers;
 * draggable(shape) lets the user drag a shape - it moves in your numbers, so it stays put when the
 * window changes; panByDrag(true) makes a drag on empty space move the view. A press on a
 * draggable shape is also a click.
 * <p/>
 * After changing what onDraw draws (new data), call redraw() and request a canvas update.
 */
public class PlotArea extends AbstractComponentWrappper<Component<?>> implements ToFocusable, AsKeyboardHandler, Clickable, Draggable {

    /** how the data window is fitted to the area */
    public enum Scaling {
        /** fill the area: x and y scaled separately */
        STRETCH,
        /** keep proportions: the same scale for x and y, centred */
        EQUAL,
        /** one data unit per dot, from the window's top-left; what doesn't fit is cut off */
        NONE
    }

    private static final int GRAB_MARGIN = 1;                  // dots around the clicked cell that still count as on a shape

    private final GridContext grid;
    private final Plotter characters;                         // the grid's characters (for text)
    private final int dotsPerCellRow;
    private final int dotsPerCellCol;

    private double xMin = -1, xMax = 1, yMin = -1, yMax = 1;
    private Scaling scaling = Scaling.STRETCH;
    private boolean axes = false;
    private Color axesColor;
    private Consumer<Pen> onDraw = (pen) -> { };
    private Consumer<Click> onClick = null;
    private boolean xTickLabels = true;
    private double yAxisX = Double.NaN;                        // NaN: automatic (x 0, else the left edge)
    private double xAxisY = Double.NaN;                        // NaN: automatic (y 0, else the bottom)

    // the mapping, worked out on each redraw: dot = origin + (data - window corner) * scale
    private double scaleX = 1, scaleY = 1, left = 0, top = 0;
    private Rectangle dots;                                    // the grid's dots

    private final List<Drawn> drawn = new ArrayList<Drawn>();  // this frame's shapes, in drawing order, with their dots
    private final Map<Shape<?>, PositionTransform.Translate> draggable = new IdentityHashMap<Shape<?>, PositionTransform.Translate>();
    private DragMove dragging;                                 // null: no shape being dragged
    private boolean panByDrag = false;
    private boolean panning;
    private Vec2<?> panFrom;                                   // data: the point grabbed to pan

    private final LeafFocusBehaviour focus = LeafFocusBehaviour.of(this);

    /** a click on the plot: the shape clicked (null: none), where (in your numbers), and the event bus (to repaint, raise events) */
    public static class Click {
        private final Shape<?> shape;
        private final Vec2<?> at;
        private final EventBus eventBus;

        Click(Shape<?> shape, Vec2<?> at, EventBus eventBus) {
            this.shape = shape;
            this.at = at;
            this.eventBus = eventBus;
        }

        public Shape<?> shape() {
            return shape;
        }

        public Vec2<?> at() {
            return at;
        }

        public EventBus eventBus() {
            return eventBus;
        }
    }

    /** a shape drawn this frame, and the dots it set (for clicks) */
    private static class Drawn {
        final Shape<?> shape;
        final BitSet dotsSet;

        Drawn(Shape<?> shape, BitSet dotsSet) {
            this.shape = shape;
            this.dotsSet = dotsSet;
        }
    }

    /** a Braille grid (2 x 4 dots per character) */
    public PlotArea(Color color, Dimension size) {
        this(color, size, GRID_TYPE.BRAILLE_GRID_CTX);
    }

    /** gridType: BRAILLE_GRID_CTX (2 x 4 dots a character), BLOCK_GRID_CTX (1 x 2), SIMPLE_GRID_CTX (one '*' a character) */
    public PlotArea(Color color, Dimension size, GRID_TYPE gridType) {
        this(color, size, GridContext.getGridContextFromType(gridType, color));
    }

    private PlotArea(Color color, Dimension size, GridContext grid) {
        super(color, grid.asComponent());
        this.grid = grid;
        this.characters = charactersOf(grid.asComponent());
        Position oneCell = grid.mapCharPositionToGridPosition(new Position.Simple(1, 1));
        Position origin = grid.mapCharPositionToGridPosition(Position.ZERO);
        this.dotsPerCellRow = oneCell.getRow() - origin.getRow();
        this.dotsPerCellCol = oneCell.getCol() - origin.getCol();
        this.axesColor = color;
        resize(size);
        redraw();
    }

    private static Plotter charactersOf(Component<?> gridComponent) {
        if (gridComponent instanceof BrailleGrid) {
            return ((BrailleGrid) gridComponent).getPlotter();
        }
        if (gridComponent instanceof BlocksGrid) {
            return ((BlocksGrid) gridComponent).getPlotter();
        }
        if (gridComponent instanceof Plotter) {
            return (Plotter) gridComponent;
        }
        throw new IllegalArgumentException("PlotArea: no characters plotter in " + gridComponent.getClass().getSimpleName());
    }

    // ---- what to show ---------------------------------------------------------------------------

    /** the window on your data: x from xMin to xMax, y from yMin (bottom) to yMax (top) */
    public PlotArea showing(double xMin, double xMax, double yMin, double yMax) {
        if (!(xMax > xMin) || !(yMax > yMin)) {
            throw new IllegalArgumentException("PlotArea.showing: needs xMin < xMax and yMin < yMax, got x " + xMin + ".." + xMax + ", y " + yMin + ".." + yMax);
        }
        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;
        redraw();
        return this;
    }

    /** the window around these points, with a margin of a twentieth of each side */
    public PlotArea fitTo(Iterable<? extends Vec2<?>> points) {
        double x0 = Double.MAX_VALUE, x1 = -Double.MAX_VALUE, y0 = Double.MAX_VALUE, y1 = -Double.MAX_VALUE;
        for (Vec2<?> p : points) {
            x0 = Math.min(x0, p.getX());
            x1 = Math.max(x1, p.getX());
            y0 = Math.min(y0, p.getY());
            y1 = Math.max(y1, p.getY());
        }
        if (x0 > x1) {
            return this;                                       // no points
        }
        double mx = Math.max((x1 - x0) / 20, 1e-9);
        double my = Math.max((y1 - y0) / 20, 1e-9);
        return showing(x0 - mx, x1 + mx, y0 - my, y1 + my);
    }

    public PlotArea scaling(Scaling scaling) {
        this.scaling = scaling;
        redraw();
        return this;
    }

    /** x and y axes (at 0, or along the edge when 0 is out of view), with labelled ticks */
    public PlotArea axes(boolean axes) {
        this.axes = axes;
        redraw();
        return this;
    }

    /** where the y axis runs (an x in your numbers, kept within the window); NaN: automatic - at x 0, else along the left */
    public PlotArea yAxisAt(double x) {
        this.yAxisX = x;
        redraw();
        return this;
    }

    /** where the x axis runs (a y in your numbers, kept within the window); NaN: automatic - at y 0, else along the bottom */
    public PlotArea xAxisAt(double y) {
        this.xAxisY = y;
        redraw();
        return this;
    }

    /** the numbers under the x axis's ticks (default on; a bar chart writes its own names there) */
    public PlotArea xTickLabels(boolean xTickLabels) {
        this.xTickLabels = xTickLabels;
        redraw();
        return this;
    }

    public PlotArea axesColor(Color axesColor) {
        this.axesColor = axesColor;
        redraw();
        return this;
    }

    /** what to draw, in your numbers - run again on every redraw (resize, showing, scaling, redraw()) */
    public PlotArea onDraw(Consumer<Pen> onDraw) {
        this.onDraw = onDraw == null ? (pen) -> { } : onDraw;
        redraw();
        return this;
    }

    /** a click: which shape (if any), where in your numbers, and the event bus */
    public PlotArea onClick(Consumer<Click> onClick) {
        this.onClick = onClick;
        return this;
    }

    /** the user can drag this shape (drawn with pen.draw); it moves in your numbers */
    public PlotArea draggable(Shape<?> shape) {
        draggable.putIfAbsent(shape, new PositionTransform.Translate(0f, 0f));
        return this;
    }

    /** a drag that starts on empty space (not on a draggable shape) moves the view - the point grabbed stays under the mouse */
    public PlotArea panByDrag(boolean panByDrag) {
        this.panByDrag = panByDrag;
        return this;
    }

    public PlotArea zoom(double factor) {
        double cx = (xMin + xMax) / 2, cy = (yMin + yMax) / 2;
        double hx = (xMax - xMin) / 2 / factor, hy = (yMax - yMin) / 2 / factor;
        return showing(cx - hx, cx + hx, cy - hy, cy + hy);
    }

    public PlotArea pan(double dx, double dy) {
        return showing(xMin + dx, xMax + dx, yMin + dy, yMax + dy);
    }

    /** a title (and so a border: the grid's inside shrinks by a character each side - drawn again to fit) */
    public PlotArea setTitle(String title) {
        if (grid.asComponent() instanceof HasTitleAndBorder) {
            ((HasTitleAndBorder<?>) grid.asComponent()).setTitle(title);
            redraw();
        }
        return this;
    }

    /** one character's width, in your x units (e.g. to space out text) */
    public double characterWidth() {
        return dotsPerCellCol / scaleX;
    }

    /** one character's height, in your y units */
    public double characterHeight() {
        return dotsPerCellRow / scaleY;
    }

    public double getXMin() { return xMin; }
    public double getXMax() { return xMax; }
    public double getYMin() { return yMin; }
    public double getYMax() { return yMax; }

    // ---- the mapping ----------------------------------------------------------------------------

    /** the grid dot a data point falls on (row down) - rounded, as drawing rounds */
    public Position toGrid(double x, double y) {
        return new Position.Simple((int) Math.round(dotRow(y)), (int) Math.round(dotCol(x)));
    }

    /** the data point at a grid dot */
    public Vec2<?> toData(Position dot) {
        return new Vec2.BaseVec2((float) dataX(dot.getCol()), (float) dataY(dot.getRow()));
    }

    double dotCol(double x) {
        return dots.getCol() + left + (x - xMin) * scaleX;
    }

    double dotRow(double y) {
        return dots.getRow() + top + (yMax - y) * scaleY;
    }

    double dataX(double col) {
        return xMin + (col - dots.getCol() - left) / scaleX;
    }

    double dataY(double row) {
        return yMax - (row - dots.getRow() - top) / scaleY;
    }

    /** data -> dots, for shapes: applied to their corners */
    final PositionTransform view = (p) -> new Position.Simple((float) dotRow(p.getY()), (float) dotCol(p.getX()));

    private void workOutMapping() {
        dots = grid.getGridRectangle();
        double spanCols = Math.max(1, dots.getWidth() - 1);
        double spanRows = Math.max(1, dots.getHeight() - 1);
        double dx = xMax - xMin, dy = yMax - yMin;
        // a dot's height / width on screen (a character is about twice as tall as wide)
        double dotAspect = 2.0 * dotsPerCellCol / dotsPerCellRow;
        switch (scaling) {
            case EQUAL:
                double s = Math.min(spanCols / dx, spanRows / dy * dotAspect);
                scaleX = s;
                scaleY = s / dotAspect;
                left = (spanCols - dx * scaleX) / 2;
                top = (spanRows - dy * scaleY) / 2;
                break;
            case NONE:
                scaleX = 1;
                scaleY = 1;
                left = 0;
                top = 0;
                break;
            default:
                scaleX = spanCols / dx;
                scaleY = spanRows / dy;
                left = 0;
                top = 0;
        }
    }

    // ---- drawing --------------------------------------------------------------------------------

    /** draw again: axes, then onDraw */
    public PlotArea redraw() {
        if (grid == null || drawn == null) {
            return this;                                           // still being built (a resize from the base class)
        }
        if (grid.getGridRectangle() == null || grid.getGridRectangle().getWidth() < 1) {
            return this;
        }
        workOutMapping();
        grid.asPointPlotter().clearPlots();
        drawn.clear();
        Pen pen = new Pen(this);
        if (axes) {
            drawAxes(pen);                                      // lines first: your drawing goes over them
        }
        pen.color(getColor());
        onDraw.accept(pen);
        if (axes) {
            labelAxes();                                        // labels last: nothing covers them
        }
        markAsDirty();
        return this;
    }

    @Override
    public void resize(Dimension d) {
        super.resize(d);
        redraw();
    }

    /** where the points go: the grid, cut at its edges, also noting the dots of the shape being drawn */
    PointPlotter dotsPlotter(BitSet recording) {
        PointPlotter target = grid.asPointPlotter();
        Rectangle r = dots;
        return new PointPlotter.Delegate(target) {
            @Override
            public void plot(float x, float y) {
                int col = Math.round(x);
                int row = Math.round(y);
                if (col < r.getCol() || row < r.getRow() || col >= r.getCol() + r.getWidth() || row >= r.getRow() + r.getHeight()) {
                    return;
                }
                if (recording != null) {
                    recording.set((row - r.getRow()) * r.getWidth() + (col - r.getCol()));
                }
                super.plot(col, row);
            }
        };
    }

    /** a shape, in data units (with its drag offset, if draggable), recorded for clicks */
    void drawShape(Shape<?> shape, Color color) {
        PositionTransform own = shape.getPositionTransform();
        PositionTransform.Translate moved = draggable.get(shape);
        PositionTransform inData = moved == null ? own : new PositionTransform.FofG(moved, own);
        BitSet recording = new BitSet();
        PointPlotter plotter = dotsPlotter(recording);
        plotter.setNextPlotColor(color);
        shape.setPositionTransform(new PositionTransform.FofG(view, inData));
        try {
            shape.draw(plotter);
        } finally {
            shape.setPositionTransform(own);
        }
        drawn.add(new Drawn(shape, recording));
    }

    /** text in the character cell at a grid dot (it replaces what that cell showed) */
    void text(Position dot, String text, Color color) {
        textInCell(cellRow(dot.getRow()), cellCol(dot.getCol()), text, color);
    }

    /** text that clicks as a shape: its characters count as part of shape (a legend line, a bar's name) */
    void text(Position dot, String text, Color color, Shape<?> clicksAs) {
        text(dot, text, color);
        int row = cellRow(dot.getRow());
        int col = cellCol(dot.getCol());
        BitSet cells = new BitSet();
        for (int c = Math.max(0, col); c < col + text.length(); c++) {
            for (int dr = 0; dr < dotsPerCellRow; dr++) {
                for (int dc = 0; dc < dotsPerCellCol; dc++) {
                    int r = row * dotsPerCellRow + dr;
                    int k = c * dotsPerCellCol + dc;
                    if (r >= 0 && r < dots.getHeight() && k < dots.getWidth()) {
                        cells.set(r * dots.getWidth() + k);
                    }
                }
            }
        }
        drawn.add(new Drawn(clicksAs, cells));
    }

    /** text from a character cell on (it replaces what those cells showed) */
    private void textInCell(int row, int col, String text, Color color) {
        for (int i = 0; i < text.length(); i++) {
            int c = col + i;
            if (row < 0 || c < 0 || row >= characters.getHeight() || c >= characters.getWidth()) {
                continue;
            }
            Plotter.Plot at = characters.getPlotAt(row, c);
            if (at != null) {
                at.setPlotChar(text.charAt(i));
                at.setPlotColor(color);
            } else {
                characters.addPlot(row, c, text.charAt(i), color);
            }
        }
    }

    Rectangle dots() {
        return dots;
    }

    // ---- axes -----------------------------------------------------------------------------------

    /** the x axis: at y 0, else along the bottom; the y axis: at x 0, else along the left */
    private double axisY() {
        if (!Double.isNaN(xAxisY)) {
            return Math.max(yMin, Math.min(yMax, xAxisY));
        }
        return (yMin <= 0 && 0 <= yMax) ? 0 : yMin;
    }

    private double axisX() {
        if (!Double.isNaN(yAxisX)) {
            return Math.max(xMin, Math.min(xMax, yAxisX));
        }
        return (xMin <= 0 && 0 <= xMax) ? 0 : xMin;
    }

    private double xStep() {
        return niceStep(xMax - xMin, Math.max(2, dots.getWidth() / dotsPerCellCol / 10));      // a label about every 10 characters
    }

    private double yStep() {
        return niceStep(yMax - yMin, Math.max(2, dots.getHeight() / dotsPerCellRow / 4));      // ... about every 4 rows
    }

    /** the axis lines and their ticks */
    private void drawAxes(Pen pen) {
        double axisY = axisY(), axisX = axisX();
        pen.color(axesColor);
        pen.line(xMin, axisY, xMax, axisY);
        pen.line(axisX, yMin, axisX, yMax);
        double xStep = xStep(), yStep = yStep();
        for (double x = Math.ceil(xMin / xStep) * xStep; x <= xMax + xStep / 1e6; x += xStep) {
            Position at = toGrid(x, axisY);
            pen.dot(at.getCol(), at.getRow() - 1);
            pen.dot(at.getCol(), at.getRow() + 1);
        }
        for (double y = Math.ceil(yMin / yStep) * yStep; y <= yMax + yStep / 1e6; y += yStep) {
            Position at = toGrid(axisX, y);
            pen.dot(at.getCol() - 1, at.getRow());
            pen.dot(at.getCol() + 1, at.getRow());
        }
    }

    /** the tick labels: x ones in the character row below the x axis (above, when it runs along the bottom), y ones right of the y axis */
    private void labelAxes() {
        double axisY = axisY(), axisX = axisX();
        double xStep = xStep(), yStep = yStep();
        int cellsHigh = dots.getHeight() / dotsPerCellRow;
        int axisCellRow = cellRow(toGrid(0, axisY).getRow());
        int xLabelRow = axisCellRow + 1 < cellsHigh ? axisCellRow + 1 : axisCellRow - 1;
        for (double x = Math.ceil(xMin / xStep) * xStep; xTickLabels && x <= xMax + xStep / 1e6; x += xStep) {
            if (Math.abs(x - axisX) < xStep / 1e6) {
                continue;                                         // the corner: the y axis labels it
            }
            textInCell(xLabelRow, cellCol(toGrid(x, axisY).getCol()), label(x, xStep), axesColor);
        }
        int yLabelCol = cellCol(toGrid(axisX, 0).getCol()) + 1;
        for (double y = Math.ceil(yMin / yStep) * yStep; y <= yMax + yStep / 1e6; y += yStep) {
            textInCell(cellRow(toGrid(axisX, y).getRow()), yLabelCol, label(y, yStep), axesColor);
        }
    }

    private int cellRow(int dotRow) {
        return Math.floorDiv(dotRow - dots.getRow(), dotsPerCellRow);
    }

    private int cellCol(int dotCol) {
        return Math.floorDiv(dotCol - dots.getCol(), dotsPerCellCol);
    }

    /** a round step (1, 2 or 5 times a power of ten) giving about count steps across span */
    static double niceStep(double span, int count) {
        double raw = span / Math.max(1, count);
        double power = Math.pow(10, Math.floor(Math.log10(raw)));
        double fraction = raw / power;
        double nice = fraction < 1.5 ? 1 : fraction < 3.5 ? 2 : fraction < 7.5 ? 5 : 10;
        return nice * power;
    }

    /** a tick label with as many decimals as the step needs */
    static String label(double value, double step) {
        int decimals = Math.max(0, (int) Math.ceil(-Math.log10(step) - 1e-9));
        if (Math.abs(value) < step / 1e6) {
            value = 0;                                            // no "-0"
        }
        return String.format("%." + decimals + "f", value);
    }

    // ---- the mouse ------------------------------------------------------------------------------

    /** the grid dots of the character under the mouse; mouseOnCanvas: the mouse, relative to the screen */
    private Rectangle dotsUnder(Position mouseOnCanvas) {
        Position charactersOnCanvas = Component.UTIL.getCanvasOffsetFor(characters);
        if (mouseOnCanvas == null || charactersOnCanvas == null) {
            return null;
        }
        Position cell = mouseOnCanvas.offsetAdd(charactersOnCanvas.negate());
        return new Rectangle.Simple(dots.getRow() + cell.getRow() * dotsPerCellRow, dots.getCol() + cell.getCol() * dotsPerCellCol,
                                    dotsPerCellRow, dotsPerCellCol);
    }

    /**
     * The last drawn shape with a dot in these dots; failing that, the last one with a dot next to
     * them (GRAB_MARGIN) - exactly under the mouse wins over nearby, so the line clicked beats the
     * one below it.
     */
    private Shape<?> shapeAt(Rectangle under, boolean draggableOnly) {
        for (int margin = 0; margin <= GRAB_MARGIN; margin++) {
            Shape<?> found = shapeAt(under, draggableOnly, margin);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private Shape<?> shapeAt(Rectangle under, boolean draggableOnly, int margin) {
        for (int i = drawn.size() - 1; i >= 0; i--) {
            Drawn d = drawn.get(i);
            if (draggableOnly && !draggable.containsKey(d.shape)) {
                continue;
            }
            for (int row = under.getRow() - margin; row < under.getRow() + under.getHeight() + margin; row++) {
                for (int col = under.getCol() - margin; col < under.getCol() + under.getWidth() + margin; col++) {
                    if (row < dots.getRow() || col < dots.getCol() || row >= dots.getRow() + dots.getHeight() || col >= dots.getCol() + dots.getWidth()) {
                        continue;
                    }
                    if (d.dotsSet.get((row - dots.getRow()) * dots.getWidth() + (col - dots.getCol()))) {
                        return d.shape;
                    }
                }
            }
        }
        return null;
    }

    /** the last drawn shape at (or next to) a grid dot; null: none */
    Shape<?> shapeAt(Position dot) {
        return shapeAt(new Rectangle.Simple(dot.getRow(), dot.getCol(), 1, 1), false);
    }

    private Vec2<?> middleOf(Rectangle under) {
        return toData(new Position.Simple(under.getRow() + under.getHeight() / 2, under.getCol() + under.getWidth() / 2));
    }

    @Override
    public void onClick(EventBus eventBus) {
        // a click without a position: nothing to tell
    }

    @Override
    public void onClick(ApplicationMouseEvent e) {
        if (onClick == null) {
            return;
        }
        // a click comes relative to this component: make it relative to the screen
        Position meOnCanvas = Component.UTIL.getCanvasOffsetFor(this);
        if (meOnCanvas == null || e.getMousePosition() == null) {
            return;
        }
        Rectangle under = dotsUnder(e.getMousePosition().offsetAdd(meOnCanvas));
        if (under == null) {
            return;
        }
        onClick.accept(new Click(shapeAt(under, false), middleOf(under), e.getEventBus()));
    }

    @Override
    public boolean onDragStart(ApplicationMouseEvent e) {
        dragging = null;
        panning = false;
        Rectangle under = dotsUnder(e.getMousePosition());        // a drag comes relative to the screen
        if (under == null) {
            return false;
        }
        Shape<?> shape = shapeAt(under, true);
        if (shape == null) {
            if (!panByDrag) {
                return false;
            }
            panning = true;                                       // empty space: move the view
            panFrom = middleOf(under);
            return true;
        }
        dragging = DragMove.of(draggable.get(shape));             // its offset, in your numbers
        dragging.start(middleOf(under));
        return true;
    }

    @Override
    public boolean onDrag(ApplicationMouseEvent e) {
        Rectangle under = dotsUnder(e.getMousePosition());
        if (panning && under != null) {
            Vec2<?> now = middleOf(under);                        // the grabbed point goes back under the mouse
            pan(panFrom.getX() - now.getX(), panFrom.getY() - now.getY());
            e.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(this));
            return true;
        }
        if (dragging == null || under == null) {
            return false;
        }
        dragging.moveTo(middleOf(under));
        redraw();
        e.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(this));
        return true;
    }

    @Override
    public boolean onDragStop(ApplicationMouseEvent e) {
        boolean was = dragging != null || panning;
        dragging = null;
        panning = false;
        return was;
    }

    // ---- focus ----------------------------------------------------------------------------------

    @Override
    public Focusable asFocusable() {
        return focus.asFocusable();
    }

    @Override
    public KeyboardHandler asKeyboardHandler() {
        return focus.asKeyboardHandler();
    }
}
