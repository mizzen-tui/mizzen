package mizzen.components;

import java.util.TreeSet;

import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.ColorParsingUtils;
import mizzen.controlcodes.color.ColorParsingUtils.ColorColorSegmentBuilder;
import mizzen.geom.Dimension;
import mizzen.geom.Rectangle;
import mizzen.textsegments.CleanString;
import mizzen.textsegments.IScreenLineSegmentsBuffer;
import mizzen.textsegments.ScreenLineSegment;
import mizzen.textsegments.ScreenLineSegment.SegmentImpl;

/**
 * A component that only allows you to append text.
 * 
 * Good for view logs
 * 
 *
 */
public class AppendOnlyComponent extends AbstractComponent<AppendOnlyComponent> {

    private Rectangle sizeOfSegments;
    private TreeSet<ScreenLineSegment> segments = ScreenLineSegment.UTIL.makeCollection(null);

    public AppendOnlyComponent(Color color, Dimension d) {
        super(color);
        sizeOfSegments = new Rectangle.Simple(d);
        this.setFillOnClear(false);
    }

    public AppendOnlyComponent(Color color) {
        this(color, new Rectangle.Simple(0,0,0,0));
    }
    
    public void clear() {
        super.clear();
        sizeOfSegments = new Rectangle.Simple(0,0,0,0);
        segments = ScreenLineSegment.UTIL.makeCollection(null);
    }

    public void appendText(String text) {
        
        if (text == null || text.length()==0) {
            return;
        }
        
        String[] lines = text.split("\n", -1);
        if (lines == null || lines.length == 0) {
            return;
        }
        
        for (int i=0; i< lines.length; i++) {
            appendLineRawText(lines[i]);
//            String knownClean = CleanString.UTIL.clean(lines[i]);
//            // here's a quirk .. if str known to be clean; lines[i] == knownClean
//            // then do not have to handle color
//            if (knownClean == lines[i]) {
//                appendLine( new CleanString.KnownCleanString(lines[i]));
//            } else {
//                appendLineRawText(lines[i]);
//            }
            //appendLine( new CleanString.Simple(lines[i]));
        }

        this.markAsDirty();
    }
    
    public void signalSizeChanged() {
        if (this.getParentContainer()!=null) {
            this.getParentContainer().childResized(this);
        }
    }
    
    private void appendLineRawText(String line) {
        int ypos = AppendOnlyComponent.this.getHeight();
        ColorParsingUtils.parseColorsAndBuildResult(line, new ColorColorSegmentBuilder() {
            int currentWidth = 0;
            @Override
            public boolean newColorSegment(Color color, CleanString simpleText) {
                color = color == null ? getColor() : color;
                ScreenLineSegment segment = new SegmentImpl(color, ypos, currentWidth, -1, simpleText);
                currentWidth += segment.getWidth();
                segments.add(segment);
                sizeOfSegments = sizeOfSegments.mergeAndExpandDimensionsWith(segment);
                return true; // true means keep parsing
            }
        });
    }
    
    private void appendLine(CleanString line) {
        if (line.length()<1) {
            sizeOfSegments = new Rectangle.Simple(  getRow(),  getCol(), getHeight() + 1, getWidth());
            return;
        }
        ScreenLineSegment segment = new SegmentImpl(getColor(), this.getHeight(), 0, -1, line);
        segments.add(segment);
        sizeOfSegments = sizeOfSegments.mergeAndExpandDimensionsWith(segment);
    }
    
    @Override
    public int getRow() {
        return 0;
    }

    @Override
    public int getCol() {
        return 0;
    }

    @Override
    public int getWidth() {
        return sizeOfSegments.getWidth();
    }

    @Override
    public int getHeight() {
        return sizeOfSegments.getHeight();
    }

    @Override
    public void moveTo(int row, int col) {
        // do nothing
    }



    @Override
    protected Rectangle getRectangleForClearAndFill() {
        return this;
    }

    @Override
    protected void clearBeforeDraw() {
        // we don't want to clear
    }


    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer mySegments) {
        mySegments.clearAndMerge(segments);
    }
    
}