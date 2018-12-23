package dtb.terminalIO.components;

import dtb.terminalIO.Canvas;
import dtb.terminalIO.Canvas.CanvasBackend;
import dtb.terminalIO.Canvas.NullCanvasBackEnd;
import dtb.terminalIO.ControlCode.Color;
import dtb.terminalIO.IScreenLineSegmentsBuffer;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;

/**
 * A component that wraps an internal canvas.
 */
public class CanvasComponent implements Component<CanvasComponent>, CanResize {

    Canvas canvas;
    Position pos;
    
    CanvasBackend backend;
    
    public CanvasComponent() {
        this.backend = new NullCanvasBackEnd();
        this.canvas = new Canvas(this.backend);
        this.pos = Position.ZERO;
    }
    
    public CanvasComponent(Dimension d) {
        this();
        this.resize(d);
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
        return this;
    }
    
    public CanvasComponent addTextHandleColors(String s) {
        this.canvas.addTextHandleColors(s);
        return this;
    }    

    public CanvasComponent t(String s) {
        this.canvas.t(s);
        return this;
    }
    
    public void addComponentSegments(int rowOffset, int colOffset, Component<?> component) {
        this.canvas.addComponentSegments(rowOffset, colOffset, component);
    }

    public void addComponentSegments(Position offset, Component<?> component) {
        this.canvas.addComponentSegments(offset, component);
    }

    public void addComponentSegments(Component<?> component) {
        this.canvas.addComponentSegments(component);
    }
    
    public int segmentsSize() {
        return getSegmentsBuffer().getSegments().size();
    }
    
    
    
    @Override
    public IScreenLineSegmentsBuffer getSegmentsBuffer() {
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
        return this;
    }

    @Override
    public void clear() {
        this.canvas.clearCanvas();
    }

    @Override
    public void markAsDirty() {
        // do nothing for now - this might have to change
    }

}
