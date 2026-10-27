package mizzen.components.scroll;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Supplier;

import mizzen.components.BoundedComponent;
import mizzen.components.Component;
import mizzen.components.GetComponentAtOffset;
import mizzen.components.HasName;
import mizzen.components.ParentContainer;
import mizzen.controlcodes.color.Color;
import mizzen.events.OnRequestScrollToViewEventHandler;
import mizzen.events.RequestScrollToView;
import mizzen.events.RequestUpdateCanvasEvent;
import mizzen.geom.Dimension;
import mizzen.geom.Rectangle;
import mizzen.geom.position.Position;
import mizzen.textsegments.IScreenLineSegmentsBuffer;
import mizzen.textsegments.ScreenLineSegmentsView;
import mizzen.traverse.FindPathToTarget;
import mizzen.util.ElapsedTime;

/**
 * Used by ScrollableComponent.  It is the View into the scrollable content.
 */
public class WindowViewComponent extends BoundedComponent<WindowViewComponent>
                                 implements  ParentContainer, 
                                             GetComponentAtOffset {

    private Component<?> viewSource;
    private ScreenLineSegmentsView segmentsViewBuffer;

    private OnResizeService onViewSourceResizeService = new OnResizeService();

    public WindowViewComponent(Color color, Dimension d, Component<?> viewSource) {
        super(color, d);
        setViewSource(viewSource);
    }

    public void setViewSource(Component<?> viewSource) {
        if (this.viewSource != null ) {
            // remove existing viewsource and tell it that it is 
            // now orphaned .. Boo hoo..
            this.viewSource.setParentContainer(null);
        }
        this.viewSource = viewSource;
        this.viewSource.setParentContainer(this);
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
        segmentsViewBuffer.resize(sizeAfterSuperResize());          // not d: the parent's rules may have resized this again
    }

    public void markAsDirty() {
        if (segmentsViewBuffer != null) {
            segmentsViewBuffer.markAsDirty();
        }
        super.markAsDirty();
    }

    public static interface CustomComponentDrawFunction  {
        void draw(IScreenLineSegmentsBuffer componentSegments, IScreenLineSegmentsBuffer segmentsViewBuffer);
    }

    CustomComponentDrawFunction segmentsDrawFunction = (mySegments, clippedSegments) -> {
        mySegments.clearAndMerge(segmentsViewBuffer.getSegments());
    };

    public CustomComponentDrawFunction getSegmentsDrawFunction() {
        return segmentsDrawFunction;
    }

    public void setSegmentsDrawFunction(CustomComponentDrawFunction segmentsDrawFunction) {
        this.segmentsDrawFunction = segmentsDrawFunction;
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer mySegments) {
        
        //Logging.info("WindowViewComponent draw callled!" + String.valueOf(this));
        
        long elapsed = ElapsedTime.onRunOf(()->{
            
            segmentsDrawFunction.draw(mySegments, segmentsViewBuffer);
            
            //mySegments.clearAndMerge(segmentsViewBuffer.getSegments());
            
        }).elapsed();

        //Logging.info("    ---- sc.viewer.draw() - elapsed: " + elapsed );
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

        // adjust offset relative to viewSource
        Position adjustedOffset = offset.offsetAdd( this.getPositionRelativeToParent(viewSourceComponent).negate());

        if (viewSourceComponent.containsPosition(adjustedOffset)) {
            // adjusted offset is inside (contained by) viewSourceComponent
            if (viewSourceComponent instanceof GetComponentAtOffset) {
                return ((GetComponentAtOffset)viewSourceComponent).getComponentAtOffset(adjustedOffset);
            }
            return viewSourceComponent;
        }
        return this;
    }

    public static class ScrollToViewHandler extends OnRequestScrollToViewEventHandler {

        public ScrollToViewHandler(Supplier<WindowViewComponent> viewerProvider, CanScrollToView scrollable) {
            super((te)-> {
                            // CAN-HANDLE-PREDICATE 
                            if (viewerProvider.get() == null) {
                               return false;
                            }
                            Component<?> componentToScrollTo = ((RequestScrollToView)te).getScrollToComponent();
                            
                            boolean findPathToComponent = new FindPathToTarget<Component<?>>().exists(viewerProvider.get(), componentToScrollTo);
                            
                            return findPathToComponent;
                         }, 
                  (te)-> {
                            // THE-HANDLER
                            Component<?> componentToScrollTo = ((RequestScrollToView)te).getScrollToComponent();
                            Position offset = Component.UTIL.getOffsetFromComponentContainer(componentToScrollTo, viewerProvider.get());
                            Position offsetIntoSource = viewerProvider.get().getOffsetIntoSource();
                            if ( ! offsetIntoSource.isSameLocation(Position.ZERO)) {
                                if (offset == null) {
                                    // offset should not be null findPathToComponent (above) was true.
                                    throw new NullPointerException("Bad state: offset null for "+ HasName.UTIL.getName(componentToScrollTo));
                                }
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