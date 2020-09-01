package dtb.terminalIO.components;

import java.util.HashMap;
import java.util.Map;

import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.geom.position.PositionVerticalRelative;

public class VerticalPanel extends Composite {

    private int verticalSpacer = 0;

    public VerticalPanel() {
        super();
    }

    public VerticalPanel(Color color) {
        super(color);
    }

    // todo - really need to introduce concept of layout managers

    public int getVerticalSpacer() {
        return verticalSpacer;
    }

    public VerticalPanel setVerticalSpacer(int verticalSpacer) {
        this.verticalSpacer = verticalSpacer;
        fullrecalcDim();
        return this;
    }
    
    public DefaultFocusManager buildFocusManager() {
        DefaultFocusManager focusManager = new DefaultFocusManager();
        focusManager.setNavKeysFirst(false);
        return focusManager;
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
            // last component becomes the anchor
            anchor = getChildrenComponentsWithOffset().get(childrenSize-1);
        }

        return super.add(makeOffsetForChild(anchor, verticalSpacer) , component);
    }

    public Composite remove(Component<?> component) {
        ComponentWithOffset child = findChildOrFail(component, "remove");
        super.remove(component);
        offsetsMap.remove(child);
        return this;
    }

    private Map<ComponentWithOffset, RectangleFromAnchor> offsetsMap = new HashMap<ComponentWithOffset,RectangleFromAnchor>();

    private Position makeOffsetForChild(ComponentWithOffset anchor, int verticalSpacer) {
        RectangleFromAnchor rectangleFromAnchor = null; 
        if (anchor != null) {
           rectangleFromAnchor = new RectangleFromAnchor(anchor); 
           offsetsMap.put(anchor, rectangleFromAnchor);
        }
        return new PositionVerticalRelative(rectangleFromAnchor, verticalSpacer);
    }

    public void markAsDirty() {
        if ( ! isDirty()) {
            readjustAllAnchors();
        }
        super.markAsDirty();
    }
    
    
    private void readjustAllAnchors() {
        for (ComponentWithOffset child : getChildrenComponentsWithOffset()) {
            RectangleFromAnchor rectFromAnchor = offsetsMap.get(child);
            if (rectFromAnchor != null) {
                rectFromAnchor.reCalcRectangle();
            }
        }
    }

    private class RectangleFromAnchor extends Rectangle.Delegate {

        ComponentWithOffset anchor;
        boolean needsRectangleCalc = false;

        public RectangleFromAnchor(ComponentWithOffset anchor) {
            super(anchor == null ? Rectangle.ZERO : new Rectangle.Simple(anchor.getRectangleForLayout()));
            this.anchor = anchor;
        }

        public void reCalcRectangle() {
            needsRectangleCalc = true;
        }

        public Rectangle getRectangle() {
            if (anchor == null) return Rectangle.ZERO;
            if (needsRectangleCalc) {
                super.setRectangle( new Rectangle.Simple(anchor.getRectangleForLayout()) );
                needsRectangleCalc = false;
            }
            return super.getRectangle();
        }
        
    }

}