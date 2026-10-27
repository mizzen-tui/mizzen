package mizzen.components;

import mizzen.controlcodes.color.Color;
import mizzen.geom.Dimension;
import mizzen.geom.Rectangle;
import mizzen.textsegments.IScreenLineSegmentsBuffer;
import mizzen.textsegments.ScreenLineSegment;
import mizzen.textsegments.ScreenLineSegment.SegmentImpl;

/**
 * A Component that renders one character.
 */
public class CharComponent extends BoundedComponent<CharComponent> {
    char ch;
    
    public CharComponent(Color color, char ch, ParentContainer parentContainer) {
        super(color, new Dimension.Simple(1,1), parentContainer);
        this.ch = ch;
        this.setFillOnClear(false);
    }

    public CharComponent(Color color, char ch) {
        super(color, new Dimension.Simple(1,1));
        this.setFillOnClear(false);
        this.ch = ch;
    }

    public CharComponent(Color color, int row, int col, char ch, ParentContainer parentContainer) {
        super(color, new Rectangle.Simple(row, col, 1,1), parentContainer);
        this.setFillOnClear(false);
        this.ch = ch;
    }

    public CharComponent(Color color, int row, int col, char ch) {
        super(color, new Rectangle.Simple(row, col, 1,1));
        this.ch = ch;
    }

    public void setChar(char ch) {
        this.ch = ch;
        markAsDirty();
    }
    
    @Override
    public void draw(Color color, IScreenLineSegmentsBuffer segments) {
        ScreenLineSegment textSeg = new SegmentImpl(color, this, -1, String.valueOf(ch));
        segments.add(textSeg);
    }

}
