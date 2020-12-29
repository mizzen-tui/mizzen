package dtb.terminalIO.components;

import java.util.TreeSet;

import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.ColorParsingUtils;
import dtb.terminalIO.controlcodes.color.Colors;
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
 * CR's ('\n') in the text will be respected and will
 * cause a multi-line text component to be rendered.
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
    private TreeSet<ScreenLineSegment> parsedSegments;

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
        this(Colors.FGBGColors.NO_COLOR_SPECIFIED, text);
    }
    
    public TextComponent(String text) {
        this(Colors.FGBGColors.NO_COLOR_SPECIFIED, text);
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
        this.parsedSegments = null;
        return this;
    }
    
    public Color getColor() {
        Color theColor = super.getColor();
        return theColor;
    }
    
    
    @Override
    public void draw(Color color, IScreenLineSegmentsBuffer segments) {
        
        SegmentsFromStringResult result;
        
        if (parsedSegments == null) {
            if (this.hasColorCodes) {
                result = ScreenLineSegment.UTIL.textSegmentsFromStringWithEmbeddedColor(this.text, 0, 0, this,
                        (strColor, p, txt) -> new SegmentImpl( strColor != null ? strColor : color, p, -1, txt));
            }  else {
                result = ScreenLineSegment.UTIL.textSegmentsFromString(this.getText(), 0, 0, this,  // TODO: more efficient way!
            	                                                   (p,s)->new SegmentImpl(color, p, -1, s));
            }
            // keeping a copy around to avoid having to reparse the text on
            // subsequent draw requests.  parsedSegments null'd if text is changed which causes parseSegments to rebuilt.
            parsedSegments = ScreenLineSegment.UTIL.makeCollection(result.ts);
        } else {
            result = new SegmentsFromStringResult(ScreenLineSegment.UTIL.makeCollection(parsedSegments));
        }
        
        if (result.ts.size()!=0) {
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

    public synchronized void setTextAndResize(String text) {
        this.resize(Dimension.UTIL.stringToDimension(text));
        this.setText(text);
    }

    public void setTextAndResize(CleanString text) {
        this.resize(Dimension.UTIL.stringToDimension(text));
        this.setText(text);
    }

    public void resize(Dimension d) {
        if (d != null && d.isSameDimension(this)) {
            return;
        }
        super.resize(d);
        parsedSegments = null;
    }
    
    public void setText(CleanString text) {
        if (text == null) {
            throw new NullPointerException("attempting to set null text on TextComponent!");
        }
        this.cleanText = text;
        this.text = null;
        this.hasColorCodes = false;
        this.clear();
        parsedSegments = null;
        this.markAsDirty();
    }

    public void setText(String text) {
        if (text == null) {
            throw new NullPointerException("attempting to set null text on TextComponent!");
        }
        this.hasColorCodes = ColorParsingUtils.hasColorCodes(text);
        if (! this.hasColorCodes && CleanString.UTIL.isClean(text)) {
            setText( new CleanString.Simple(text) );
            return;
        }
        this.text = text;
        this.cleanText = null;
        this.clear();
        parsedSegments = null;
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