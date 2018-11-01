package dtb.terminalIO;

import dtb.terminalIO.geom.Position;

public class CursorPos extends Position.Simple {

    public CursorPos() {
	super(Position.ZERO);
    }

    public CursorPos(int row, int col) {
	super(row, col);
    }

    public CursorPos moveRightCols(int cols) {
	return new CursorPos(getRow(), getCol() + cols);
    }

}
