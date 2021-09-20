package dtb.terminalIO.components.scroll;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Supplier;

import dtb.terminalIO.components.BoundedComponent;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.GetComponentAtOffset;
import dtb.terminalIO.components.ParentContainer;
import dtb.terminalIO.components.WindowFrame;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.OnRequestScrollToViewEventHandler;
import dtb.terminalIO.events.RequestScrollToView;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.textsegments.ScreenLineSegmentsView;
import dtb.terminalIO.traverse.FindPathToTarget;
import dtb.terminalIO.util.ElapsedTime;
import dtb.terminalIO.util.Logging;

/**
 * Used exclusively by the ScrollableComponent.  It is the View into the scrollable content.
 */
public class WindowViewComponent extends BoundedComponent<WindowViewComponent>
                                 implements  ParentContainer, 
                                             GetComponentAtOffset {

    private IScreenLineSegmentsBuffer.Provider viewSource;
    private ScreenLineSegmentsView segmentsViewBuffer;
    
    private OnResizeService onViewSourceResizeService = new OnResizeService();

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
        segmentsViewBuffer = new ScreenLineSegmentsView(this.viewSource, WindowViewComponent.this.getRectangle());
        this.markAsDirty();
    }

    public IScreenLineSegmentsBuffer.Provider getViewSource() {
        return this.viewSource;
    }


    public Position calculateScrollToViewOffset(Rectangle targetToView) {
        return segmentsViewBuffer.calculateScrollToViewOffset(targetToView);
    }

    @Override
    public void resize(Dimension d) {
        super.resize(d);
        segmentsViewBuffer.resize(d);
    }

    public void markAsDirty() {
        if (segmentsViewBuffer != null) {
            segmentsViewBuffer.markAsDirty();
        }
        super.markAsDirty();
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer mySegments) {
        
        long elapsed = ElapsedTime.onRunOf(()->{
            
            mySegments.clearAndMerge(segmentsViewBuffer.getSegments());
            
        }).elapsed();

        Logging.info("    ---- sc.viewer.draw() - elapsed: " + elapsed );
    }

    public boolean setViewOffset(int r, int c) {
        return setViewOffset(new Position.Simple(r, c));
    }

    public boolean setViewOffset(Position offset) {
        boolean changed = segmentsViewBuffer.setOffsetIntoSource(offset);
        if (changed) {
           markAsDirty();
        }
        return changed;
    }

    public Position getOffsetIntoSource() {
        return segmentsViewBuffer.getOffsetIntoSource();
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
        if (child == this.viewSource) {
           this.onViewSourceResizeService.resize(child);
        }
    }

    public WindowViewComponent setOnViewSourcResize(Consumer<Dimension> onResize) {
        this.onViewSourceResizeService.setOnResize(onResize);
        return this;
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

    
    public static class ScrollToViewHandler extends OnRequestScrollToViewEventHandler {

        public ScrollToViewHandler(Supplier<WindowViewComponent> viewerProvider, CanScrollToView scrollable) {
            super((te)-> {
                            if (viewerProvider.get() == null) {
                               return false;
                            }
                            Component<?> componentToScrollTo = ((RequestScrollToView)te).getScrollToComponent();
                            
                            boolean findPathToComponent = new FindPathToTarget<Component<?>>().exists(viewerProvider.get(), componentToScrollTo);
                            
                            return findPathToComponent;
                         }, 
                  (te)-> {
                            Component<?> componentToScrollTo = ((RequestScrollToView)te).getScrollToComponent();
                            Position offset = Component.UTIL.getOffsetFromComponentContainer(componentToScrollTo, viewerProvider.get());
                            Position offsetIntoSource = viewerProvider.get().getOffsetIntoSource();
                            if ( ! offsetIntoSource.isSameLocation(Position.ZERO)) {
                                offset = offset.offsetAdd( offsetIntoSource);
                            }
                            if (offset != null) {
                                Rectangle offsetRectangle = ((RequestScrollToView)te).getScrollToOffsetRectangle();
                                if (offsetRectangle != null) {
                                    offsetRectangle = new Rectangle.Simple(offsetRectangle.offsetAdd(offset), (Dimension)offsetRectangle);
                                    if (scrollable.scrollToView(offsetRectangle)) {
                                        te.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(scrollable) );
                                    }
                                } else {
                                    if ( scrollable.scrollToView(new Rectangle.Simple(offset, (Dimension) componentToScrollTo))) {
                                        te.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(scrollable) );
                                    }
                                }
                            }
                            
                         });
        }
    }

}