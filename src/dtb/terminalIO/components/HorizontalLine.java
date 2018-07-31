package dtb.terminalIO.components;

import dtb.terminalIO.ControlCode.FGBGColors;
import dtb.terminalIO.IScreenLineSegmentsBuffer;
import dtb.terminalIO.ScreenLineSegment;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;

public class HorizontalLine extends BoundedComponent<HorizontalLine> {

    public HorizontalLine(FGBGColors color, Position pos, int len) {
        super(color, pos, new Dimension.Simple(1, len));
    }

    public HorizontalLine(FGBGColors color, int row, int col, int len) {
        super(color, new Position.Simple(row, col), new Dimension.Simple(1, len));
    }

    @Override
    protected void draw(FGBGColors color, IScreenLineSegmentsBuffer segments) {
        segments.clear();
        if (getWidth() > 0) {
    	  ScreenLineSegment line = ScreenLineSegment.UTIL.blankScreenObject(color, this.getBlockChars().getHORIZONTAL_LINE(), getRow(), getCol(), -1, getWidth());
    	  segments.add(line);
        }
    }

}