package dtb.terminalIO.components;

import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.controlcodes.ControlCode.FGBGColors;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.textsegments.CleanString;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.textsegments.ScreenLineSegment;
import dtb.terminalIO.textsegments.ScreenLineSegment.SegmentImpl;
import dtb.terminalIO.textsegments.ScreenLineSegment.UTIL.SegmentsFromStringResult;

/**
 * A Component that renders a piece of text (a string).  
 * 
 * <p/>
 * 
 * CR's ('\n') in the text will will be respected and will
 * cause a multi-line component to be rendered.
 * 
 * <p/>
 *   
 * If the text has any recognized Color codes embedded in it,
 * these will be parsed and broken down internally into
 * the appropriate ScreenLineSegment objects.
 * 
 * <p/>
 *   
 * Translation .. the colors will be preserved.
 * 
 */
public class TextComponent extends BoundedComponent<TextComponent> {
    private String text;            // will be non-null only if we have color control codes.
    private CleanString cleanText;  // will be null if we have color control codes.
    private boolean hasColorCodes = false;
    
    public TextComponent(Color color, String text) {
        super(color, calcDimentionOfText(text));
        this.setText(text);
    }

    public TextComponent(Color color, CleanString text) {
        super(color, calcDimentionOfText(text));
        this.setText(text);
    }

    static private Dimension calcDimentionOfText(String text) {
	return Dimension.UTIL.stringToDimension(text);
    }

    static private Dimension calcDimentionOfText(CleanString text) {
        return Dimension.UTIL.stringToDimension(text);
    }
    
    public TextComponent(CleanString text) {
        this(FGBGColors.NO_COLOR_SPECIFIED, text);
    }
    
    public TextComponent(String text) {
        this(FGBGColors.NO_COLOR_SPECIFIED, text);
    }
    
    public TextComponent(Color color, int row, int col, String text) {
        super(color, new Rectangle.Simple(row, col, Dimension.UTIL.stringToDimension(text)));
        setText(text);
    }

    public TextComponent(Color color, int row, int col, CleanString text) {
        super(color, new Rectangle.Simple(row, col, Dimension.UTIL.stringToDimension(text)));
        setText(text);
    }

    public TextComponent(Color color, Dimension d, String text) {
        super(color,new Rectangle.Simple(d));
        setText(text);
    }

    public TextComponent(Color color, Dimension d, CleanString text) {
        super(color,new Rectangle.Simple(d));
        setText(text);
    }

    public TextComponent(Color color, Dimension d) {
        super(color,new Rectangle.Simple(d));
    }

    @Override
    public TextComponent setColor(Color drawingcolor) {
        super.setColor(drawingcolor);
        return this;
    }
    
    public Color getColor() {
        Color theColor = super.getColor();
        return theColor;
    }
    
    
    @Override
    public void draw(Color color, IScreenLineSegmentsBuffer segments) {
        SegmentsFromStringResult result = null;
        
        if (this.hasColorCodes) {
            result = ScreenLineSegment.UTIL.textSegmentsFromStringWithEmbeddedColor(this.text, 0, 0, this,
                    (strColor, p, txt) -> new SegmentImpl( strColor != null ? strColor : color, p, -1, txt));
        }  else {
            result = ScreenLineSegment.UTIL.textSegmentsFromString(this.getText(), 0, 0, this,  // TODO: more efficient way!
        	                                                   (p,s)->new SegmentImpl(color, p, -1, s));
        }
        if (result.size()!=0) {
            if ( this.isFillOnClear()) {
                this.clear();
                segments.merge(result.ts);
            } else {
                segments.clearAndMerge(result.ts);
            }
        }
    }

    public String getText() {
        return this.text != null ? text : (cleanText != null ? cleanText.getText() : null) ;
    }

    public void setTextAndResize(String text) {
        this.resize(Dimension.UTIL.stringToDimension(text));
        this.setText(text);
    }

    public void setTextAndResize(CleanString text) {
        this.resize(Dimension.UTIL.stringToDimension(text));
        this.setText(text);
    }

    public void setText(CleanString text) {
        this.cleanText = text;
        this.text = null;
        this.hasColorCodes = false;
        this.clear();
        this.markAsDirty();
    }

    public void setText(String text) {
        this.hasColorCodes = ControlCode.hasColorCodes(text);
        if (! this.hasColorCodes && CleanString.UTIL.isClean(text)) {
            setText( new CleanString.Simple(text) );
            return;
        }
        this.text = text;
        this.cleanText = null;
        this.clear();
        this.markAsDirty();
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