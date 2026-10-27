package mizzen.geom;

import mizzen.geom.position.Position;

/**
 * An object that can move itself.
 * 
 */
public interface Movable {


    default public void moveTo(Position offset) {
        this.moveTo(offset.getRow(), offset.getCol());
    }

	public void moveTo(int row, int col);

    default public void moveDelta(Position offset) {
        this.moveDelta(offset.getRow(), offset.getCol());
    }

    public void moveDelta(int rowOffset, int colOffset);
	
	
}
