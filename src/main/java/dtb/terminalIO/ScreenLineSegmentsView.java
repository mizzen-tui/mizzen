package dtb.terminalIO;

import java.io.ByteArrayOutputStream;
import java.util.Collection;

import dtb.terminalIO.ControlCode.Color;
import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.BoundedComponent;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.geom.Rectangle;

/**
 * IScreenLineSegmentsBuffer that has a view into a 'source'
 * IScreenLineSegmentsBuffer object
 *
 */
public class ScreenLineSegmentsView extends IScreenLineSegmentsBuffer.Delegate {

    private IScreenLineSegmentsBuffer sourceSegments;
    //private ScreenLineSegmentsBuffer  viewSegments;

    private Position                  offsetIntoSource;
    private Dimension                 maxsize;

    public ScreenLineSegmentsView(IScreenLineSegmentsBuffer sourceSegments, Position offsetIntoSource, Dimension maxsize) {
        super(new ScreenLineSegmentsBuffer());
        this.sourceSegments = sourceSegments;
        //this.viewSegments = (ScreenLineSegmentsBuffer) this.getDelegate();
        this.offsetIntoSource = new Position.Simple(offsetIntoSource);
        this.maxsize = new Dimension.Simple(maxsize);
        popuateViewFromSource();
    }

    Rectangle rectangeFromOffsetAndDimension() {
        return new Rectangle.Simple(offsetIntoSource, maxsize);
    }
    

    public Position getOffsetIntoSource() {
        return offsetIntoSource;
    }

    public void setOffsetIntoSource(Position offsetIntoSource) {
        this.offsetIntoSource = offsetIntoSource;
    }

    private void popuateViewFromSource() {
        this.clear();
        Rectangle bounds = rectangeFromOffsetAndDimension();
        for (ScreenLineSegment sourceSegment : this.sourceSegments.getSegments()) {
            if (Rectangle.UTIL.hasOverlap(bounds, sourceSegment)) {
                this.add(new ScreenLineSegmentView(sourceSegment));
            }
        }
        segmentsUpToDate = true;
    }

    boolean segmentsUpToDate = false;
    
    @Override
    public Collection<ScreenLineSegment> getSegments() {
        if (segmentsUpToDate) {
            return super.getSegments();
        }
        popuateViewFromSource();
        return super.getSegments();
    }

    
    public ScreenLineSegmentsView(IScreenLineSegmentsBuffer sourceSegments, Rectangle bounds) {
        this(sourceSegments, bounds, bounds);
    }

    public ScreenLineSegmentsView(IScreenLineSegmentsBuffer sourceSegments, Dimension maxsize) {
        this(sourceSegments, Position.ZERO, maxsize);
    }

    class ScreenLineSegmentView implements ScreenLineSegment {

        ScreenLineSegment sourceSegment;

        ScreenLineSegmentView(ScreenLineSegment sourceSegment) {
            this.sourceSegment = sourceSegment;
        }

        public Color getColor() {
            return sourceSegment.getColors();
        }

        @Override
        public int getLength() {
            return this.getText().length();
        }

        @Override
        public int getRow() {
            int row = sourceSegment.getRow() - ScreenLineSegmentsView.this.offsetIntoSource.getRow();
            return row < 0 ? 0 : row;
        }

        @Override
        public int getCol() {
            int col = sourceSegment.getCol() - ScreenLineSegmentsView.this.offsetIntoSource.getCol();
            return col < 0 ? 0 : col;
        }

        public String toString() {
            return String.format("VS(row=%d,lcol=%d,rcol=%d,[id=%d]):%s", getRow(), getCol(), getRightMostCol(), getId(), getText());
        }

        @Override
        public Color getColors() {
            return sourceSegment.getColors();
        }

        @Override
        public void moveTo(int row, int col) {
            throw new IllegalStateException("No implemented yet!");
        }

        @Override
        public int getId() {
            return sourceSegment.getId();
        }

        @Override
        public String getText() {
            int start = 0;
            int src_col = sourceSegment.getCol();
            int offset_col = ScreenLineSegmentsView.this.offsetIntoSource.getCol();
            if (offset_col > src_col) {
                start = offset_col - src_col;
            }
            int src_width = sourceSegment.getWidth();
            int col = getCol();
            if (col + (src_width - start) > maxsize.getWidth()) {
                int endOffset = maxsize.getWidth() - getCol();
                return sourceSegment.getText().substring(start, endOffset);
            }
            return sourceSegment.getText().substring(start);
        }

        @Override
        public ScreenLineSegment fromOffset(Position offset) {
            return new SegmentImpl(getColor(), Position.Simple.withOffset((Position) this, offset), getId(), this.getText());
        }

    }

    static class WindowViewComponent extends BoundedComponent<WindowViewComponent> {
        
        Component<?> viewSource;
        ScreenLineSegmentsView segmentsViewBuffer;
        
        public WindowViewComponent(Color color, Dimension d, Component<?> viewSource) {
            super(color, d);
            segmentsViewBuffer = new  ScreenLineSegmentsView(viewSource.getSegmentsBuffer(), 
                                                                                    WindowViewComponent.this.getRectangle());
        }


        @Override
        protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
             segments.merge(segmentsViewBuffer);
            
        }

        public void setViewOffset(int r, int c) {
            segmentsViewBuffer.setOffsetIntoSource(new Position.Simple(r,c));
        }

    }
    
    static class ScrollableComponent extends Composite {
        BorderedComponent borderedComponent;
        WindowViewComponent viewer;
        Component componentBeingViewed;
        
        
        public ScrollableComponent(Color color, Dimension d, Component<?> componentBeingViewed) {
            this.componentBeingViewed = componentBeingViewed;
            viewer = new WindowViewComponent(color, new Dimension.Simple(d.getHeight()-2,d.getWidth()-2), this.componentBeingViewed);
            borderedComponent = new BorderedComponent(color, viewer);
            this.add(borderedComponent);
        }
        
        public void setViewOffset(int r, int c) {
            viewer.setViewOffset(r,c);
        }
        
    }
    
    
    public static boolean testWindowVIewer() {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        TerminalScreen screen = new TerminalScreen();
        screen.setControlCodesEnabled(false);

        Canvas canvas = new Canvas(screen);
        canvas.setSize(12, 80);

        // canvas.t("TEST");

        TextComponent srcOfText = new TextComponent(
                        "T'was Brillig and the slithy toth\n"+
                        "Dide gere and gimble in the wabe\n"+
                        "And hast thou slain the JaberWock\n"+
                        "Oh Frabjious day \n"+
                        "Oh Frabjious joy!\n"+
                        "Calloo callay\n"+
                        "Basterdized Lewis Carroll");
        
        
        Dimension sizeOfWindow = new Dimension.Simple(10,35);
        Position offsetOnCanvas = new Position.Simple(1,1);
        Position viewOffset = new Position.Simple(0,0);
        
        ScrollableComponent viewComp1 = new ScrollableComponent(Colors.Gre, sizeOfWindow, srcOfText);
        
        WindowViewComponent viewComp2 = new WindowViewComponent(Colors.Gre, sizeOfWindow, srcOfText);
        
        viewComp1.setViewOffset(viewOffset.getRow(),viewOffset.getCol());
        
        viewComp2.setViewOffset(viewOffset.getRow(),viewOffset.getCol());

        canvas.addComponentSegments(offsetOnCanvas, viewComp1);
        canvas.addComponentSegments(offsetOnCanvas.offsetAdd(0, sizeOfWindow.getWidth()+5), viewComp2);

        canvas.refresh();
        
        return true;

    }
    
    public static void main(String[] args) {
        
        if (testWindowVIewer()) return;

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        TerminalScreen screen = new TerminalScreen();
        screen.setControlCodesEnabled(false);

        Canvas canvas = new Canvas(screen);
        canvas.setSize(12, 30);

        // canvas.t("TEST");

        ScreenLineSegmentsBuffer srcSegments = new ScreenLineSegmentsBuffer();

        
        int i=0;
        
        
        srcSegments.add(new ScreenLineSegment.SegmentImpl(Colors.BBla, i++, 0, -1, "How now brown cow"));
        srcSegments.add(new ScreenLineSegment.SegmentImpl(Colors.BBla, i++, 1, -1, "Hello there"));
        srcSegments.add(new ScreenLineSegment.SegmentImpl(Colors.BBla, i++, 0, -1, "To error is devine providence"));
        srcSegments.add(new ScreenLineSegment.SegmentImpl(Colors.BBla, i++, 4, -1, "Waiter!"));
        srcSegments.add(new ScreenLineSegment.SegmentImpl(Colors.BBla, i++, 3, -1, "There is a fly in my soup"));
        srcSegments.add(new ScreenLineSegment.SegmentImpl(Colors.BBla, i++, 6, -1, "So? .. what is it doing?"));
        srcSegments.add(new ScreenLineSegment.SegmentImpl(Colors.BBla, i++, 3, -1, "... The back stroke?"));

        Rectangle viewBounds = new Rectangle.Simple(1, 2, 5, 10);
        
        IScreenLineSegmentsBuffer theSegments = new ScreenLineSegmentsView(srcSegments, viewBounds);

        // IScreenLineSegmentsBuffer theSegments = new
        // IScreenLineSegmentsBuffer.Delegate(srcSegments);

        Dimension compDIM = new Dimension.Simple(10,25);
        // Dimension compDIM = new Dimension.Simple(viewBounds);
        //Dimension compDIM = new Dimension.Simple(srcSegments);
        
        BoundedComponent comp = new BoundedComponent(null, compDIM) {

            @Override
            protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
                segments.merge(theSegments);

            }

        };

        canvas.addComponentSegments(1, 2, comp);

        canvas.refresh();

    }

}
