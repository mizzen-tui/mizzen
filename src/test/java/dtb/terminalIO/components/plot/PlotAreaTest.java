package dtb.terminalIO.components.plot;

import dtb.terminalIO.components.plot.grid.GridContext.GRID_TYPE;
import dtb.terminalIO.components.plot.shape.Line;
import dtb.terminalIO.components.plot.shape.RectangleShape;
import dtb.terminalIO.components.plot.shape.Shape;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.geom.position.Vec2;
import junit.framework.TestCase;

/**
 * PlotArea: your numbers (y up) mapped onto its grid. These use the plain-character grid - one dot
 * per character - so what was plotted can be read back character by character.
 */
public class PlotAreaTest extends TestCase {

    /** an 11 x 11 plot showing x 0..10, y 0..10: one data unit per dot */
    private PlotArea area(int size) {
        return new PlotArea(Colors.Gre, new Dimension.Simple(size, size), GRID_TYPE.SIMPLE_GRID_CTX)
                .showing(0, 10, 0, 10);
    }

    private static Plotter characters(PlotArea area) {
        return (Plotter) area.getWrappedComponent();
    }

    public void testToGridAndBackAgain() {
        PlotArea area = area(11);
        Vec2<?> back = area.toData(area.toGrid(3, 7));
        assertEquals(3f, back.getX(), 1e-4);
        assertEquals(7f, back.getY(), 1e-4);
    }

    public void testYIsUp() {
        PlotArea area = area(11);
        assertEquals("y 10 is the top row", 0, area.toGrid(0, 10).getRow());
        assertEquals("y 0 is the bottom row", 10, area.toGrid(0, 0).getRow());
        assertEquals("x 0 is the left column", 0, area.toGrid(0, 0).getCol());
    }

    public void testEqualScalingKeepsProportionsAndCentres() {
        PlotArea area = new PlotArea(Colors.Gre, new Dimension.Simple(20, 20), GRID_TYPE.BRAILLE_GRID_CTX)   // 40 x 80 dots
                .showing(0, 10, 0, 10)
                .scaling(PlotArea.Scaling.EQUAL);
        Position origin = area.toGrid(0, 0);
        int tenAcross = area.toGrid(10, 0).getCol() - origin.getCol();
        int tenUp = origin.getRow() - area.toGrid(0, 10).getRow();
        assertEquals("ten units as wide as they are tall (Braille dots are square)", tenAcross, tenUp, 1);
        // 40 dots wide, 80 tall: the width is filled, the height centred
        assertEquals("fills the width", 0, origin.getCol());
        assertTrue("a margin above", area.toGrid(0, 10).getRow() > 0);
        assertEquals("as much below", area.toGrid(0, 10).getRow(), 79 - origin.getRow(), 1);
    }

    public void testAShapeIsDrawnInDataUnits() {
        PlotArea area = area(11).onDraw((pen) -> pen.draw(new Line(0, 0, 10, 10)));
        for (int y = 0; y <= 10; y++) {
            Position at = area.toGrid(y, y);
            assertNotNull("the diagonal at " + y, characters(area).getCharAt(at.getRow(), at.getCol()));
        }
        assertNull("off the line", characters(area).getCharAt(area.toGrid(2, 8).getRow(), area.toGrid(2, 8).getCol()));
    }

    /** a zoomed shape is scaled at its corners - so it has no gaps (moving each plotted dot would leave them) */
    public void testAZoomedShapeHasNoGaps() {
        PlotArea area = new PlotArea(Colors.Gre, new Dimension.Simple(21, 21), GRID_TYPE.SIMPLE_GRID_CTX)
                .showing(0, 2, 0, 2)                                       // ten dots per data unit
                .onDraw((pen) -> pen.draw(new Line(0f, 0f, 2f, 2f, null)));
        for (int row = 0; row <= 20; row++) {
            boolean found = false;
            for (int col = 0; col <= 20; col++) {
                found |= characters(area).getCharAt(row, col) != null;
            }
            assertTrue("row " + row + " has the line", found);
        }
    }

    public void testAClickFindsTheLastDrawnShapeUnderIt() {
        Shape<?> back = new RectangleShape(new Rectangle.Simple(2, 2, 5, 5)).setFilled(true);
        Shape<?> front = new Line(0, 5, 10, 5);
        PlotArea area = area(11).onDraw((pen) -> pen.draw(back).draw(front));
        assertSame("on the line, drawn last", front, area.shapeAt(area.toGrid(5, 5)));
        assertSame("on the rectangle only", back, area.shapeAt(area.toGrid(4, 3)));
        assertNull("on nothing", area.shapeAt(area.toGrid(9, 9)));
    }

    public void testTextReplacesWhatTheCharacterShowed() {
        PlotArea area = area(11).onDraw((pen) -> pen.line(0, 5, 10, 5).text(2, 5, "hi"));
        Position at = area.toGrid(2, 5);
        assertEquals("h", characters(area).getCharAt(at.getRow(), at.getCol()));
        assertEquals("i", characters(area).getCharAt(at.getRow(), at.getCol() + 1));
    }

    public void testFunctionAndFillUnder() {
        PlotArea area = area(11).onDraw((pen) -> pen.fillUnder((x) -> 4, 2, 6));
        assertNotNull("under the curve", characters(area).getCharAt(area.toGrid(4, 2).getRow(), area.toGrid(4, 2).getCol()));
        assertNull("above the curve", characters(area).getCharAt(area.toGrid(4, 6).getRow(), area.toGrid(4, 6).getCol()));
        assertNull("outside from..to", characters(area).getCharAt(area.toGrid(8, 2).getRow(), area.toGrid(8, 2).getCol()));
    }

    public void testNiceSteps() {
        assertEquals(1.0, PlotArea.niceStep(10, 10), 1e-9);
        assertEquals(0.5, PlotArea.niceStep(2 * Math.PI, 12), 1e-9);
        assertEquals(20.0, PlotArea.niceStep(100, 5), 1e-9);
        assertEquals("0.5", PlotArea.label(0.5, 0.5));
        assertEquals("no minus zero", "0.0", PlotArea.label(-1e-12, 0.5));
        assertEquals("3", PlotArea.label(3, 1));
    }
}
