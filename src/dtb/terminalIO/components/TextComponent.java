package dtb.terminalIO.components;

import dtb.terminalIO.ControlCode.FGBGColors;
import dtb.terminalIO.IScreenLineSegmentsBuffer;
import dtb.terminalIO.ScreenLineSegment;
import dtb.terminalIO.ScreenLineSegment.SegmentImpl;
import dtb.terminalIO.ScreenLineSegment.UTIL.SegmentsFromStringResult;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;

public class TextComponent extends BoundedComponent<TextComponent> {
    private String text;
    
    public TextComponent(FGBGColors color, String text) {
        super(color, Dimension.UTIL.stringToDimension(text));
        this.text = text;
    }

    public TextComponent(String text) {
        this(FGBGColors.NO_COLOR_SPECIFIED, text);
    }

    
    public TextComponent(FGBGColors color, int row, int col, String text) {
        super(color, new Rectangle.Simple(row, col, Dimension.UTIL.stringToDimension(text)));
        this.text = text;
    }

    public TextComponent(FGBGColors color, Dimension d, String text) {
        super(color,new Rectangle.Simple(d));
        this.text = text;
    }

    public TextComponent(FGBGColors color, Dimension d) {
        super(color,new Rectangle.Simple(d));
    }

    @Override
    public void draw(FGBGColors color, IScreenLineSegmentsBuffer segments) {
        SegmentsFromStringResult result = ScreenLineSegment.UTIL.textSegmentsFromString(this.text, 0, 0, this, 
        	                                                                        (p,s)->new SegmentImpl(color, p, -1, s));
        if (result.size()!=0) {
            segments.merge(result.ts);
        }
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    
    
}