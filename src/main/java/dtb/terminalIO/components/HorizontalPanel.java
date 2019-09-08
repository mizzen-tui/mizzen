package dtb.terminalIO.components;

import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.geom.position.PositionHorizontalRelative;

public class HorizontalPanel extends Composite {

    int horizontalSpacer = 1;
    
    public HorizontalPanel() {
        super();
    }

    public HorizontalPanel(Color color) {
        super(color);
    }
    
    // todo - really need to introduce concept of layout managers
    
    public int getHorizontalSpacer() {
        return horizontalSpacer;
    }

    public HorizontalPanel setHorizontalSpacer(int horizontalSpacer) {
        this.horizontalSpacer = horizontalSpacer;
        fullrecalcDim();
        return this;
    }

    public Composite add(Component<?> component) {
        
        ComponentWithOffset child = findChild(component);
        if (child != null) {
            throw new IllegalArgumentException("Child Component add failure:  Duplicate !");
        }
        
        if ( ! component.isSamePosition(Position.ZERO)) {
            throw new IllegalArgumentException("Child Component add failure. Must not specify offet. Set to (0,0) ! Current position =" + String.valueOf(component.getPostion_TopLeft()));
        }
        
        ComponentWithOffset anchor = null;
        int childrenSize = getChildrenComponentsWithOffset().size();
        if (childrenSize > 0) {
            anchor = getChildrenComponentsWithOffset().get(childrenSize-1);
        }

        return super.add(  new PositionHorizontalRelative(anchor, horizontalSpacer) , component);
    }

    private void readjustAllAnchors() {
        int childCount = getChildrenComponentsWithOffset().size();
        if (childCount > 0) {
            ComponentWithOffset anchor = getChildrenComponentsWithOffset().get(0); 
            anchor.setOffset(new PositionHorizontalRelative(null));
            for (int i = 1; i < childCount; i++) {
                ComponentWithOffset componentWithOffset = getChildrenComponentsWithOffset().get(i);
                componentWithOffset.setOffset(new PositionHorizontalRelative(anchor.getRectangleForLayout(), horizontalSpacer)  );
                anchor = componentWithOffset;
            }
        }
    }
    
    public void fullrecalcDim() {
        readjustAllAnchors();
        super.fullrecalcDim();
    }

}
