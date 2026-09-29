package dtb.terminalIO.components.composite;

import java.util.List;

import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.HasName;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.geom.position.PositionHorizontalRelative;

/**
 * Adds child components such that they are aligned horizontally
 */
public class HorizontalPanel extends Composite {

    int horizontalSpacer = 1;

    public HorizontalPanel() {
        super();
    }

    public HorizontalPanel(Color color) {
        super(color);
    }

    public HorizontalPanel(Color color, int horizontalSpacer) {
        super(color);
        this.setHorizontalSpacer(horizontalSpacer);
    }
    
    public int getHorizontalSpacer() {
        return horizontalSpacer;
    }

    public HorizontalPanel setHorizontalSpacer(int horizontalSpacer) {
        this.horizontalSpacer = horizontalSpacer;
        fullrecalcDim();
        return this;
    }

    public HorizontalPanel add(Component<?> component) {

        ComponentWithOffset child = findChild(component);
        if (child != null) {
            throw new IllegalArgumentException(HasName.UTIL.getName(this)+":Child Component add failure:  Duplicate child("+HasName.UTIL.getName(component)+") !");
        }

        if ( ! component.isSameLocation(Position.ZERO)) {
            throw new IllegalArgumentException(HasName.UTIL.getName(this)+":Child Component ("+HasName.UTIL.getName(component)+") add failure. Must not specify Position offet. Set to (0,0) ! Current position =" + String.valueOf(component.getPostion_TopLeft()));
        }

        ComponentWithOffset anchor = null;
        int childrenSize = getChildrenComponentsWithOffset().size();
        if (childrenSize > 0) {
            anchor = getChildrenComponentsWithOffset().get(childrenSize-1);
        }

        return (HorizontalPanel) super.add(  new PositionHorizontalRelative(anchor, horizontalSpacer) , component);
    }

    public HorizontalPanel addText(String text) {
        return add(new TextComponent(getColor(), text));
    }

    
    private void readjustAllAnchors() {
        List<ComponentWithOffset> visibleChildren = getVisibleChildrenComponentsWithOffset();
        int childCount = visibleChildren.size();
        if (childCount > 0) {
            ComponentWithOffset anchor = visibleChildren.get(0); 
            anchor.setOffset(new PositionHorizontalRelative(null));
            for (int i = 1; i < childCount; i++) {
                ComponentWithOffset componentWithOffset = visibleChildren.get(i);
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
