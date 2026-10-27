package mizzen.components;

import java.util.Collection;

import mizzen.controlcodes.color.Color;
import mizzen.geom.Dimension;
import mizzen.geom.position.Position;
import mizzen.textsegments.IScreenLineSegmentsBuffer;
import mizzen.textsegments.ScreenLineSegment;
import mizzen.textsegments.ScreenLineSegment.SegmentImpl;

/**
 * A Component that draws a vertical line
 */
public class VerticalLine extends BoundedComponent<VerticalLine> {

    private char verticalLineChar = 0;

    public VerticalLine(Color color, Position pos, int len) {
        super(color, pos, new Dimension.Simple(len, 1));
    }

    public VerticalLine(Color color, int row, int col, int len) {
        super(color, new Position.Simple(row, col), new Dimension.Simple(len, 1));
    }

    public VerticalLine setVerticalLineChar(char verticalLineChar) {
        this.verticalLineChar = verticalLineChar;
        return this;
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        int bottomRow = this.getBottomMostRow();
        String vertCharStr = String.valueOf(this.verticalLineChar != 0 ? this.verticalLineChar : this.getBlockChars().getVERTICAL_LINE());
        Collection<ScreenLineSegment> newSegments = ScreenLineSegment.UTIL.makeCollection(null);
        for (int row = this.getRow(); row <= bottomRow; row++) {
            ScreenLineSegment dot = new SegmentImpl(color, row, getCol(), -1, vertCharStr);
            newSegments.add(dot);
        }
        segments.clearAndMerge(newSegments);
    }
}