package dtb.terminalIO.components;

import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.textsegments.ScreenLineSegment;
import dtb.terminalIO.textsegments.ScreenLineSegment.SegmentImpl;
import dtb.terminalIO.traverse.HasChildren;

/**
 * A Component that renders one character.
 */
public class CharComponent extends BoundedComponent<CharComponent> {
    char ch;
    
    public CharComponent(Color color, char ch, ParentContainer parentContainer) {
        super(color, new Dimension.Simple(1,1), parentContainer);
        this.ch = ch;
    }

    public CharComponent(Color color, char ch) {
        super(color, new Dimension.Simple(1,1));
        this.ch = ch;
    }

    public CharComponent(Color color, int row, int col, char ch, ParentContainer parentContainer) {
        super(color, new Rectangle.Simple(row, col, 1,1), parentContainer);
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
