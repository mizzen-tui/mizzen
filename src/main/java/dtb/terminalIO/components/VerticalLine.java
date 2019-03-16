package dtb.terminalIO.components;

import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.core.CursorPos;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.textsegments.ScreenLineSegment;
import dtb.terminalIO.textsegments.ScreenLineSegment.SegmentImpl;

/**
 * A Component that draws a vertical line
 */
public class VerticalLine extends BoundedComponent<VerticalLine> {

    private char verticalLineChar;
    
    public VerticalLine(Color color, Position pos, int len) {
	super(color, pos, new Dimension.Simple(len, 1));
	this.setVerticalLineChar( this.getBlockChars().getVERTICAL_LINE() );
    }

    public VerticalLine(Color color, int row, int col, int len) {
	super(color, new Position.Simple(row, col), new Dimension.Simple(len, 1));
        this.setVerticalLineChar( this.getBlockChars().getVERTICAL_LINE() );
    }

    public char getVerticalLineChar() {
        return verticalLineChar;
    }

    public VerticalLine setVerticalLineChar(char verticalLineChar) {
        this.verticalLineChar = verticalLineChar;
        return this;
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
	int bottomRow = this.getBottomMostRow();
	for (int row = this.getRow(); row <= bottomRow; row++) {
	    ScreenLineSegment dot = new SegmentImpl(color, new CursorPos(row, getCol()), -1, String.valueOf(getVerticalLineChar()));
	    segments.add(dot);
	}
    }
}