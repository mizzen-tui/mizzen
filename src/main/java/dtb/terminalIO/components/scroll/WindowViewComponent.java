package dtb.terminalIO.components.scroll;

import java.util.Collection;
import java.util.Collections;

import dtb.terminalIO.components.BoundedComponent;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.GetComponentAtOffset;
import dtb.terminalIO.components.ParentContainer;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.textsegments.ScreenLineSegmentsView;
import dtb.terminalIO.util.ElapsedTime;
import dtb.terminalIO.util.Logging;

/**
 * Used exclusively by the ScrollableComponent.  It is the View into the scrollable content.
 *
 */
public class WindowViewComponent extends BoundedComponent<WindowViewComponent>
                                 implements  ParentContainer, 
                                             GetComponentAtOffset {

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
        
        long elapsed = ElapsedTime.onRunOf(()->{
            
            segments.clearAndMerge(segmentsViewBuffer.getSegments());
            
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

    Position getOffsetIntoSource() {
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
    
    

}