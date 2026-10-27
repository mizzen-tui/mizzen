package mizzen.components;

import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.geom.Dimension;
import mizzen.geom.position.Position;
import mizzen.textsegments.IScreenLineSegmentsBuffer;
import mizzen.textsegments.ScreenLineSegment;

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
        super(Colors.FGBGColors.NO_COLOR_SPECIFIED, Position.ZERO, new Dimension.Simple(1, len));
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
    	  // use the fast TreeSet add
    	  segments.getSegments().add(line);
        } 
    }

}