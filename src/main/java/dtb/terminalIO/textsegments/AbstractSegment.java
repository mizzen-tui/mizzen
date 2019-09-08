package dtb.terminalIO.textsegments;

import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.geom.position.Position;

abstract class AbstractSegment implements ScreenLineSegment {
    Color color;
    Position   cursorPos;

    public AbstractSegment(Color color, Position cursorPos) {
        super();
        this.color = color;
        this.cursorPos = new Position.Simple(cursorPos); // very important to make a copy
    }

    public Color getColor() {
        return color;
    }

    public Position getCursorPos() {
        return cursorPos;
    }

    @Override
    public int length() {
        return this.getText().length();
    }

    @Override
    public int getRow() {
        return getCursorPos().getRow();
    }

    @Override
    public int getCol() {
        return getCursorPos().getCol();
    }

    public String toString() {
        return String.format("(row=%d,lcol=%d,rcol=%d,[id=%d]):%s", getRow(), getCol(), getRightMostCol(), getId(), getText());
    }

    @Override
    public Color getColors() {
        return color;
    }

    @Override
    public void moveTo(int row, int col) {
        // TODO: this.getCursorPos().moveTo(row, col);
        throw new IllegalStateException("No implemented yet!");
    }
}