package dtb.terminalIO.components;

import java.util.TreeSet;

import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.textsegments.CleanString;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.textsegments.ScreenLineSegment;
import dtb.terminalIO.textsegments.ScreenLineSegment.SegmentImpl;

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
    }

    public AppendOnlyComponent(Color color) {
        this(color, new Rectangle.Simple(0,0,0,0));
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
            appendLine( new CleanString.Simple(lines[i]));
        }

        this.markAsDirty();
    }
    
    public void signalSizeChanged() {
        if (this.getParentContainer()!=null) {
            this.getParentContainer().childResized(this);
        }
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