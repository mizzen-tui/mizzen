package dtb.terminalIO.components;

import dtb.terminalIO.ControlCode.FGBGColors;
import dtb.terminalIO.IScreenLineSegmentsBuffer;
import dtb.terminalIO.components.Component.AbstractComponent;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.geom.Rectangle;

// todo dropshadow component

public class BorderedComponent extends AbstractComponent<BorderedComponent> implements Rectangle {

    private SimpleBorder        border;
    private BoundedComponent<?> component;

    public BorderedComponent(FGBGColors color, BoundedComponent<?> component) {
        super(color);
        this.component = component;
        component.moveDelta(1,1);
        Position.Simple p = new Position.Simple(component);
        p.moveDelta(-1, -1);
        this.border = new SimpleBorder(color, p, new Dimension.Simple(component.getHeight() + 2, component.getWidth() + 2));
        //this.border.setColor(new FGBGColors(Colors.Bla, Colors.On_IWhi));
    }

    public BorderedComponent(BoundedComponent<?> component) {
        this(component.getColor(), component);
    }

    public void setEmptyChar(char emptyChar) {
        super.setEmptyChar(emptyChar);
        border.setEmptyChar(emptyChar);
    }
    
    @Override
    public int getCol() {
        return this.border.getCol();
    }

    @Override
    public int getRow() {
        return this.border.getRow();
    }

    @Override
    public int getWidth() {
        return this.border.getWidth();
    }

    @Override
    public int getHeight() {
        return this.border.getHeight();
    }

    @Override
    public void moveTo(int row, int col) {
        this.border.moveTo(row, col);
        this.component.moveTo(row + 1, col + 1);
    }

    @Override
    public void moveDelta(int rowOffset, int colOffset) {
        this.border.moveDelta(rowOffset, colOffset);
        this.component.moveDelta(rowOffset, colOffset);
    }

    @Override
    protected void draw(FGBGColors color, IScreenLineSegmentsBuffer segments) {
        segments.merge(this.component.getSegmentsBuffer());
        segments.merge(this.border.getSegmentsBuffer());
    }

    public String getTitle() {
        return border.getTitle();
    }

    public BorderedComponent setTitle(FGBGColors titleColor, String title) {
        border.setTitle(titleColor, title);
        return this;
    }

    public BorderedComponent setTitle(String title) {
        border.setTitle(title);
        return this;
    }

    @Override
    protected Rectangle getRectangleForClearAndFill() {
        return this;
    }

}
