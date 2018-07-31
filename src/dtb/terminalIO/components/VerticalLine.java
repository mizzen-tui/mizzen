package dtb.terminalIO.components;

import dtb.terminalIO.ControlCode.FGBGColors;
import dtb.terminalIO.CursorPos;
import dtb.terminalIO.IScreenLineSegmentsBuffer;
import dtb.terminalIO.ScreenLineSegment;
import dtb.terminalIO.ScreenLineSegment.SegmentImpl;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;

public class VerticalLine extends BoundedComponent<VerticalLine> {

    public VerticalLine(FGBGColors color, Position pos, int len) {
	super(color, pos, new Dimension.Simple(len, 1));
    }

    public VerticalLine(FGBGColors color, int row, int col, int len) {
	super(color, new Position.Simple(row, col), new Dimension.Simple(len, 1));
    }

    @Override
    protected void draw(FGBGColors color, IScreenLineSegmentsBuffer segments) {
	int bottomRow = this.getBottomMostRow();
	for (int row = this.getRow(); row <= bottomRow; row++) {
	    ScreenLineSegment dot = new SegmentImpl(color, new CursorPos(row, getCol()), -1, String.valueOf(this.getBlockChars().getVERTICAL_LINE()));
	    segments.add(dot);
	}
    }
}