package mizzen.textsegments;

import mizzen.controlcodes.color.Color;
import mizzen.geom.position.Position;

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

    public Position getPosition() {
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
        return String.format("(row=%d,col=%d,width=%d,'%s')", getRow(), getCol(), getWidth(), getText());
    }

    @Override
    public Color getColors() {
        return color;
    }

    @Override
    public void moveTo(int row, int col) {
        throw new IllegalStateException("No implemented yet!");
    }
}