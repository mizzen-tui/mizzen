package dtb.terminalIO.core;

import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.ScreenLineSegment;

// used?
public class LayeredCanvasBackend implements CanvasBackend, Rectangle {

    private Canvas baseCanvas;
    private Canvas sourceCanvas;
    private int rowOffset;
    private int colOffset;

    public LayeredCanvasBackend(Canvas baseCanvas, int rowOffset, int colOffset) {
        super();
        if (baseCanvas.getHeight() < rowOffset)
            throw new IllegalArgumentException("baseCanvas.height<rowOffset");
        if (baseCanvas.getWidth() < colOffset)
            throw new IllegalArgumentException("baseCanvas.width<colOffset");
        this.baseCanvas = baseCanvas;
        this.rowOffset = rowOffset;
        this.colOffset = colOffset;
    }

    @Override
    public void writeToBackend(ScreenLineSegment sc) {
        if (sc.getColors().isMergeable())
            throw new IllegalStateException("Huh? \n" + String.valueOf(sc) + "\n" + String.valueOf(sc.getColors()));

        baseCanvas.writeToScreen(rowOffset, colOffset, sc);
    }

    @Override
    public void signal_eol() {
        // do nothing
    }

    @Override
    public void refresh() {
        baseCanvas.refresh();
    }

    public Canvas getSourceCanvas() {
        return sourceCanvas;
    }

    public void setSourceCanvas(Canvas sourceCanvas) {
        this.sourceCanvas = sourceCanvas;
    }

    @Override
    public void cr() {
        // do nothing
    }

    @Override
    public int getCol() {
        return baseCanvas.asRectangle().getCol() + colOffset;
    }

    @Override
    public int getRow() {
        return baseCanvas.asRectangle().getRow() + rowOffset;
    }

    @Override
    public int getWidth() {
        return baseCanvas.asRectangle().getRightMostCol() - getCol() + 1;
    }

    @Override
    public int getHeight() {
        return baseCanvas.asRectangle().getBottomMostRow() - getRow() + 1;
    }

    protected Rectangle getChildCanvasAsOffsetRectangle() {
        if (getSourceCanvas() == null) {
            throw new NullPointerException(
                    "Cannot perform remove or move operations " + "if source (or child) canvas has not been set.  "
                            + "call setSourceCanvas(Canvas sourceCanvas) first!");
        }

        Position position = this;
        Dimension dimension = getSourceCanvas();
        Rectangle r = new Rectangle.Simple(position, dimension);

        return r;
    }

    public void removeChildCanvas() {
        baseCanvas.getSegmentsBuffer().removeRect(getChildCanvasAsOffsetRectangle());
    }

    public void removeChildCanvasAndRefresh() {
        removeChildCanvas();
        refresh();
    }

    @Override
    public void moveTo(int row, int col) {
        // warning this clears the whole area of the screen -- ie more than just
        // the child
        removeChildCanvas();
        this.rowOffset = row;
        this.colOffset = col;
        this.getSourceCanvas().refresh();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + ": [row=" + getRow() + ", lcol" + getCol() + ", rcol="
                + getRightMostCol() + " width=" + getWidth() + ", height=" + getHeight() + "]";
    }

    @Override
    public void showCursorAt(Position showCursorPosition, boolean showCursor) {
        // TODO Auto-generated method stub

    }

    @Override
    public void hideCursor() {
        // TODO Auto-generated method stub

    }

    @Override
    public void showCursor() {
        // TODO Auto-generated method stub

    }

    @Override
    public void setSize(int height, int width) {
        // TODO Auto-generated method stub
        
    }

}