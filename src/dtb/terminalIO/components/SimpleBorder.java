package dtb.terminalIO.components;

import dtb.terminalIO.ControlCode.FGBGColors;
import dtb.terminalIO.IScreenLineSegmentsBuffer;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.geom.Rectangle;

public class SimpleBorder extends BoundedComponent<SimpleBorder>{

    private RectangleComponent border;
    private String title = null;
    private FGBGColors titleColor;

    public SimpleBorder(FGBGColors color, Rectangle rect) {
        super(color, rect);
        this.border = new RectangleComponent(color, rect);
        setFillOnClear(false);
    }

    public SimpleBorder(FGBGColors color, Position position, Dimension d) {
        super(color, position, d);
        this.border = new RectangleComponent(color, position ,d);
        setFillOnClear(false);
    }

    public SimpleBorder(FGBGColors color, Dimension d) {
        super(color, d);
        this.border = new RectangleComponent(color, Position.ZERO ,d);
        setFillOnClear(false);
    }

    public SimpleBorder setFillOnClear(boolean fillOnClear) {
        super.setFillOnClear(fillOnClear);
        this.border.setFillOnClear(fillOnClear);
        return this;
    }
    
   
    
    @Override
    protected void draw(FGBGColors color, IScreenLineSegmentsBuffer segments) {
        segments.merge(this.border.getSegmentsBuffer());
        if (this.getTitle() != null) {
                segments.merge(makeChildComponent(()->new TextComponent(titleColor, getRow(), getCol() + 1, getTitle())).getSegmentsBuffer());
        }
    }

    public String getTitle() {
        return title;
    }
    
    public FGBGColors getTitleColor() {
        FGBGColors tColor = titleColor != null ? titleColor : getColor();
        return tColor;
    }

    public SimpleBorder setTitle(FGBGColors titleColor, String title) {
        titleColor = titleColor != null ? titleColor : getColor();
        titleColor = titleColor.mergeWith(getColor());
        this.titleColor = titleColor;
        if (title != null) {
                title = title.trim();
//                if (title.length() > getWidth() - 2) {
//                        int rightIDX=
//                        title = title.substring(0, getWidth() - 2);
//                }
        }
        this.title = title;
        return this;
    }
    
    public SimpleBorder setTitle(String title) {
        return setTitle(null, title);
}
    

}
