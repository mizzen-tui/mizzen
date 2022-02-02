package dtb.terminalIO.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.geom.Align;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.VAlign;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.geom.position.PositionRelativeFactory;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;

/**
 * A component that draws a border with an optional title
 * 
 * <p/>
 * 
 * Used by other components to draw borders around themselves
 *
 */
public class SimpleBorder extends BoundedComponent<SimpleBorder>
                          implements HasTitleAndBorder<SimpleBorder>, 
                                     ParentContainer,
                                     GetComponentAtOffset {

    private RectangleComponent border;
    private boolean hilited = false;
    
    private Component<?> titleComponent;
    private String       title = null;
    private Color        titleColor;
    private Position     titlePosition;
    private Align        titleAlign;

    private Component<?> footerComponent;
    private Position     footerPosition;
    private Align        footerAlign;

    public SimpleBorder(Color color, Rectangle rect) {
        super(color, rect);
        this.border = new RectangleComponent(color, rect);
        this.border.setParentContainer(this);
        setFillOnClear(false);
    }

    public SimpleBorder(Color color, Position position, Dimension d) {
        super(color, position, d);
        this.border = new RectangleComponent(color, position, d);
        this.border.setParentContainer(this);
        setFillOnClear(false);
    }

    public SimpleBorder(Color color, Dimension d) {
        super(color, d);
        this.border = new RectangleComponent(color, Position.ZERO, d);
        this.border.setParentContainer(this);
        setFillOnClear(false);
    }

    public SimpleBorder setFillOnClear(boolean fillOnClear) {
        super.setFillOnClear(fillOnClear);
        this.border.setFillOnClear(fillOnClear);
        this.border.setParentContainer(this);
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
        if (this.titleComponent != null && child == this.titleComponent && this.titlePosition != null) {
            return new Position.Simple(this.titlePosition); // return back a simple pos from a computed pos
        }
        if (this.footerComponent != null && child == this.footerComponent && this.footerPosition != null) {
            return new Position.Simple(this.footerPosition); // return back a simple pos from a computed pos
        }
        return null;
    }

    @Override
    public void childResized(Component<?> child) {
        this.markAsDirty();
        if (this.getParentContainer() != null) {
            this.getParentContainer().childResized(this);
        }
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        // contract:  segments have already been cleared
        //segments.merge(this.border.getSegmentsBuffer());
        // do a fast add of border
        segments.getSegments().clear();
        segments.getSegments().addAll( this.border.getSegmentsBuffer().getSegments() );
        if (this.getTitle() != null || titleComponent != null) {
            if (titleComponent == null) {
                titleComponent = makeChildComponent(() -> new TextComponent(getTitleColor(), getTitle()));
                if (titlePosition == null) {
                    titlePosition = PositionRelativeFactory.fromAlign(this.titleAlign, this, titleComponent, 1);
                }
            }
            // need to do a "smart(slow) merge" here
            segments.mergeAt(titlePosition, titleComponent.getSegmentsBuffer());
        }
        if (footerComponent != null) {
            if (footerPosition == null) {
                footerPosition = PositionRelativeFactory.fromAlign(VAlign.BOTTOM, this.footerAlign, this, footerComponent);
            }
            // need to do a "smart(slow) merge" here
            segments.mergeAt(footerPosition, footerComponent.getSegmentsBuffer());
        }
    }

    public String getTitle() {
        return title;
    }

    public Color getTitleColor() {
        Color tColor = titleColor != null ? titleColor : getColor();
        if (hilited) {
            tColor = tColor.bold();
        }
        return tColor;
    }

    public SimpleBorder setTitle(Color titleColor, String title, Align titleAlign) {
        titleColor = titleColor != null ? titleColor : getColor();
        titleColor = titleColor.mergeWith(getColor());
        this.titleColor = titleColor;

        this.title = title;
        this.titleAlign = titleAlign;
        this.titlePosition = null;
        this.titleComponent = null;
        this.markAsDirty();
        return this;
    }

    public SimpleBorder setTitle(Component<?> titleComponent, Align titleAlign) {
        titleComponent.setColor( titleComponent.getColor().mergeWith(getColor()) );
        this.titleAlign = titleAlign;
        this.titlePosition = null;
        this.titleComponent = titleComponent;
        this.titleComponent.setParentContainer(this);
        this.markAsDirty();
        return this;
    }
    
    public SimpleBorder setTitle(Color titleColor, String title) {
        // Color tColor = titleColor != null ? titleColor : getColor();
        if (titleColor != null) {
            if ( ! Color.hasBackgroundColor(titleColor)) {
                titleColor = titleColor.mergeWith(getColor());
            }
        } else {
            titleColor = this.titleColor !=null? this.titleColor : getColor();
        }
        setTitle(titleColor, title, titleAlign == null ? Align.LEFT : titleAlign);
        return this;
    }

    public SimpleBorder setTitle(Component<?> titleComponent) {
        return setTitle(titleComponent, Align.LEFT);
    }


    public SimpleBorder setTitle(String title) {
        return setTitle(titleColor, title);
    }

    public boolean hasBorder() {
        // see setBorder() below for reason for hard-coded true value
        return true;
    }

    @Override
    public SimpleBorder setBorder(boolean hasBorder) {
        if (!hasBorder)
            throw new IllegalStateException(HasName.UTIL.getName(this)+".setBorder(): Cannnot turn off border");
        return this;
    }

    public SimpleBorder setFooter(Component<?> footerComponent, Align footerAlign) {
        footerComponent.setColor( footerComponent.getColor().mergeWith(getColor()) );
        this.footerAlign = footerAlign;
        this.footerPosition = null;
        this.footerComponent = footerComponent;
        this.footerComponent.setParentContainer(this);
        this.markAsDirty();
        return this;
    }

    public SimpleBorder setFooter(Component<?> footerComponent) {
        setFooter(footerComponent, Align.RIGHT);
        return this;
    }

    public SimpleBorder setFooter(String footerText, Align titleAlign) {
        Color footerColor = titleColor;
        if (footerColor == null) {
            footerColor = getColor();
        }
        setFooter( new TextComponent(footerColor, footerText), titleAlign);
        return this;
    }

    public SimpleBorder setFooter(String footerText) {
        setFooter( footerText, Align.RIGHT);
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
        if (this.titleComponent != null) {
            new HiliterAdapter<Component<?>>(this.titleComponent).getHiliter().hilite();
        }
        this.hilited = true;
    }

    @Override
    public void unHilite() {
        if (this.hilited) {
            markAsDirty();
        }
        this.border.unHilite();
        if (this.titleComponent != null) {
            new HiliterAdapter<Component<?>>(this.titleComponent).getHiliter().unHilite();
        }
        this.hilited = false;
    }

    @Override
    public Collection<? extends Component<?>> getChildren() {
        if (this.titleComponent == null && footerComponent == null) {
           return Collections.singleton(this.border);
        } else {
            List<Component<?>> children = new ArrayList<Component<?>>(3);
           children.add(this.border);
            if (titleComponent != null) {
                children.add(this.border);
             }
            if (footerComponent != null) {
                children.add(this.footerComponent);
             }
            return children;
        }
    }

    @Override
    public Component<?> getComponentAtOffset(Position offset) {
        if (!this.containsPosition(offset)) {
            return null;
        }
        if (this.titleComponent == null && footerComponent == null) {
            return this;
        }        
        if (titleComponent != null && titleComponent.containsPosition(offset.offsetAdd(titlePosition.negate()))) {
            return GetComponentAtOffset.UTIL.getComponentAtOffset(this.titleComponent, offset.offsetAdd(titlePosition.negate()));
        }
        if (footerComponent != null && footerComponent.containsPosition(offset.offsetAdd(footerPosition.negate()))) {
            return GetComponentAtOffset.UTIL.getComponentAtOffset(this.footerComponent, offset.offsetAdd(footerPosition.negate()));
        }
        if (offset.getRow() == 0 || offset.getCol() == 0 || offset.getRow() == this.getBottomMostRow() || offset.getCol() == this.getRightMostCol()) {
            return GetComponentAtOffset.UTIL.getComponentAtOffset(this.border, offset);
        }
        return this;
    }

}
