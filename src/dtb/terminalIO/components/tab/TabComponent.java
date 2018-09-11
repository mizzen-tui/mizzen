package dtb.terminalIO.components.tab;

import dtb.terminalIO.Canvas;
import dtb.terminalIO.Colors;
import dtb.terminalIO.ControlCode.FGBGColors;
import dtb.terminalIO.IScreenLineSegmentsBuffer;
import dtb.terminalIO.components.BoundedComponent;
import dtb.terminalIO.components.CharComponent;
import dtb.terminalIO.components.HorizontalLine;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.VerticalLine;
import dtb.terminalIO.examples.TestsCommon;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.geom.Rectangle;

public class TabComponent extends BoundedComponent<TabComponent>{

    //  
    //    +-----------------+
    //    | Sample Tab Text |
    //   -+                 +- 
    
    boolean active = false;
    String tabText;
    
    
    public TabComponent(FGBGColors color, String tabText, boolean active) {
	super(color, new Dimension.Simple(3, tabText.length()+4));
	this.active = active;
	this.tabText = tabText;
    }

    public TabComponent(FGBGColors color, Position position, String tabText, boolean active) {
	super(color, position, new Dimension.Simple(3, tabText.length()+4));
	this.active = active;
	this.tabText = tabText;
    }

    @Override
    protected void draw(FGBGColors color, IScreenLineSegmentsBuffer segments) {
        FGBGColors tabColor = active ? this.getColor().bold() : this.getColor();

        HorizontalLine topLine = makeChildComponent(()->new HorizontalLine(tabColor, getRow(), getCol()+1, getWidth() - 2));

        VerticalLine vLeft = makeChildComponent(()->new VerticalLine(tabColor, this.getRow()+1, this.getCol(), getHeight()-2));
        VerticalLine vRight = makeChildComponent(()->new VerticalLine(tabColor, this.getRow()+1, this.getRightMostCol(), getHeight()-2));
        
        
        // verticals!
        segments.merge(topLine.getSegmentsBuffer());
        segments.merge(vLeft.getSegmentsBuffer());
        segments.merge(vRight.getSegmentsBuffer());

        if (!active) {
           HorizontalLine bottomLine = makeChildComponent(()->new HorizontalLine(tabColor, getBottomMostRow(), getCol()+1, getWidth() - 2));
           segments.merge(bottomLine.getSegmentsBuffer());
        }
        
        
        
        TextComponent tc = new TextComponent(tabColor, 1,2 ,this.tabText);
        segments.merge(tc.getSegmentsBuffer());
        
        segments.merge(makeChildComponent(()->new CharComponent(tabColor, this.getRow(), this.getCol(),this.getBlockChars().getTOP_LEFT())).getSegmentsBuffer());
        segments.merge(makeChildComponent(()->new CharComponent(tabColor, this.getRow(), this.getRightMostCol(),this.getBlockChars().getTOP_RIGHT())).getSegmentsBuffer());
        segments.merge(makeChildComponent(()->new CharComponent(tabColor, this.getBottomMostRow(), this.getCol(),active?this.getBlockChars().getBOTTOM_RIGHT():this.getBlockChars().getHORIZONTAL_UP())).getSegmentsBuffer());
        segments.merge(makeChildComponent(()->new CharComponent(tabColor, this.getBottomMostRow(), this.getRightMostCol(),active?this.getBlockChars().getBOTTOM_LEFT():this.getBlockChars().getHORIZONTAL_UP())).getSegmentsBuffer());
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getTabText() {
        return tabText;
    }

    public void setTabText(String tabText) {
	Rectangle new_rect = new Rectangle.Simple( new Position.Simple(this.getRow(), this.getCol()), new Dimension.Simple(3, tabText.length()+4));
	this.setRectangle( new_rect );
        this.tabText = tabText;
    }
    

}
