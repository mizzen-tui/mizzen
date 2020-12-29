package dtb.terminalIO.components.tab;

import dtb.terminalIO.components.BlockChars;
import dtb.terminalIO.components.BoundedComponent;
import dtb.terminalIO.components.CharComponent;
import dtb.terminalIO.components.Hiliter;
import dtb.terminalIO.components.HorizontalLine;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.VerticalLine;
import dtb.terminalIO.components.BlockChars.BlockHiliter;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.focus.ToFocusable;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.CleanString;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;

/**
 * A TabComponent is part of the TabPanel.
 * 
 * It is one of the Tab 'button's rendered at the top of the TabPanel
 * 
 *     +-----------------+
 *     | Sample Tab Text |
 *     +---------------- +
 * 
 * @author dbalme
 *
 */
public class TabComponent extends BoundedComponent<TabComponent> implements ToFocusable, Hiliter  {

    // implementation note:  Its important that TabComponenet implement ToFocusable and that
    // ToFocusable points to its TabAction - which is a focusable managed by the TabPanel FocusManager
    //  in this way when focus is requested of a TabComponent - it will properly travel up
    //  the FocusManager tree because it will first switch to the TabAction ( which is in the tree)
    
    
    private boolean hilited = false;
    boolean active = false;
    CleanString tabText;
    Color textActiveTabColor;
    TabAction tabAction;
    private BlockHiliter blockHiliter = new BlockChars.BlockHiliter();


    TabComponent(Color color, String tabText, boolean active, TabAction tabAction) {
        super(color, new Dimension.Simple(3, tabText.length() + 4));
        this.active = active;
        this.tabText = new CleanString.Simple(tabText);
        this.textActiveTabColor = color.bold();
        this.tabAction = tabAction;
    }

    @Override
    protected void draw(Color colorcolor, IScreenLineSegmentsBuffer segments) {
        Color tabColor = active ? this.getColor().bold() : this.getColor();

        HorizontalLine topLine = makeChildComponent(
                () -> new HorizontalLine(tabColor, getRow(), getCol() + 1, getWidth() - 2));

        VerticalLine vLeft = makeChildComponent(
                () -> new VerticalLine(tabColor, this.getRow() + 1, this.getCol(), getHeight() - 2));
        VerticalLine vRight = makeChildComponent(
                () -> new VerticalLine(tabColor, this.getRow() + 1, this.getRightMostCol(), getHeight() - 2));

        // vertical!
        segments.merge(topLine.getSegmentsBuffer());
        segments.merge(vLeft.getSegmentsBuffer());
        segments.merge(vRight.getSegmentsBuffer());

        if (!active) {
            HorizontalLine bottomLine = makeChildComponent(
                    () -> new HorizontalLine(tabColor, getBottomMostRow(), getCol() + 1, getWidth() - 2));
            segments.merge(bottomLine.getSegmentsBuffer());
        }

        TextComponent tc = new TextComponent(active ? textActiveTabColor : tabColor, 1, 2, this.tabText);
        segments.merge(tc.getSegmentsBuffer());

        segments.merge(makeChildComponent(
                                            () -> new CharComponent(tabColor, this.getRow(), this.getCol(), this.getBlockChars().getTOP_LEFT()))
                                                    .getSegmentsBuffer());
        segments.merge(makeChildComponent(() -> new CharComponent(tabColor, this.getRow(), this.getRightMostCol(),
                                                    this.getBlockChars().getTOP_RIGHT())).getSegmentsBuffer());

        segments.merge(makeChildComponent(() -> new CharComponent(tabColor, this.getBottomMostRow(), this.getCol(),
                                                                    active ? this.getBlockChars().getBOTTOM_RIGHT() : this.getBlockChars().getHORIZONTAL_UP()))
                                                                            .getSegmentsBuffer());
        segments.merge(
                makeChildComponent(() -> new CharComponent(tabColor, this.getBottomMostRow(), this.getRightMostCol(),
                        active ? this.getBlockChars().getBOTTOM_LEFT() : this.getBlockChars().getHORIZONTAL_UP()))
                                .getSegmentsBuffer());
    }

    public BlockChars getBlockChars() {
        return blockHiliter.getBlockChars();
    }
    
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        if (active) {
            hilite();
        } else {
            unHilite();
        }
        this.markAsDirty();
    }

    public CleanString getTabText() {
        return tabText;
    }

    public void setTabText(String tabText) {
        setTabText(new CleanString.Simple(tabText) );
    }

    public void setTabText(CleanString tabText) {
        Rectangle new_rect = new Rectangle.Simple(new Position.Simple(this.getRow(), this.getCol()),
                                                  new Dimension.Simple(3, tabText.length() + 4));
        this.setRectangle(new_rect);
        this.tabText = tabText;
        this.markAsDirty();
    }

    public Color getTextActiveTabColor() {
        return textActiveTabColor;
    }

    public void setTextActiveTabColor(Color colortextActiveTabColor) {
        this.textActiveTabColor = colortextActiveTabColor;
    }

    @Override
    public Focusable asFocusable() {
        return tabAction;
    }

    @Override
    public void hilite() {
        if ( ! this.hilited) {
            markAsDirty();
        }
        //colorHiliter.hilite();
        blockHiliter.hilite();
        this.hilited = true;
    }

    @Override
    public void unHilite() {
        if (this.hilited) {
            markAsDirty();
        }
        //colorHiliter.unHilite();
        blockHiliter.unHilite();
        this.hilited = false;
    }

}
