package dtb.terminalIO.components;

import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.controlcodes.ControlCode.FGBGColors;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.textsegments.ScreenLineSegment;

/*
 * A component that draws a horizontal line
 */
public class HorizontalLine extends BoundedComponent<HorizontalLine> {

    private char horizontalLineChar = 0;
    
    public HorizontalLine(Color color, Position pos, int len) {
        super(color, pos, new Dimension.Simple(1, len));
    }

    public HorizontalLine(Color color, int len) {
        super(color, Position.ZERO, new Dimension.Simple(1, len));
    }

    public HorizontalLine(int len) {
        super(FGBGColors.NO_COLOR_SPECIFIED, Position.ZERO, new Dimension.Simple(1, len));
    }

    public HorizontalLine(Color color, int row, int col, int len) {
        super(color, new Position.Simple(row, col), new Dimension.Simple(1, len));
    }

    public HorizontalLine setHorizontalLineChar(char horizontalLineChar) {
        this.horizontalLineChar = horizontalLineChar;
        return this;
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        segments.clear();
        if (getWidth() > 0) {
          char horizChar = this.horizontalLineChar != 0 ? this.horizontalLineChar : this.getBlockChars().getHORIZONTAL_LINE();
    	  ScreenLineSegment line = ScreenLineSegment.UTIL.blankScreenObject(color, horizChar, getRow(), getCol(), -1, getWidth());
    	  segments.add(line);
        }
    }

}