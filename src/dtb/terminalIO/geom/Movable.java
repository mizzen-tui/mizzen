package dtb.terminalIO.geom;

/**
 * An object that can move itself.
 * 
 */
public interface Movable {

	public void moveTo(int row, int col);

	public void moveDelta(int rowOffset, int colOffset);
	
	
}
