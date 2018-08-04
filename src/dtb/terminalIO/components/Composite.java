package dtb.terminalIO.components;

import java.util.ArrayList;
import java.util.List;

import dtb.terminalIO.ControlCode.FGBGColors;
import dtb.terminalIO.IScreenLineSegmentsBuffer;
import dtb.terminalIO.ScreenLineSegment;
import dtb.terminalIO.components.Component.AbstractComponent;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.geom.Rectangle;

public class Composite extends AbstractComponent<Composite> implements Rectangle {

    private SimpleBorder border;
    private boolean      boundsFixed = false;
    private Rectangle    bounds = Rectangle.ZERO;
    
    private List<BoundedComponent<?>> children = new ArrayList<BoundedComponent<?>>();
    
    public Composite() {
        super(FGBGColors.NO_COLOR_SPECIFIED);
    }
    
    public Composite setBounds(int row, int col, int height, int width) {
        this.bounds = new Rectangle.Simple(row, col, height, width);
        this.boundsFixed = true;
        return this;
    }

    public Composite setBounds(int height, int width) {
        this.bounds = new Rectangle.Simple(new Dimension.Simple(height, width));
        this.boundsFixed = true;
        rebuildBorder(this.bounds);
        return this;
    }

    // todo:  not sure what this method means -- what do i
    public Composite setPosition(int i, int j) {
        this.bounds.moveTo(i, j);
        fullrecalcDim();
        return this;
    }

    private Rectangle calcBorderSize(Rectangle inner) {
        Rectangle brdr = new Rectangle.Simple(inner.getCol(),
                inner.getRow(),
                inner.getHeight()+2,
                inner.getWidth()+2);
        return brdr;
    }
    
    private void fullrecalcDim() {
        fullrecalcDim(hasBorder());
    }
    
    private void fullrecalcDim(boolean includeBorder) {
        
        if (boundsFixed) {
            return;
        }

        Rectangle calculatedBounds =  Rectangle.UTIL.calcDimensionsWith(children);
        if (includeBorder) {
            rebuildBorder( calcBorderSize(calculatedBounds));
            bounds = new Rectangle.Simple(this.border);
            return ;
        }
        bounds = calculatedBounds;
    }
    
//    protected void moveChildrenDelta(Position delta) {
//        moveChildrenDelta(delta.getRow(), delta.getCol());
//    }
//    protected void moveChildrenDelta(int rowOffset, int colOffset) {
//        for (BoundedComponent<?> child : children) {
//            child.moveDelta(rowOffset, colOffset);
//        }
//    }
    
    protected boolean hasBorder() {
        return border != null;
    }
    
    protected void rebuildBorder(Rectangle borderSize) {
        FGBGColors tColor = border!=null?border.getTitleColor():this.getColor();
        FGBGColors bColor = border!=null?border.getColor():this.getColor();
        String title = border!=null? border.getTitle() : null;
        this.border = new SimpleBorder(bColor, borderSize);
        this.border.setTitle(tColor, title);
    }
    
    public Composite setBorder(boolean hasBorder) {
            
        if (hasBorder  && boundsFixed) {
            rebuildBorder(bounds);
        } else {
            fullrecalcDim(false);
            if (hasBorder) {
                this.bounds = calcBorderSize(this.bounds);
                rebuildBorder(this.bounds);
            } else {
                this.border = null;
            }
        }
        return this;
    }

    public Composite setBorderColor(FGBGColors borderColors) {
        setBorder(true);
        this.border.setColor(borderColors);
        return this;
    }

    public Composite setTitle(FGBGColors titleColors, String title) {
        setBorder(true);
        this.border.setTitle(titleColors, title);
        return this;
    }

    public Composite setTitle(String title) {
        setBorder(true);
        this.border.setTitle(title);
        return this;
    }

    public Composite add(BoundedComponent<?> component) {
        children.add(component);
        fullrecalcDim();
        return this;
    }

    public Composite add(Position offset, BoundedComponent<?> component) {
        component.moveDelta(offset.getRow(), offset.getCol());
        add(component);
        return this;
    }

    public Composite add(int offsetRow, int offsetCol, BoundedComponent<?> component) {
        component.moveDelta(offsetRow, offsetCol);
        add(component);
        return this;
    }

    @Override
    public int getCol() {
        return bounds.getCol();
    }

    @Override
    public int getRow() {
        return bounds.getRow();
    }

    @Override
    public int getWidth() {
        return bounds.getWidth();
    }

    @Override
    public int getHeight() {
        return bounds.getHeight();
    }

    @Override
    public void moveTo(int row, int col) {
        Position delta = this.offsetTo(row, col);
        bounds.moveDelta(delta.getRow(), delta.getCol());
        fullrecalcDim();
        
    }

    public void clear() {
        super.clear();
//        segments.clear();
//        fillPolicy.fillEmptyIfConfigured();
//        this.hasDrawn = false;
        for (BoundedComponent<?> child : children) {
            child.clear();
        }
    }
    
    
    protected void drawBorderIfNeeded(IScreenLineSegmentsBuffer segments) {
        if (hasBorder()) {
            segments.merge(this.border.getSegmentsBuffer());
        }
    }
    
    
    @Override
    protected void draw(FGBGColors color, IScreenLineSegmentsBuffer segments) {
        // loop thru children and draw

        Position offset = Position.ZERO;
        if (this.hasBorder()) {
            offset = offset.offsetTo(1, 1);
        }
        
        for (BoundedComponent<?> child : children) {

            if (boundsFixed) {
                ScreenLineSegment.UTIL.addSegmentsWithOffsetBounded(offset,
                                                                    bounds,
                                                                    segments.getSegments(),
                                                                    child.getSegmentsBuffer().getSegments());
            } else {
                ScreenLineSegment.UTIL.addSegmentsWithOffsetUnBounded(offset, 
                                                                      segments.getSegments(),
                                                                      child.getSegmentsBuffer().getSegments());
            }
            
           // segments.merge(child.getSegmentsBuffer());
        }
        
        drawBorderIfNeeded(segments);
        
    }

    @Override
    protected Rectangle getRectangleForClearAndFill() {
        return bounds;
    }

    public Composite t(int i, int j, String text) {
        this.add(new TextComponent(getColor(),i,j, text));
        return this;
    }

    public Composite t(String text) {
        this.add(new TextComponent(getColor(), text));
        return this;
    }

    
}
