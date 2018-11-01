package dtb.terminalIO.components;

import dtb.terminalIO.ControlCode.Color;
import dtb.terminalIO.IScreenLineSegmentsBuffer;
import dtb.terminalIO.ScreenLineSegment;
import dtb.terminalIO.ScreenLineSegment.SegmentImpl;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;

/**
 * A Component that renders one character.
 */
public class CharComponent extends BoundedComponent<TextComponent> {
    char ch;
    
    public CharComponent(Color color, char ch) {
        super(color, new Dimension.Simple(1,1));
        this.ch = ch;
    }

    public CharComponent(Color color, int row, int col, char ch) {
        super(color, new Rectangle.Simple(row, col, 1,1));
        this.ch = ch;
    }

    @Override
    public void draw(Color color, IScreenLineSegmentsBuffer segments) {
        ScreenLineSegment textSeg = new SegmentImpl(color, this, -1, String.valueOf(ch));
        segments.add(textSeg);
    }

}
