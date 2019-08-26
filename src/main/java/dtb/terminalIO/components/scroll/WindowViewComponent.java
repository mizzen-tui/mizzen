package dtb.terminalIO.components.scroll;

import java.util.Collection;
import java.util.Collections;

import dtb.terminalIO.components.BoundedComponent;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.Hiliter;
import dtb.terminalIO.components.ParentContainer;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.mouse.GetComponentAtOffset;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.textsegments.ScreenLineSegmentsView;

/**
 * Used by ScrollableComponent.  It is the View into content.
 *
 */
public class WindowViewComponent extends BoundedComponent<WindowViewComponent>
        implements Hiliter, Focusable, ParentContainer, GetComponentAtOffset {

    private IScreenLineSegmentsBuffer.Provider viewSource;
    private ScreenLineSegmentsView segmentsViewBuffer;

    public WindowViewComponent(Color color, Dimension d, IScreenLineSegmentsBuffer.Provider viewSource) {
        super(color, d);
        setViewSource(viewSource);
    }

    public void setViewSource(IScreenLineSegmentsBuffer.Provider viewSource) {
        if (this.viewSource != null && this.viewSource instanceof Component) {
            ((Component<?>)this.viewSource).setParentContainer(null);
        }
        this.viewSource = viewSource;
        if (viewSource instanceof Component) {
            ((Component<?>) this.viewSource).setParentContainer(this);
        }
        segmentsViewBuffer = new ScreenLineSegmentsView(this.viewSource, (Rectangle) WindowViewComponent.this);
        this.markAsDirty();
    }

    public Position calculateScrollToViewOffset(Rectangle targetToView) {
        return segmentsViewBuffer.calculateScrollToViewOffset(targetToView);
    }

    @Override
    public void resize(Dimension d) {
        super.resize(d);
        segmentsViewBuffer.resize(d);
    }

    public boolean isDirty() {
        if (super.isDirty())
            return true;
        return segmentsViewBuffer.isDirty();
    }

    public void markAsDirty() {
        super.markAsDirty();
        if (segmentsViewBuffer != null) {
            segmentsViewBuffer.markAsDirty();
        }
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        segments.merge(segmentsViewBuffer);
    }

    public void setViewOffset(int r, int c) {
        setViewOffset(new Position.Simple(r, c));
    }

    public void setViewOffset(Position offset) {
        markAsDirty();
        segmentsViewBuffer.setOffsetIntoSource(offset);
    }

    Position getOffsetIntoSource() {
        return segmentsViewBuffer.getOffsetIntoSource();
    }

    @Override
    public void hilite() {
        if (viewSource instanceof Hiliter) {
            ((Hiliter) viewSource).hilite();
        }
    }

    @Override
    public void unHilite() {
        if (viewSource instanceof Hiliter) {
            ((Hiliter) viewSource).unHilite();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<? extends Component<?>> getChildren() {
        if (viewSource instanceof Component) {
            return (Collection<? extends Component<?>>) Collections.singleton((Component<?>) this.viewSource);
        }
        return Collections.emptyList();
    }

    @Override
    public Position getPositionRelativeToParent(Component<?> child) {
        Position offsetIntoSource = getOffsetIntoSource();
        return new Position.Simple(offsetIntoSource.getRow()*-1, offsetIntoSource.getCol()*-1);
        //return Position.ZERO; // todo - need a proper implementation (using offset)
    }

    public void childResized(Component<?> child) {
        markAsDirty();
    }

    @Override
    public Component<?> getComponentAtOffset(Position offset) {
        if ( ! this.containsPosition(offset)) {
            return null;
        }
        
        if (!(viewSource instanceof Component)) {
            return this;
        }
        
        Component<?> viewSourceComponent = (Component<?>) viewSource;
        
        offset = offset.offsetAdd( this.getPositionRelativeToParent(viewSourceComponent).negate());

        if (viewSourceComponent.containsPosition(offset)) {
            if (viewSourceComponent instanceof GetComponentAtOffset) {
                return ((GetComponentAtOffset)viewSourceComponent).getComponentAtOffset(offset);
            }
            return viewSourceComponent;
        }
        return this;
    }
    
    
    @Override
    public EventHandler canHandle(TerminalEvent<?> te) {
        if (viewSource instanceof ConditionalEventHandler) {
            return ((ConditionalEventHandler) viewSource).canHandle(te);
        }
        return null;
    }

    @Override
    public void handle(TerminalEvent<?> te) {
        if (viewSource instanceof EventHandler) {
            ((EventHandler) viewSource).handle(te);
        }
    }

    @Override
    public boolean hasFocus() {
        if (viewSource instanceof Focusable) {
            return ((Focusable) viewSource).hasFocus();
        }
        return false;
    }

    @Override
    public void setFocus(boolean focus, EventBus eventBus) {
        if (viewSource instanceof Focusable) {
            ((Focusable) viewSource).setFocus(focus, eventBus);
            return;
        }
    }
}