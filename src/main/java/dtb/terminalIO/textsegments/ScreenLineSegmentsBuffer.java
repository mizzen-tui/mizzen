package dtb.terminalIO.textsegments;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;

/**
 * This is the primary (and default) implementation of IScreenLineSegmentsBuffer.
 */
public class ScreenLineSegmentsBuffer implements Rectangle, IScreenLineSegmentsBuffer {

    private TreeSet<ScreenLineSegment> segments              = ScreenLineSegment.UTIL.makeCollection(null);

    protected boolean                    recalcNeeded = true;
    protected Rectangle                  preCalculatedRectangle = Rectangle.ZERO;

    @Override
    public boolean isEmpty() {
        return segments == null || segments.isEmpty();
    }

    synchronized protected void fullrecalcDim() {
        preCalculatedRectangle = Rectangle.UTIL.calcDimensionsWith(segments);
        recalcNeeded = false;
    }

    @Override
    public void clear() {
        segments.clear();
        recalcNeeded = true;
    }

    protected void doAdd(TreeSet<ScreenLineSegment> segments, ScreenLineSegment sc) {
        // add to segments buffer (collection)
        ScreenLineSegment.UTIL.addTo(segments, sc);
    }

    public TreeSet<ScreenLineSegment> getSegments() {
        return segments;
    }

    @Override
    public void add(ScreenLineSegment sc) {
        // add to buffer

        doAdd(segments, sc);
        recalcNeeded = true;
    }

    @Override
    public int getCol() {
        return this.getPreCalulatedRectangle().getCol();
    }

    @Override
    public int getRow() {
        return this.getPreCalulatedRectangle().getRow();
    }

    @Override
    public int getHeight() {
        return this.getPreCalulatedRectangle().getHeight();
    }

    @Override
    public int getWidth() {
        return this.getPreCalulatedRectangle().getWidth();
    }

    @Override
    public void merge(IScreenLineSegmentsBuffer other) {
        if (other == this)
            return; // sanity
        for (ScreenLineSegment sc : other) {
            this.doAdd(segments, sc);
        }
        recalcNeeded = true;
    }

    @Override
    public void clearAndMerge(Collection<ScreenLineSegment> segments) {
        if (segments instanceof TreeSet) {

            this.segments = (TreeSet<ScreenLineSegment>) segments;

        } else {
            this.clear();
            this.merge(segments);
        }

    }
    

    @Override
    public void merge(Collection<ScreenLineSegment> other_segments) {
        for (ScreenLineSegment sc : other_segments) {
            this.doAdd(segments, sc);
        }
        recalcNeeded = true;
    }

    @Override
    public void mergeAt(int row, int col, IScreenLineSegmentsBuffer segmentsBuffer) {
        Position position = new Position.Simple(row, col);
        ScreenLineSegment.UTIL.addSegmentsWithOffsetUnBounded( position,
                                                               this.getSegments(), 
                                                               segmentsBuffer.getSegments());
    }
    
    @Override
    public void mergeAtBounded(int row, int col, Rectangle bounds, IScreenLineSegmentsBuffer segmentsBuffer) {
        Position position = new Position.Simple(row, col);  
        ScreenLineSegment.UTIL.addSegmentsWithOffsetBounded( position, bounds,
                                                             this.getSegments(), 
                                                             segmentsBuffer.getSegments());
    }
    
    
    public Iterator<ScreenLineSegment> iterator() {
        return segments.iterator();
    }

    @Override
    public ScreenLineSegment getAtCol(int row, int col) {
        return ScreenLineSegment.UTIL.getAtRowCol(segments, row, col);
    }

    @Override
    public String getTextAt(int row, int col) {
        ScreenLineSegment sc = getAtCol(row, col);
        if (sc != null)
            return sc.getText();
        return null;
    }

    @Override
    public String getCharAt(int row, int col) {
        return ScreenLineSegment.UTIL.getCharAt(segments, row, col);
    }

    @Override
    public void setCharAt(int row, int col, char ch, Color color) {
        ScreenLineSegment.UTIL.setCharAt(segments, row, col, ch, color);
    }
    
    public boolean removeSegment(ScreenLineSegment sc) {
        boolean result = this.segments.remove(sc); 
        recalcNeeded = true;
        return result;
    }

    public boolean removeSegments(Collection<ScreenLineSegment> scs) {
        boolean result = false;
        for (ScreenLineSegment sc : scs) {
            boolean currResult = this.segments.remove(sc);
            result = result || currResult;
        }
        if (result)
            recalcNeeded = true;
        return result;
    }

    @Override
    public boolean removeRow(int row) {
        Collection<ScreenLineSegment> theRow = this.segments.stream().filter(sc -> sc.getRow() == row).collect(Collectors.toList());
        return removeSegments(theRow);
    }

    @Override
    public boolean removeRect(Rectangle r) {
        ScreenLineSegment.UTIL.removeRect(segments, r);
        return true;
    }

    public void removeRectangles(Collection<? extends Rectangle> rectangles) {
        ScreenLineSegment.UTIL.removeRectangles(segments, rectangles);
        recalcNeeded = true;
    }

    @Override
    public void moveTo(int row, int col) {
        moveDelta(row - getRow(), col - getCol());
    }

    @Override
    public void moveDelta(int rowOffset, int colOffset) {
        for (ScreenLineSegment sc : segments) {
            sc.moveDelta(rowOffset, colOffset);
        }
        recalcNeeded = true;
    }
    
    @Override
    public String toString() {
        String simple = this.getClass().getSimpleName();
        if (simple == null || simple.length()==0) {
            simple = this.getClass().getName()+":ScreenLineSegmentsBuffer";
        }
        return simple+" [segments=" + segments + ", preCalulatedRectangle=" + getPreCalulatedRectangle() + "]";
    }

    public Rectangle getPreCalulatedRectangle() {
        if (recalcNeeded) {
            fullrecalcDim();
        }
        return preCalculatedRectangle;
    }

    public void setSegments(TreeSet<ScreenLineSegment> segments) {
        this.segments = segments;
        recalcNeeded = true;
    }

    public static class Layer extends Rectangle.Delegate {
        int                             layerId;

        public static Comparator<Layer> COMPARATOR = new Comparator<Layer>() {
                                                       @Override
                                                       public int compare(Layer l1, Layer l2) {
                                                           return Integer.compare(l1.layerId, l2.layerId);
                                                       }
                                                   };

        public Layer(int layerId, IScreenLineSegmentsBuffer segments) {
            super(segments);
            this.layerId = layerId;
        }

        public int getLayerId() {
            return layerId;
        }

        public void setLayerId(int layerId) {
            this.layerId = layerId;
        }

        public IScreenLineSegmentsBuffer getSegments() {
            return (IScreenLineSegmentsBuffer) this.getRectangle();
        }

    }

    public static class Layered implements Rectangle {
        private Map<Integer, Layer> layers                = new HashMap<Integer, Layer>();

        private Rectangle           preCalulatedRectangle = Rectangle.ZERO;

        @Override
        public int getWidth() {
            return preCalulatedRectangle.getWidth();
        }

        @Override
        public int getHeight() {
            return preCalulatedRectangle.getHeight();
        }

        @Override
        public int getCol() {
            return preCalulatedRectangle.getCol();
        }

        @Override
        public int getRow() {
            return preCalulatedRectangle.getRow();
        }

        public void putLayer(Layer newLayer) {
            layers.put(newLayer.getLayerId(), newLayer);
            preCalulatedRectangle = preCalulatedRectangle.mergeAndExpandDimensionsWith(newLayer);
        }

        public Layer putLayer(int layerId, IScreenLineSegmentsBuffer segmentsBuffer) {
            Layer newLayer = new Layer(layerId, segmentsBuffer);
            putLayer(newLayer);
            return newLayer;
        }

        public int getTopLayerId() {
            return layers.keySet().stream().max(Integer::compareTo).orElse(0);
        }

        public int getBottomLayerId() {
            return layers.keySet().stream().min(Integer::compareTo).orElse(0);
        }

        public int nextLayerId() {
            return getTopLayerId() + 1;
        }

        private void fullrecalcDim() {
            preCalulatedRectangle = Rectangle.UTIL.calcDimensionsWith(layers.values());
        }

        public Layer addNewLayer(IScreenLineSegmentsBuffer segmentsBuffer) {
            return putLayer(nextLayerId(), segmentsBuffer);
        }

        public void removeLayer(int layerId) {
            layers.remove(layerId);
            fullrecalcDim();
        }

        public void removeLayer(Layer layer) {
            removeLayer(layer.getLayerId());
        }

        @Override
        public void moveTo(int row, int col) {
            moveDelta(row - getRow(), col - getCol());
        }

        @Override
        public void moveDelta(int rowOffset, int colOffset) {
            for (Layer layer : layers.values()) {
                layer.moveDelta(rowOffset, colOffset);
            }
            fullrecalcDim();
        }

    }

    @Override
    public void fillRectWithChar(Color color, char fillChar, Rectangle bounds) {
        ScreenLineSegment.UTIL.fillRectWithChar(color, fillChar, bounds, getSegments());
    }

}