package dtb.terminalIO.components;

import dtb.terminalIO.ControlCode;
import dtb.terminalIO.ControlCode.Color;
import dtb.terminalIO.ControlCode.FGBGColors;
import dtb.terminalIO.IScreenLineSegmentsBuffer;
import dtb.terminalIO.ScreenLineSegment;
import dtb.terminalIO.ScreenLineSegment.SegmentImpl;
import dtb.terminalIO.ScreenLineSegment.UTIL.SegmentsFromStringResult;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;

/**
 * A Component that renders a piece of text.  CR's in the text will will be respected and will
 * cause a multi-line component to be rendered.
 */
public class TextComponent extends BoundedComponent<TextComponent> {
    private String text;
    private boolean hasColorCodes = false;
    
    public TextComponent(Color color, String text) {
        super(color, calcDimentionOfText(text));
        this.setText(text);
    }

    static private Dimension calcDimentionOfText(String text) {
	return Dimension.UTIL.stringToDimension(text);
    }
    
    public TextComponent(String text) {
        this(FGBGColors.NO_COLOR_SPECIFIED, text);
    }

    
    public TextComponent(Color color, int row, int col, String text) {
        super(color, new Rectangle.Simple(row, col, Dimension.UTIL.stringToDimension(text)));
        setText(text);
    }

    public TextComponent(Color color, Dimension d, String text) {
        super(color,new Rectangle.Simple(d));
        setText(text);
    }

    public TextComponent(Color color, Dimension d) {
        super(color,new Rectangle.Simple(d));
    }

    @Override
    public void draw(Color color, IScreenLineSegmentsBuffer segments) {
        SegmentsFromStringResult result = null;
        if (this.hasColorCodes) {
            result = ScreenLineSegment.UTIL.textSegmentsFromStringWithEmbeddedColor(this.text, 0, 0, this,
                    (strColor, p, txt) -> new SegmentImpl( strColor != null ? strColor : color, p, -1, txt));
        } else {
            result = ScreenLineSegment.UTIL.textSegmentsFromString(this.text, 0, 0, this, 
        	                                                   (p,s)->new SegmentImpl(color, p, -1, s));
        }
        if (result.size()!=0) {
            segments.merge(result.ts);
        }
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
        this.hasColorCodes = ControlCode.hasColorCodes(text);
    }

    @Override
    public String toString() {
        return super.toString().replace("TextComponent[","TextComponent["+getText()+"][");
    }
    
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((text == null) ? 0 : text.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TextComponent other = (TextComponent) obj;
        if (text == null) {
            if (other.text != null)
                return false;
        } else if (!text.equals(other.text))
            return false;
        return true;
    }

    
    
}