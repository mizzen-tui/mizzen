package dtb.terminalIO.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.geom.CanResize;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.textsegments.ScreenLineSegment;

public class HilitingComponent extends AbstractComponent<HilitingComponent>   implements ParentContainer, 
                                                                                         CanResize, 
                                                                                         GetComponentAtOffset, 
                                                                                         HasName<HilitingComponent> {

    private Color selectedTextColor    = new Colors.FGBGColors(Colors.BBla, Colors.On_IYel);
    private Color hiliteTextColor    = new Colors.FGBGColors(Colors.BBla, Colors.On_Whi);
    private List<HilightRequest> hilightRequestList = new ArrayList<HilightRequest>();
    private HilightRequest selected;

    private Component<?> child;

    
    public HilitingComponent(Color color, Component<?> child) {
        super(color);
        this.child = child;
        child.setParentContainer(this);
    }

    public Color getHiliteTextColor() {
        return hiliteTextColor;
    }

    public Color getSelectedTextColor() {
        return selectedTextColor;
    }

    public void setHiliteTextColor(Color hiliteTextColor) {
        this.hiliteTextColor = hiliteTextColor;
    }

    public void setSelected(HilightRequest selected) {
        if (hilightRequestList.contains(selected)) { // potentially expensive
            this.selected = selected;
            this.markAsDirty();
        }
    }

    public void setSelectedIndex(int index) {
        if (hilightRequestList == null || hilightRequestList.size()==0 || index>= hilightRequestList.size() ) {
            return;
        }
        
        this.selected = hilightRequestList.get(index);
        
        // this is a bit of trickery to ensure parent redraws itself.. 
        if (getParentContainer() != null) {
            getParentContainer().markAsDirty();
        }
        
//        this.markAsDirty();
    }

    public void addHilitedRegion(Color hiliteColor, Rectangle r) { // assume a height of 1 for now
        if (this.hilightRequestList == null) {
            this.hilightRequestList = new ArrayList<HilightRequest>(); 
        }
        this.hilightRequestList.add(new HilightRequest(hiliteColor, r));
        this.markAsDirty();
    }

    public void addHilitedRegion(Rectangle r) { // assume a height of 1 for now
        addHilitedRegion(hiliteTextColor, r);
    }

    public void clearHilights() {
        
        if (this.hilightRequestList != null && this.hilightRequestList.size()>0) {
            this.markAsDirty();
        }
        
        this.hilightRequestList = null;
        
    }
    
    @Override
    public Collection<? extends Component<?>> getChildren() {
        return Collections.singleton(child);
    }

    @Override
    public int getRow() {
        return child.getRow();
    }

    @Override
    public int getCol() {
        return child.getCol();
    }

    @Override
    public int getWidth() {
        return child.getWidth();
    }

    @Override
    public int getHeight() {
        return child.getHeight();
    }

    @Override
    public void moveTo(int row, int col) {
        child.moveTo(row, col);
    }

    @Override
    public Component<?> getComponentAtOffset(Position offset) {
        return child;
    }


    @Override
    public void resize(Dimension d) {
        if (child instanceof CanResize) {
            ((CanResize) child).resize(d);
        }
    }

    @Override
    public Position getPositionRelativeToParent(Component<?> child) {
        return Position.ZERO;
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

        
        TreeSet<ScreenLineSegment> ts = ScreenLineSegment.UTIL.makeCollection(child.getSegmentsBuffer().getSegments());
        
        if (hilightRequestList != null ) {
            hilightRequestList.stream().map(r -> r.toScreenLineSegment())
                                       .filter(sc-> sc != null)
                                       .forEach(sc -> ScreenLineSegment.UTIL.addTo(ts, sc) );
        }
        segments.clearAndMerge( ts );
    }

    @Override
    protected Rectangle getRectangleForClearAndFill() {
        return this;
    }
    
    
    private class HilightRequest {
        Color     hiliteColor;
        Rectangle r;

        public HilightRequest(Color hiliteColor, Rectangle r) {
            super();
            this.hiliteColor = hiliteColor;
            this.r = r;
        }

        ScreenLineSegment toScreenLineSegment() {
            final ScreenLineSegment segment = ScreenLineSegment.UTIL.getAtRowCol(child.getSegmentsBuffer().getSegments(),  r.getRow(),r.getCol());
            if (segment == null || ! segment.contains(r)) {
                return null;
            }
            
            return new ScreenLineSegment() {

                String _clippedText = null;

                @Override
                public void moveTo(int row, int col) {
                    throw new IllegalStateException("No implemented yet!");
                }

                @Override
                public int getId() {
                    return segment.getId();
                }

                @Override
                public String getText() {
                    if (_clippedText != null) {
                        return _clippedText;
                    }

                    int startOffset = r.getCol()-segment.getCol();
                    _clippedText = segment.getText().substring(startOffset, r.getRightMostCol() - segment.getCol() + 1 );

                    return _clippedText;
                }

                @Override
                public Color getColors() {
                    return (selected == HilightRequest.this) ? selectedTextColor :  HilightRequest.this.hiliteColor;
                }

                @Override
                public int length() {
                    return r.getWidth();
                }

                @Override
                public int getRow() {
                    return r.getRow();
                }

                @Override
                public int getCol() {
                    return r.getCol();
                }

                @Override
                public ScreenLineSegment fromOffset(Position offset) {
                    return new SegmentImpl(getColor(), new Position.Simple(this).offsetAdd(offset), getId(), this.getText());
                }
                
            };
        }
        
    }
    

}
