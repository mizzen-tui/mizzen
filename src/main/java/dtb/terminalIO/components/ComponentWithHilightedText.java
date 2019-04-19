package dtb.terminalIO.components;

import java.util.ArrayList;
import java.util.List;

import dtb.terminalIO.controlcodes.Colors;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.controlcodes.ControlCode.FGBGColors;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.textsegments.ScreenLineSegment;
import dtb.terminalIO.textsegments.ScreenLineSegment.SegmentImpl;

public class ComponentWithHilightedText extends Composite {
    
    private Color hiliteTextColor = new FGBGColors(Colors.BBla,Colors.On_Whi);

    private List<Component<?>> hilightList = new ArrayList<Component<?>>();
    
    private List<HilightRequest> hilightRequestList = new ArrayList<HilightRequest>();
    
    private class HilightRequest {
        Color hiliteColor;
        Rectangle r;
        public HilightRequest(Color hiliteColor, Rectangle r) {
            super();
            this.hiliteColor = hiliteColor;
            this.r = r;
        }
    }
    
    
    public ComponentWithHilightedText(Color color) {
        super(color);
    }

    public ComponentWithHilightedText() {
        super();
    }
    
    
    public Color getHiliteTextColor() {
        return hiliteTextColor;
    }

    public void setHiliteTextColor(Color hiliteTextColor) {
        this.hiliteTextColor = hiliteTextColor;
    }

    private List<ComponentWithOffset> addHilites(Color color, IScreenLineSegmentsBuffer segments) {
        
        if (hilightRequestList.size() ==0) {
            return null;
        }
        
        List<ComponentWithOffset> addedComponentsList = new ArrayList<ComponentWithOffset>(hilightRequestList.size());
        
        for (HilightRequest hilightRequest : hilightRequestList ) {
            ScreenLineSegment sc = segments.getAtCol(hilightRequest.r.getRow(), hilightRequest.r.getCol());
            if (sc == null || !sc.overlapsWith(hilightRequest.r) || sc.getWidth() < hilightRequest.r.getWidth()) {
                continue;
            }
            String sc_text = sc.getText();
            
            String s = sc_text.substring(hilightRequest.r.getCol() - sc.getCol());
            
            s = s.substring(0, hilightRequest.r.getWidth());
            
            TextComponent sc_SRCH = new TextComponent(hilightRequest.hiliteColor, hilightRequest.r,  s);
            //SegmentComponent sc_SRCH = new SegmentComponent(new SegmentImpl(hilightRequest.hiliteColor, hilightRequest.r,-1,s));
            
            
            ComponentWithOffset componentWithOffset = this.addComponentWithOffset(hilightRequest.r, sc_SRCH);
            
            addedComponentsList.add(componentWithOffset);
            
            hilightList.add(sc_SRCH);
        }
        hilightRequestList.clear();
        
        return addedComponentsList;
    }
    
    public void markAsDirty() {
        super.markAsDirty();
        // Why mark the children dirty - not necessary is it?
//      for (ComponentWithOffset child : children) {
//          child.component.markAsDirty();
//      }
    }

    
    @Override    
    synchronized protected void drawChildren(Color color, IScreenLineSegmentsBuffer segments, List<ComponentWithOffset> children, boolean boundsFixed) {
        super.drawChildren(color, segments, children, boundsFixed);
        if (hilightRequestList.size()>0) {
            List<ComponentWithOffset> addedComponentsList = addHilites(color, segments);
            if (addedComponentsList != null && addedComponentsList.size()>0) {
                super.drawChildren(color, segments, addedComponentsList, boundsFixed);
                this.markAsDirty();
            }
        }
    }
    
    public void addHilitedRegion(Color hiliteColor, Rectangle r) { // assume a height of 1 for now
        this.hilightRequestList.add( new HilightRequest( hiliteColor, r ));
        this.markAsDirty();
    }
    
    public void addHilitedRegion(Rectangle r) { // assume a height of 1 for now
        addHilitedRegion(hiliteTextColor, r);
    }
    
    public void clearHiliteRegion(Rectangle r) { // assume a height of 1 for now
        Component<?> c_removed = null;
        for (Component<?> c : hilightList) {
            
            // to get the actual rectangle of c we need its ComponentWithOffset 
            ComponentWithOffset child = findChild(c);
            
            if (child != null && Rectangle.DEFAULT_COMPARATOR.compare(r, child) == 0) { // equals
                this.remove(c);
                c_removed = c;
                break;
            }
        }
        if (c_removed != null) {
           hilightList.remove(c_removed);
        }
    }
    
    
    public void clearHilights() {
        for (Component<?> c : hilightList) {
            this.remove(c);
        }
        hilightList.clear();
    }
    
}