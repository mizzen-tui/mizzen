package dtb.terminalIO.components;

import java.util.Collection;
import java.util.Collections;

import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;

/**
 * A component that draws a border with an optional title
 * 
 * <p/>
 * 
 * Used by other components to draw borders around themselves
 *
 */
public class SimpleBorder extends BoundedComponent<SimpleBorder> implements HasTitleAndBorder<SimpleBorder> , ParentContainer {

    private RectangleComponent border;
    private String title = null;
    private Color titleColor;
    private boolean hilited = false;

    public SimpleBorder(Color color, Rectangle rect) {
        super(color, rect);
        this.border = new RectangleComponent(color, rect);
        this.border.setParentContainer(this);
        setFillOnClear(false);
    }

    public SimpleBorder(Color color, Position position, Dimension d) {
        super(color, position, d);
        this.border = new RectangleComponent(color, position ,d);
        setFillOnClear(false);
    }

    public SimpleBorder(Color color, Dimension d) {
        super(color, d);
        this.border = new RectangleComponent(color, Position.ZERO ,d);
        setFillOnClear(false);
    }

    public SimpleBorder setFillOnClear(boolean fillOnClear) {
        super.setFillOnClear(fillOnClear);
        this.border.setFillOnClear(fillOnClear);
        return this;
    }
    
    public void resize(Dimension d) {
	super.resize(d);
	this.border.resize(d);
    }
   
    
    @Override
    public Position getPositionRelativeToParent(Component<?> child) {
        if (child == this.border) {
            return Position.ZERO;
        }
        return null;
    }
    
    
    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        segments.merge(this.border.getSegmentsBuffer());
        if (this.getTitle() != null) {
                segments.merge(makeChildComponent(()->new TextComponent(getTitleColor(), getRow(), getCol() + 1, getTitle())).getSegmentsBuffer());
        }
    }

    public String getTitle() {
        return title;
    }
    
    public Color getTitleColor() {
        Color tColor = titleColor != null ? titleColor : getColor();
        if (hilited ) {
            tColor = tColor.bold();
        }
        return tColor;
    }

    public SimpleBorder setTitle(Color titleColor, String title) {
        titleColor = titleColor != null ? titleColor : getColor();
        titleColor = titleColor.mergeWith(getColor());
        this.titleColor = titleColor;
//        if (title != null) {  <-- breaks color codes in title - because it appears trim remove control-codes as well as whitespace!!!!!
//                title = title.trim();
////                if (title.length() > getWidth() - 2) {
////                        int rightIDX=
////                        title = title.substring(0, getWidth() - 2);
////                }
//        }
        this.title = title;
        return this;
    }
    
    public SimpleBorder setTitle(String title) {
        return setTitle(null, title);
}

    @Override
    public SimpleBorder setBorder(boolean hasBorder) {
        if (!hasBorder) 
            throw new IllegalStateException("Cannnot turn off border");
        return this;
    }

    @Override
    public SimpleBorder setBorderColor(Color borderColors) {
        border.setColor(borderColors);
        return this;
    }
    
    public Color getBorderColor() {
        return border.getColor();
    }

    @Override
    public void hilite() {
        if (!this.hilited) {
            markAsDirty();
        }
        this.border.hilite();
        this.hilited = true;
    }

    @Override
    public void unHilite() {
        if (this.hilited) {
            markAsDirty();
        }
        this.border.unHilite();
        this.hilited = false;
    }

    @Override
    public Collection<? extends Component<?>> getChildren() {
        return Collections.singleton(this.border);
    }

}
