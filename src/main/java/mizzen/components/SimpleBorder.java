package mizzen.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import mizzen.controlcodes.color.Color;
import mizzen.geom.Align;
import mizzen.geom.Dimension;
import mizzen.geom.Rectangle;
import mizzen.geom.VAlign;
import mizzen.geom.position.Position;
import mizzen.geom.position.PositionRelativeFactory;
import mizzen.textsegments.IScreenLineSegmentsBuffer;
import mizzen.textsegments.ScreenLineSegment;
import mizzen.textsegments.ScreenLineSegment.SegmentImpl;

/**
 * A component that draws a border with an optional title. Used by other components to draw borders around themselves.
 * <p/>
 * The top line has three places: the title at the left, one in the middle and one at the right. All three can show
 * at once, each text or a component (a button, a few radio buttons):
 * <pre>
 *     border.setTitle(" Visitor stats ");
 *     border.setTitleRight(flipButtons);
 *
 *     ╭ Visitor stats ───────────── (•) Below  ( ) Right ╮
 * </pre>
 * Short of room: the right one keeps its width and the middle one goes first; the title is cut to leave at least one
 * ─ before the next one. A component that doesn't fit whole isn't drawn, takes no clicks and is no TAB stop; text
 * is cut. A component there that can take focus is a TAB stop, before what is inside the border.
 */
public class SimpleBorder extends BoundedComponent<SimpleBorder>
                          implements HasTitleAndBorder<SimpleBorder>, 
                                     ParentContainer,
                                     GetComponentAtOffset {

    private RectangleComponent border;
    private boolean hilited = false;
    
    private Color        titleColor;                      // the text title's own colour; null: the border's
    private boolean      titleLineLaidOut = false;         // drawnAt below is for the current size and parts

    /** a place on the top line: the left (setTitle), the middle or the right one */
    private final class TitlePart {
        final Align  align;
        String       text;                                 // a text title, cut to the room it gets; null: a component
        Component<?> component;                            // given, or made from text
        Position     position;                             // where it goes, following the border's width
        Position     drawnAt;                              // laid out to be drawn here; null: not drawn
        int          drawnWidth;                           // its width as drawn: less when the title is cut

        TitlePart(Align align) {
            this.align = align;
        }

        boolean isEmpty() {
            return text == null && component == null;
        }
    }

    private final TitlePart left   = new TitlePart(Align.LEFT);
    private final TitlePart middle = new TitlePart(Align.CENTER);
    private final TitlePart right  = new TitlePart(Align.RIGHT);
    private final TitlePart[] titleParts = { left, middle, right };

    private Component<?> footerComponent;
    private Position     footerPosition;
    private Align        footerAlign;

    public SimpleBorder(Color color, Rectangle rect) {
        super(color, rect);
        this.border = new RectangleComponent(color, rect);
        this.border.setParentContainer(this);
        this.border.setThemeColor((theme) -> getColor());          // the line, given no colour: the border's
        setFillOnClear(false);
    }

    public SimpleBorder(Color color, Position position, Dimension d) {
        super(color, position, d);
        this.border = new RectangleComponent(color, position, d);
        this.border.setParentContainer(this);
        this.border.setThemeColor((theme) -> getColor());          // the line, given no colour: the border's
        setFillOnClear(false);
    }

    public SimpleBorder(Color color, Dimension d) {
        super(color, d);
        this.border = new RectangleComponent(color, Position.ZERO, d);
        this.border.setParentContainer(this);
        this.border.setThemeColor((theme) -> getColor());          // the line, given no colour: the border's
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
        titleLineLaidOut = false;
    }

    @Override
    public Position getPositionRelativeToParent(Component<?> child) {
        if (child == this.border) {
            return Position.ZERO;
        }
        for (TitlePart part : titleParts) {
            if (part.component != null && child == part.component) {
                layOutTitleLine();
                return new Position.Simple(part.drawnAt != null ? part.drawnAt : part.position);   // a simple pos from a computed pos
            }
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
        titleLineLaidOut = false;                                    // a part's width may have changed
        this.border.markAsDirty();
        for (TitlePart part : titleParts) {
            if (part.component != null) {
                part.component.markAsDirty();
            }
        }
    }


    @Override
    public void childResized(Component<?> child) {
        this.markAsDirty();
        titleLineLaidOut = false;
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
        titleLineLaidOut = false;                                    // drawing: work it out afresh
        layOutTitleLine();
        for (TitlePart part : titleParts) {
            if (part.drawnAt != null) {
                mergeCut(segments, part.component, part.drawnAt, part.drawnWidth);
            }
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

    /**
     * Works out where each part of the top line goes at the current width (drawnAt, drawnWidth; drawnAt null: not
     * drawn). The right one keeps its width, or isn't drawn when it doesn't fit between the corners. The middle one
     * is drawn whole, centred, with at least one ─ before the right one, or not at all. The title starts one in from
     * the left corner and is cut to end at least one ─ before the next part drawn - text cut short, a component's right
     * end cut off. Made from text, a part is a TextComponent of the cut text, made again when the cut changes.
     * <p/>
     * Done once per size and set of parts (titleLineLaidOut): draw, clicks and the focus tree all ask for it.
     */
    private void layOutTitleLine() {
        if (titleLineLaidOut) {
            return;
        }
        titleLineLaidOut = true;
        for (TitlePart part : titleParts) {
            part.drawnAt = null;
        }
        boolean shown = !(this.getHeight() == 1 && footerComponent != null) && this.getHeight() > 0;   // one row: the footer's
        int inner = this.getWidth() - 2;                                                             // between the corners
        if (!shown || inner <= 0) {
            return;
        }
        int nextStart = this.getWidth();                       // where the part right of the one being laid out starts
        if (placeWhole(right, inner, this.getWidth())) {
            nextStart = right.drawnAt.getCol();
        }
        if (placeWhole(middle, inner, nextStart - 1)) {        // ends at least one ─ before the right one
            nextStart = middle.drawnAt.getCol();
        }
        int room = nextStart - 2;                              // from column 1 to one ─ before the next
        if (!left.isEmpty() && room > 0) {
            makeTextComponent(left, room);
            left.drawnAt = new Position.Simple(left.position);
            left.drawnWidth = Math.min(left.component.getWidth(), room);
        }
    }

    // the middle or right part, whole: drawn if it lies between the corners and ends before column endBefore
    private boolean placeWhole(TitlePart part, int inner, int endBefore) {
        if (part.isEmpty()) {
            return false;
        }
        makeTextComponent(part, inner);
        int col = part.position.getCol();
        int width = part.component.getWidth();
        if (width <= 0 || col < 1 || col + width > Math.min(this.getWidth() - 1, endBefore)) {
            return false;
        }
        part.drawnAt = new Position.Simple(part.position);
        part.drawnWidth = width;
        return true;
    }

    // a part given as text: its TextComponent, of the text cut to room (made again when the cut changes)
    private void makeTextComponent(TitlePart part, int room) {
        if (part.text == null) {
            return;
        }
        String shown = part.text.length() <= room ? part.text : part.text.substring(0, room);
        if (part.component == null || !shown.equals(((TextComponent) part.component).getText())) {
            part.component = makeChildComponent(() -> new TextComponent((Color) null, shown)
                                                          .setThemeColor((theme) -> getTitleColor()));   // follows: no colour baked in
            if (hilited && part == left) {
                new HiliterAdapter<Component<?>>(part.component).getHiliter().hilite();
            }
            part.position = positionFor(part);
        }
    }

    // follows the border's width: the title one in from the left corner, the right part ending one in from the right
    private Position positionFor(TitlePart part) {
        int fromTheSide = Align.LEFT.equals(part.align) ? 1 : 0;
        return PositionRelativeFactory.fromAlign(VAlign.TOP, 0, part.align, fromTheSide, this, part.component);
    }

    // component's segments at at, with columns from width on cut off (a cut title)
    private static void mergeCut(IScreenLineSegmentsBuffer segments, Component<?> component, Position at, int width) {
        TreeSet<ScreenLineSegment> cut = ScreenLineSegment.UTIL.makeCollection(null);
        for (ScreenLineSegment segment : component.getSegmentsBuffer().getSegments()) {
            String text = segment.getText();
            int keep = Math.min(text.length(), width - segment.getCol());
            if (keep <= 0) {
                continue;
            }
            cut.add(new SegmentImpl(segment.getColors(), new Position.Simple(at.getRow() + segment.getRow(), at.getCol() + segment.getCol()),
                                    text.substring(0, keep)));
        }
        segments.merge(cut);
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

    /** the title given as text, wherever it is (setTitle with an Align puts it there); null: none */
    public String getTitle() {
        for (TitlePart part : titleParts) {
            if (part.text != null) {
                return part.text;
            }
        }
        return null;
    }

    /** the title's colour as drawn: its own (on the border's background, if it has none), else the border's */
    public Color getTitleColor() {
        Color tColor = titleColor != null ? titleColor.mergeWith(getColor()) : getColor();
        if (hilited) {
            tColor = tColor.bold();
        }
        return tColor;
    }

    /**
     * The title as text, in the place titleAlign names (LEFT: the title; CENTER, RIGHT: as setTitleMiddle, setTitleRight).
     * There is one text title: given again, it moves. titleColor: its own colour; null: the border's - worked out as it is
     * drawn (getTitleColor), so it follows a theme. A null title removes it.
     */
    public SimpleBorder setTitle(Color titleColor, String title, Align titleAlign) {
        this.titleColor = titleColor;
        for (TitlePart part : titleParts) {
            if (part.text != null) {
                clear(part);
            }
        }
        TitlePart part = partFor(titleAlign);
        clear(part);
        part.text = title;
        this.markAsDirty();
        return this;
    }

    /** component in the place titleAlign names on the top line (LEFT: the title); null empties it */
    public SimpleBorder setTitle(Component<?> titleComponent, Align titleAlign) {
        TitlePart part = partFor(titleAlign);
        clear(part);
        if (titleComponent != null) {
            titleComponent.setColor( titleComponent.getColor().mergeWith(getColor()) );
            part.component = titleComponent;
            part.position = positionFor(part);
            titleComponent.setParentContainer(this);
        }
        this.markAsDirty();
        return this;
    }

    /** a component centred on the top line, between the title and the right one, e.g. a status; null removes it */
    public SimpleBorder setTitleMiddle(Component<?> component) {
        return setTitle(component, Align.CENTER);
    }

    /** a component at the right of the top line, e.g. a control; null removes it. It keeps its width: the title is cut */
    public SimpleBorder setTitleRight(Component<?> component) {
        return setTitle(component, Align.RIGHT);
    }

    /** the component in a place on the top line (a title given as text: the TextComponent drawn); null: none */
    public Component<?> getTitleComponent(Align align) {
        return partFor(align).component;
    }

    /** the parts of the top line drawn at this size, left to right: the components there to click or TAB to */
    public List<Component<?>> getTitleLineParts() {
        layOutTitleLine();
        List<Component<?>> parts = new ArrayList<Component<?>>(3);
        for (TitlePart part : titleParts) {
            if (part.drawnAt != null) {
                parts.add(part.component);
            }
        }
        return parts;
    }

    /**
     * Takes over another border's top line: its parts (the same components) and the title's colour - for a border made
     * again as its owner's size changes (Composite).
     */
    public SimpleBorder takeTitleLineFrom(SimpleBorder old) {
        this.titleColor = old.titleColor;
        for (int i = 0; i < titleParts.length; i++) {
            TitlePart from = old.titleParts[i];
            TitlePart to = titleParts[i];
            clear(to);
            to.text = from.text;
            if (from.text == null && from.component != null) {
                to.component = from.component;
                to.position = positionFor(to);
                to.component.setParentContainer(null);         // it leaves the old border: one parent at a time
                to.component.setParentContainer(this);
            }
        }
        markAsDirty();
        return this;
    }

    private TitlePart partFor(Align align) {
        return Align.RIGHT.equals(align) ? right : Align.CENTER.equals(align) ? middle : left;
    }

    private void clear(TitlePart part) {
        if (part.component != null && part.text == null && part.component.getParentContainer() == this) {
            part.component.setParentContainer(null);
        }
        part.text = null;
        part.component = null;
        part.position = null;
        part.drawnAt = null;
        titleLineLaidOut = false;
    }

    public SimpleBorder setTitle(Color titleColor, String title) {
        setTitle(titleColor != null ? titleColor : this.titleColor, title, textTitleAlign());
        return this;
    }

    // where the text title is (LEFT if there is none): setting it again keeps it there
    private Align textTitleAlign() {
        for (TitlePart part : titleParts) {
            if (part.text != null) {
                return part.align;
            }
        }
        return Align.LEFT;
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
        setFooter( new TextComponent((Color) null, footerText).setThemeColor((theme) -> getTitleColor()), titleAlign);   // as the title
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

    /** the colours given to it (none: they follow) - for a border made again, so it keeps what was given, not what was resolved */
    public Color getOwnTitleColor() {
        return titleColor;
    }

    public Color getOwnBorderColor() {
        return border.getOwnColor();
    }

    @Override
    public void hilite() {
        if (!this.hilited) {
            markAsDirty();
        }
        this.border.hilite();
        if (left.component != null) {                                // the title; the other parts keep their looks
            new HiliterAdapter<Component<?>>(left.component).getHiliter().hilite();
        }
        this.hilited = true;
    }

    @Override
    public void unHilite() {
        if (this.hilited) {
            markAsDirty();
        }
        this.border.unHilite();
        if (left.component != null) {
            new HiliterAdapter<Component<?>>(left.component).getHiliter().unHilite();
        }
        this.hilited = false;
    }

    @Override
    /**
     * The line, the top line's parts drawn at this size (left to right) and the footer. A part not drawn is left out:
     * it is no TAB stop (the focus tree is worked out from the children).
     */
    public Collection<? extends Component<?>> getChildren() {
        List<Component<?>> parts = getTitleLineParts();
        if (parts.isEmpty() && footerComponent == null) {
           return Collections.singleton(this.border);
        }
        List<Component<?>> children = new ArrayList<Component<?>>(5);
        children.add(this.border);
        children.addAll(parts);
        if (footerComponent != null) {
            children.add(this.footerComponent);
        }
        return children;
    }

    /**
     * the component given for the top line (or the one inside it) at offset, as drawn; null: none there. A title given
     * as text is not one: a click on it is a click on the border's owner, as before.
     */
    public Component<?> getTitleLinePartAt(Position offset) {
        return titleLinePartAt(offset, false);
    }

    private Component<?> titleLinePartAt(Position offset, boolean withText) {
        layOutTitleLine();
        for (TitlePart part : titleParts) {
            if (part.drawnAt == null || (part.text != null && !withText)) {
                continue;
            }
            Position inside = offset.offsetAdd(part.drawnAt.negate());
            if (inside.getCol() >= 0 && inside.getCol() < part.drawnWidth && part.component.containsPosition(inside)) {
                return GetComponentAtOffset.UTIL.getComponentAtOffset(part.component, inside);
            }
        }
        return null;
    }

    @Override
    public Component<?> getComponentAtOffset(Position offset) {
        if (!this.containsPosition(offset)) {
            return null;
        }
        Component<?> onTheTopLine = titleLinePartAt(offset, true);   // the text title too, as before
        if (onTheTopLine != null) {
            return onTheTopLine;
        }
        if (left.isEmpty() && middle.isEmpty() && right.isEmpty() && footerComponent == null) {
            return this;
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
