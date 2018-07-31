package dtb.terminalIO.components;

import java.util.HashSet;
import java.util.Set;

import dtb.terminalIO.ControlCode.FGBGColors;
import dtb.terminalIO.IScreenLineSegmentsBuffer;
import dtb.terminalIO.components.Component.AbstractComponent;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.geom.Rectangle;

public class Composite extends AbstractComponent<Composite> implements Rectangle {

    private SimpleBorder border;
    //private FGBGColors   borderColors;
    //private FGBGColors   titleColors;
    private boolean      boundsFixed = false;
    private Rectangle    bounds = Rectangle.ZERO;
    
    private Set<BoundedComponent<?>> children = new HashSet<BoundedComponent<?>>();
    
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
        return this;
    }

    // todo:  not sure what this method means -- what do i
    public Composite setPosition(int i, int j) {
        this.bounds.moveTo(i, j);
        fullrecalcDim();
        return this;
    }

    private Rectangle calcBorderSize(Rectangle inner) {
        Rectangle brdr = new Rectangle.Simple(inner.getCol()-1,
                inner.getRow()-1,
                inner.getHeight()+2,
                inner.getWidth()+2);
        return brdr;
    }
    
    private void fullrecalcDim() {
        fullrecalcDim(hasBorder());
    }
    
    private void fullrecalcDim(boolean includeBorder) {
//        if (boundsFixed) {
//            return ;
//        }
        Rectangle calculatedBounds =  Rectangle.UTIL.calcDimensionsWith(children);
        if (includeBorder) {
            Rectangle brdr = calcBorderSize(calculatedBounds);
            if (Rectangle.DEFAULT_COMPARATOR.compare(brdr, border)!=0) {
                bounds = brdr;
                rebuildBorder(bounds);
            }
            return ;
        }
        bounds = calculatedBounds;
    }
    
    protected void moveChildrenDelta(Position delta) {
        moveChildrenDelta(delta.getRow(), delta.getCol());
    }
    protected void moveChildrenDelta(int rowOffset, int colOffset) {
        for (BoundedComponent<?> child : children) {
            child.moveTo(rowOffset, colOffset);
        }
    }
    
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
        if (hasBorder  && border!=null) return this; // no change
        if (!hasBorder && border==null) return this; // no change
        // change taking place
        if (hasBorder) {
            this.moveChildrenDelta( 1, 1);
        } else {
            this.moveChildrenDelta(-1,-1);
        }
            
        if (hasBorder  && boundsFixed) {
            rebuildBorder(bounds);
        } else {
            fullrecalcDim(false);
            if (hasBorder) {
                this.bounds = calcBorderSize(this.bounds);
                rebuildBorder(this.bounds);
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
        if (hasBorder()) component.moveDelta(1, 1);
        children.add(component);
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
        moveChildrenDelta(delta);
        fullrecalcDim();
        //throw new IllegalStateException("Not sure if I will implement -  probably");
        
    }

    protected void drawBorderIfNeeded(IScreenLineSegmentsBuffer segments) {
        if (hasBorder()) {
            segments.merge(this.border.getSegmentsBuffer());
        }
    }
    
    
    @Override
    protected void draw(FGBGColors color, IScreenLineSegmentsBuffer segments) {
        // loop thru children and draw
        
        for (BoundedComponent<?> child : children) {
            segments.merge(child.getSegmentsBuffer());
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
