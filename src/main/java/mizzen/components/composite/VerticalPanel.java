package mizzen.components.composite;

import java.util.List;

import mizzen.components.Component;
import mizzen.components.HasName;
import mizzen.components.TextComponent;
import mizzen.controlcodes.color.Color;
import mizzen.geom.Rectangle;
import mizzen.geom.position.Position;
import mizzen.geom.position.PositionVerticalRelative;

/**
 * Adds child components such that they are stacked vertically
 */
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

    public VerticalPanel add(Component<?> component) {

        ComponentWithOffset child = findChild(component);

        if (child != null) {
            throw new IllegalArgumentException(HasName.UTIL.getName(this)+":Child Component add failure:  Duplicate child("+HasName.UTIL.getName(component)+") !");
        }

        if ( ! component.isSameLocation(Position.ZERO)) {
            throw new IllegalArgumentException(HasName.UTIL.getName(this)+":Child Component ("+HasName.UTIL.getName(component)+") add failure. Must not specify offet. Set to (0,0) ! Current position =" + String.valueOf(component.getPostion_TopLeft()));
        }

        ComponentWithOffset anchor = null;
        int childrenSize = getChildrenComponentsWithOffset().size();
        if (childrenSize > 0) {
            // last component becomes the anchor
            anchor = getChildrenComponentsWithOffset().get(childrenSize-1);
        }

        return (VerticalPanel) super.add(makeOffsetForChild(anchor, verticalSpacer) , component);
    }

    public VerticalPanel addText(String text) {
        return add(new TextComponent(getColor(), text));
    }
    
    
    private Position makeOffsetForChild(ComponentWithOffset anchor, int verticalSpacer) {
        RectangleFromAnchor rectangleFromAnchor = null; 
        if (anchor != null) {
           rectangleFromAnchor = new RectangleFromAnchor(anchor); 
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
        //offsetsMap.clear();
        List<ComponentWithOffset> visibleChildren = getVisibleChildrenComponentsWithOffset();
        int childCount = visibleChildren.size();
        if (childCount > 0) {
            ComponentWithOffset anchor = visibleChildren.get(0); 
            anchor.setOffset(makeOffsetForChild(null, verticalSpacer));
            for (int i = 1; i < childCount; i++) {
                ComponentWithOffset componentWithOffset = visibleChildren.get(i);
                componentWithOffset.setOffset(makeOffsetForChild(anchor, verticalSpacer)  );
                anchor = componentWithOffset;
            }
        }
    }

    public void fullrecalcDim() {
        readjustAllAnchors();
        super.fullrecalcDim();
    }

    private class RectangleFromAnchor extends Rectangle.Delegate {

        ComponentWithOffset anchor;
        boolean needsRectangleCalc = false;

        public RectangleFromAnchor(ComponentWithOffset anchor) {
            super(anchor == null ? Rectangle.ZERO : new Rectangle.Simple(anchor.getRectangleForLayout()));
            this.anchor = anchor;
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