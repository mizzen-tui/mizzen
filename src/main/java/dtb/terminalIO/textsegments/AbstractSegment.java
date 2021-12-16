package dtb.terminalIO.textsegments;

import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.geom.position.Position;

abstract class AbstractSegment implements ScreenLineSegment {
    Color color;
//    Position   cursorPos;
    int row;
    int col;

    public AbstractSegment(Color color, int row, int col) {
        this.color = color;
        this.row = row;
        this.col = col;
    }
    
    public AbstractSegment(Color color, Position cursorPos) {
        super();
        this.color = color;
        //this.cursorPos = new Position.Simple(cursorPos); // very important to make a copy
        this.row = cursorPos.getRow();
        this.col = cursorPos.getCol();
    }

    public Color getColor() {
        return color;
    }

    public Position getCursorPos() {
        return new Position.Simple(row,col);
    }

    @Override
    public int length() {
        return this.getText().length();
    }

    @Override
    public int getRow() {
        return row;
    }

    @Override
    public int getCol() {
        return col;
    }

    public String toString() {
        return String.format("x(row=%d,lcol=%d,rcol=%d,[id=%d]):%s", getRow(), getCol(), getRightMostCol(), getId(), getText());
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