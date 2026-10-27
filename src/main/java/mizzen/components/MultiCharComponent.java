package mizzen.components;

import mizzen.controlcodes.color.Color;
import mizzen.geom.Dimension;
import mizzen.geom.Rectangle;
import mizzen.textsegments.IScreenLineSegmentsBuffer;
import mizzen.textsegments.ScreenLineSegment;

/**
 * Similar to a {@link CharComponent} - but displays a rectangle of a specified character
 *
 */
public class MultiCharComponent extends BoundedComponent<MultiCharComponent> {
    char ch;
    
    public MultiCharComponent(Color color, char ch, Rectangle rect) {
        super(color, rect);
        this.setFillOnClear(false);
        this.ch = ch;
    }


    public MultiCharComponent(Color color, int row, int col, char ch, Dimension d, ParentContainer parentContainer) {
        super(color, new Rectangle.Simple(row, col, 1,1), parentContainer);
        this.setFillOnClear(false);
        this.ch = ch;
    }

    public MultiCharComponent(Color color, char ch, Dimension d, ParentContainer parentContainer) {
        this(color, 0,0, ch, new Dimension.Simple(d), parentContainer);
    }

    public MultiCharComponent(Color color, char ch, Dimension d) {
        this(color, ch, new Dimension.Simple(1,1), null);
    }

    public void setChar(char ch) {
        this.ch = ch;
        markAsDirty();
    }
    
    @Override
    public void draw(Color color, IScreenLineSegmentsBuffer segments) {
        ScreenLineSegment.UTIL.fillRectWithCharFastAdd(color, this.ch , (Rectangle)this, segments.getSegments());
    }

}
