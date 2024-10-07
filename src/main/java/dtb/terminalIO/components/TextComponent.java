package dtb.terminalIO.components;

import java.util.TreeSet;

import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.ColorParsingUtils;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.CleanString;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.textsegments.ScreenLineSegment;
import dtb.terminalIO.textsegments.ScreenLineSegment.SegmentImpl;
import dtb.terminalIO.textsegments.ScreenLineSegment.UTIL.SegmentsFromStringResult;
import dtb.terminalIO.util.StringUtils;

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

    public void moveTo(int row, int col) {
        if (row != getRow() || col != getCol()) {
            this.parsedSegments = null;
            markAsDirty();
        }
        super.moveTo(row, col);
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
            // subsequent draw requests.  parsedSegments null'd if text is changed which causes parseSegments to be rebuilt.
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

    
    // an experiment
    private  void appendText(String text) {
        if (this.text == null || parsedSegments == null) {
            if (this.text == null ) {
                text = this.text + text;
            }
            setTextAndResize(text);
            return;
        }

        // we want this next bit of code to be very fast
        // so we just append to existing non-null parsedSegments

        // TODO:  this code does not handle embedded color codes - need to fix that

        String[] lines = text.split("\n", -1);
        if (lines == null || lines.length == 0) {
            return;
        }

        int newWidth = this.getWidth();
        int newHeight = this.getHeight();

        Position nextPosition = parsedSegments.last().getPostion_BottomRight().addCols(1);
        ScreenLineSegment segment = new SegmentImpl(getColor(), nextPosition, -1, lines[0]) ;
        parsedSegments.add( segment );
        newWidth = Math.max(newWidth, segment.getRightMostCol()+1);
        nextPosition = new Position.Simple(nextPosition.getRow()+1, 0);
        for (int i=1;i<lines.length;i++) {
            segment = new SegmentImpl(getColor(), nextPosition, -1, lines[i]);
            newWidth = Math.max(newWidth, segment.getRightMostCol()+1);
            newHeight = Math.max(newHeight, segment.getBottomMostRow()+1);
            parsedSegments.add( segment );
            nextPosition = nextPosition.addRows(1);
        }

        Dimension newDim = new Dimension.Simple(newHeight, newWidth);
        if (! newDim.isSameDimension(this)) {
            TreeSet<ScreenLineSegment> savedParsedSegments = parsedSegments;
            // resize nulls out parsedSegments ( in this case, we dont want that - so this hack)
            this.resize(newDim);
            parsedSegments = savedParsedSegments; 
        }

        this.markAsDirty();

    }

    public String toStringDetails() {
        return "["+StringUtils.truncate(getText(),10)+"]";
    }


//    @Override
//    public int hashCode() {
//        final int prime = 31;
//        int result = 1;
//        result = prime * result + ((cleanText == null) ? 0 : cleanText.hashCode());
//        result = prime * result + (hasColorCodes ? 1231 : 1237);
//        result = prime * result + ((parsedSegments == null) ? 0 : parsedSegments.hashCode());
//        result = prime * result + ((text == null) ? 0 : text.hashCode());
//        return result;
//    }
//
//    @Override
//    public boolean equals(Object obj) {
//        if (this == obj)
//            return true;
//        if (obj == null)
//            return false;
//        if (getClass() != obj.getClass())
//            return false;
//        TextComponent other = (TextComponent) obj;
//        if (cleanText == null) {
//            if (other.cleanText != null)
//                return false;
//        } else if (!cleanText.equals(other.cleanText))
//            return false;
//        if (hasColorCodes != other.hasColorCodes)
//            return false;
//        if (parsedSegments == null) {
//            if (other.parsedSegments != null)
//                return false;
//        } else if (!parsedSegments.equals(other.parsedSegments))
//            return false;
//        if (text == null) {
//            if (other.text != null)
//                return false;
//        } else if (!text.equals(other.text))
//            return false;
//        return true;
//    }
//    
}