package mizzen.core;

import mizzen.geom.position.Position;

/**
 * Just a Position object -- do I really need it? I think not! delete please!!
 */
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
