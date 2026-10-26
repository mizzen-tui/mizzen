package dtb.terminalIO.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.geom.Align;
import dtb.terminalIO.geom.CanResize;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.VAlign;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.geom.position.PositionRelativeFactory;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.textsegments.ScreenLineSegment;
import dtb.terminalIO.textsegments.ScreenLineSegment.SegmentImpl;

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
        this.border.resize(sizeAfterSuperResize());                 // not d: the parent's rules may have resized this again
        if (this.titleComponent != null && this.titleComponent instanceof CanResize) {
            
        }
    }

    @Override
    public Position getPositionRelativeToParent(Component<?> child) {
        if (child == this.border) {
            return Position.ZERO;
        }
        if (this.titleComponent != null && child == this.titleComponent && this.titlePosition != null) {
            return new Position.Simple(this.titlePosition); // return back a simple pos from a computed pos
        }
        if (this.footerComponent != null && child == this.footerComponent && this.footerDrawnAt != null) {
            return new Position.Simple(this.footerDrawnAt);     // where it was drawn (clipped: may start left of the border)
        }
        if (this.footerComponent != null && child == this.footerComponent && this.footerPosition != null) {
            return new Position.Simple(this.footerPosition); // return back a simple pos from a computed pos
        }
        return null;
    }

    @Override
    public void markAsDirty() {
        if (isDirty()) {
            return;
        }
        super.markAsDirty();
        this.border.markAsDirty();
        if (this.titleComponent != null) {
            this.titleComponent.markAsDirty();
        }
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
        boolean oneRow = this.getHeight() == 1;
        if (oneRow) {
            // room for one line only: the bottom one if there is a footer (it carries live things - a
            // meter, controls, a status), else the top one with the title
            String line = footerComponent != null ? "└" + repeat('─', this.getWidth() - 2) + "┘"
                                                  : "┌" + repeat('─', this.getWidth() - 2) + "┐";
            if (this.getWidth() >= 2) {
                segments.getSegments().add(new SegmentImpl(this.border.getColor(), new Position.Simple(0, 0), line));
            }
        } else {
            segments.getSegments().addAll( this.border.getSegmentsBuffer().getSegments() );
        }
        if ((this.getTitle() != null || titleComponent != null) && !(oneRow && footerComponent != null)) {
            if (titleComponent == null) {
                titleComponent = makeChildComponent(() -> new TextComponent(getTitleColor(), getTitle()));
                if (titlePosition == null) {
                    titlePosition = PositionRelativeFactory.fromAlign(this.titleAlign, this, titleComponent, 1);
                }
                if (titleComponent.getWidth() > (this.getWidth()-2)) {
                    ((TextComponent) titleComponent).resize(new Dimension.Simple(titleComponent.getHeight(), this.getWidth()-2));
                }
            }
            // need to do a "smart(slow) merge" here
            segments.mergeAt(titlePosition, titleComponent.getSegmentsBuffer());
        }
        footerDrawnAt = null;
        if (footerComponent != null) {
            if (footerPosition == null) {
                // follows the border's size; LEFT starts one in, so the corner shows (RIGHT already ends one in)
                int fromTheSide = Align.LEFT.equals(this.footerAlign) ? 1 : 0;
                footerPosition = PositionRelativeFactory.fromAlign(VAlign.BOTTOM, 0, this.footerAlign, fromTheSide, this, footerComponent);
            }
            if (footerComponent.getWidth() <= this.getWidth() - 2) {
                footerDrawnAt = new Position.Simple(footerPosition);
                // need to do a "smart(slow) merge" here
                segments.mergeAt(footerDrawnAt, footerComponent.getSegmentsBuffer());
            } else {
                drawFooterClipped(segments);
            }
        }
    }

    private static String repeat(char c, int count) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < count; i++) {
            line.append(c);
        }
        return line.toString();
    }

    /** where the footer was drawn last (its first column may be left of the border when clipped); null: not drawn */
    private Position footerDrawnAt;

    /**
     * A footer wider than the room between the corners: drawn ending one in from the right corner,
     * as a right-aligned one would, and cut at the left corner - its right end (e.g. a search field
     * the user just opened) stays in view.
     */
    private void drawFooterClipped(IScreenLineSegmentsBuffer segments) {
        int row = this.getHeight() - 1;
        int start = (this.getWidth() - 1) - footerComponent.getWidth();
        footerDrawnAt = new Position.Simple(row, start);
        TreeSet<ScreenLineSegment> clipped = ScreenLineSegment.UTIL.makeCollection(null);
        for (ScreenLineSegment segment : footerComponent.getSegmentsBuffer().getSegments()) {
            int col = start + segment.getCol();
            String text = segment.getText();
            if (col < 1) {
                if (1 - col >= text.length()) {
                    continue;                            // entirely left of the corner
                }
                text = text.substring(1 - col);
                col = 1;
            }
            clipped.add(new SegmentImpl(segment.getColors(), new Position.Simple(row + segment.getRow(), col), text));
        }
        segments.merge(clipped);
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

    public SimpleBorder removeFooterComponent() {
        setFooter((Component<?>) null, null);
        return this;
    }

    public SimpleBorder setFooter(Component<?> footerComponent, Align footerAlign) {
        if (this.footerComponent != null) {
            this.footerComponent.setParentContainer(null);
            this.footerAlign = null;
        }
        if (footerComponent == null) {
            this.footerComponent = null;
            this.footerAlign = null;
            this.footerPosition = null;
            this.markAsDirty();
            return this;
        }
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
        if (footerComponent != null && footerDrawnAt != null && offset.getCol() >= 1 && offset.getCol() <= this.getWidth() - 2
                && footerComponent.containsPosition(offset.offsetAdd(footerDrawnAt.negate()))) {
            return GetComponentAtOffset.UTIL.getComponentAtOffset(this.footerComponent, offset.offsetAdd(footerDrawnAt.negate()));
        }
        if (offset.getRow() == 0 || offset.getCol() == 0 || offset.getRow() == this.getBottomMostRow() || offset.getCol() == this.getRightMostCol()) {
            return GetComponentAtOffset.UTIL.getComponentAtOffset(this.border, offset);
        }
        return this;
    }

}
