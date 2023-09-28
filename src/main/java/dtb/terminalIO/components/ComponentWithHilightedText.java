package dtb.terminalIO.components;

import java.util.ArrayList;
import java.util.List;

import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.textsegments.CleanString;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.textsegments.ScreenLineSegment;
import dtb.terminalIO.util.ElapsedTime;
import dtb.terminalIO.util.Logging;

public class ComponentWithHilightedText extends Composite implements Focusable {

    private Color                hiliteTextColor    = new Colors.FGBGColors(Colors.BBla, Colors.On_Whi);

    private List<Component<?>>   hilightList        = new ArrayList<Component<?>>();

    private List<HilightRequest> hilightRequestList = new ArrayList<HilightRequest>();

    private Focusable focusable;

    private class HilightRequest {
        Color     hiliteColor;
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

        if (hilightRequestList.size() == 0) {
            return null;
        }

        List<ComponentWithOffset> addedComponentsList = new ArrayList<ComponentWithOffset>(hilightRequestList.size());

        for (HilightRequest hilightRequest : hilightRequestList) {
            ScreenLineSegment sc = segments.getAtCol(hilightRequest.r.getRow(), hilightRequest.r.getCol());
            if (sc == null || !sc.overlapsWith(hilightRequest.r) || sc.getWidth() < hilightRequest.r.getWidth()) {
                continue;
            }

            CleanString s = sc.substring(hilightRequest.r.getCol() - sc.getCol());

            s = s.substring(0, hilightRequest.r.getWidth());

            TextComponent sc_SRCH = new TextComponent(hilightRequest.hiliteColor, hilightRequest.r, s);

            ComponentWithOffset componentWithOffset = this.addComponentWithOffset(hilightRequest.r, sc_SRCH);

            addedComponentsList.add(componentWithOffset);

            hilightList.add(sc_SRCH);
        }
        hilightRequestList.clear();

        return addedComponentsList;
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        Logging.info( "draw()" + ElapsedTime.onRunOf(()-> super.draw(color, segments)));
    }
    
    
    @Override
    synchronized protected void drawChildren(Color color, IScreenLineSegmentsBuffer segments, List<ComponentWithOffset> children,
            boolean boundsFixed) {
        super.drawChildren(color, segments, children, boundsFixed);
        if (hilightRequestList.size() > 0) {
            List<ComponentWithOffset> addedComponentsList = addHilites(color, segments);
            if (addedComponentsList != null && addedComponentsList.size() > 0) {
                super.drawChildren(color, segments, addedComponentsList, boundsFixed);
                // this.markAsDirty();
            }
        }
    }

    public void addHilitedRegion(Color hiliteColor, Rectangle r) { // assume a height of 1 for now
        this.hilightRequestList.add(new HilightRequest(hiliteColor, r));
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

    
    public void setFocusable(Focusable focusable) {
        this.focusable = focusable;
    }

    @Override
    public EventHandler canHandle(TerminalEvent<?> te) {
        return focusable == null ? null : focusable.canHandle(te);
    }

    @Override
    public void handle(TerminalEvent<?> te) {
        if (focusable == null) {
            return;
        }
        focusable.handle(te);
    }

    @Override
    public boolean hasFocus() {
        return (focusable == null) ? false : focusable.hasFocus();
    }

    @Override
    public void setFocus(boolean focus, EventBus eventBus) {
        if (focusable == null) {
            return;
        }
        focusable.setFocus(focus, eventBus);
    }

    @Override
    public FocusManager getParentFocusManager() {
        if (focusable == null) {
            return null;
        }
        return focusable.getParentFocusManager();
    }

    @Override
    public void setParentFocusManager(FocusManager focusManager) {
        if (focusable == null) {
            return;
        }
        Focusable.UTIL.setParentFocusManager(this, focusManager, (fm)-> focusable.setParentFocusManager(fm));
    }
    
    
}