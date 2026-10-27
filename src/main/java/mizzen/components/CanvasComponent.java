package mizzen.components;

import mizzen.controlcodes.color.Color;
import mizzen.core.Canvas;
import mizzen.core.CanvasBackend;
import mizzen.events.EventBus;
import mizzen.geom.CanResize;
import mizzen.geom.Dimension;
import mizzen.geom.position.Position;
import mizzen.textsegments.IScreenLineSegmentsBuffer;

/**
 * A component that wraps an internal canvas.
 */
public class CanvasComponent implements Component<CanvasComponent>, CanResize {

    Canvas canvas;
    Position pos;
    boolean hasDrawn = false;

    CanvasBackend backend;
    private ParentContainer parentContainer;
    
    private Position canvasOffset;

    public CanvasComponent() {
        this.backend = new CanvasBackend.NullCanvasBackEnd();
        this.canvas = new Canvas(this.backend);
        this.pos = Position.ZERO;
    }
    
    public CanvasComponent(Dimension d) {
        this();
        this.resize(d);
    }

    @Override
    public void setParentContainer(ParentContainer parentContainer) {
        if (parentContainer != null && this.getParentContainer() != null && this.getParentContainer() != parentContainer) {
            throw new IllegalStateException("Parent container already set to another component");
        }
        this.parentContainer = parentContainer;

        Component.UTIL.markParentDirtyIfChildDirty(this);
        
    }
    
    public ParentContainer getParentContainer() {
        return this.parentContainer;
    }
    
    
    @Override
    public Position getCanvasOffset() {
        return this.canvasOffset;
    }
    
    @Override
    public void setCanvasOffset(Position canvasOffset) {
        this.canvasOffset = canvasOffset;
    }
    
    
    public void setSize(int height, int width) {
        this.resize( new Dimension.Simple(height,width)  );
    }
    
    public void resize(Dimension d) {
        this.canvas.setSize(d.getHeight(), d.getWidth());
        markAsDirty();
    }
    
    public Canvas moveToCursor(Position pos) {
        return this.canvas.moveToCursor(pos);
    }

    public Canvas moveToCursor(int row, int col) {
        return this.canvas.moveToCursor(row, col);
    }
    
    public String getTextAt(int row, int col) {
        return this.canvas.getTextAt(row, col);
    }

    public String getCharAt(int row, int col) {
        return this.canvas.getCharAt(row, col);
    }

    public CanvasComponent t(int row, int col, String s) {
        this.canvas.t(row, col,s);
        this.markAsDirty();
        return this;
    }

    public CanvasComponent addTextHandleColors(String s) {
        this.canvas.addTextHandleColors(s);
        this.markAsDirty();
        return this;
    }    

    public CanvasComponent t(String s) {
        this.canvas.t(s);
        this.markAsDirty();
        return this;
    }
    
    public void addComponentSegments(int rowOffset, int colOffset, Component<?> component) {
        this.canvas.addComponentSegments(rowOffset, colOffset, component);
    }

    public void addComponentSegments(Position offset, Component<?> component) {
        this.canvas.addComponentSegments(offset, component);
        this.markAsDirty();
    }

    public void addComponentSegments(Component<?> component) {
        this.canvas.addComponentSegments(component);
        this.markAsDirty();
    }
    
    public int segmentsSize() {
        return getSegmentsBuffer().getSegments().size();
    }
    
    
    @Override
    public IScreenLineSegmentsBuffer getSegmentsBuffer() {
        hasDrawn = true;
        return canvas.getSegmentsBuffer();
    }

    @Override
    public Color getColor() {
        return canvas.getTextColor();
    }

    @Override
    public int getCol() {
        return this.pos.getCol();
    }

    @Override
    public int getRow() {
        return this.pos.getRow();
    }

    @Override
    public int getWidth() {
        return this.canvas.getWidth();
    }

    @Override
    public int getHeight() {
        return this.canvas.getHeight();
    }

    @Override
    public void moveTo(int row, int col) {
        this.pos = new Position.Simple(row, col);
        
    }

    @Override
    public CanvasComponent setColor(Color drawingcolor) {
        this.canvas.setTextColor(drawingcolor);
        this.markAsDirty();
        return this;
    }

    @Override
    public void clear() {
        this.canvas.clearCanvas();
    }

    
    
    public boolean isDirty() {
        return !this.hasDrawn;
    }
    
    public void markAsDirty() {
        if (isDirty()) {
            Component.UTIL.markParentDirtyIfChildDirty(this);
            return;
        }
        this.hasDrawn = false;

        // if parent exists, then mark it as dirty as well
        Component.UTIL.markParentDirtyIfChildDirty(this);
    }

    @Override
    public boolean isEnabled() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setEnabled(boolean enabled, EventBus eventBus) {
        // TODO Auto-generated method stub
        
    }
    
//    @Override
//    public void markAsDirty() {
//        // do nothing for now - this might have to change
//    }
//    
//    public boolean isDirty() {
//        return false; // todo: need to fix!!
//    }
    

}
