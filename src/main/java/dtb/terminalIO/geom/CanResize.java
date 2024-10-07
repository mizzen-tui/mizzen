package dtb.terminalIO.geom;

import java.util.function.Consumer;

import dtb.terminalIO.components.BoundedComponent;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.geom.position.Position;

/**
 * Specifies how to resize itself
 */
public interface CanResize {

    
    /**
     * resize object with the current size
     */
    public void resize(Dimension d);
    
    public default Consumer<Dimension> asOnResizeService() {
        return (d) -> CanResize.this.resize(d);
    }
    
    public static CanResize NOOP = new CanResize() {

        @Override
        public void resize(Dimension d) {
            // do nothing
        }
    };
    
    public static class UTIL {
        public static CanResize resizableFromService(Consumer<Dimension> service) {
            return new CanResize() {
                @Override
                public void resize(Dimension d) {
                    service.accept(d);
                }
            };
        }
    }

    public static class Delegate implements CanResize {

        protected CanResize delegate;

        public Delegate(CanResize delegate) {
            super();
            this.delegate = delegate;
        }

        public CanResize getDelegate() {
            return delegate;
        }

        @Override
        public void resize(Dimension d) {
            if (getDelegate() != null) {
               getDelegate().resize(d);
            }
        }
    }
    public static class Chain extends Delegate {

        CanResize next;

        public Chain(CanResize delegate) {
            super(delegate);
            this.next = null;
        }

        public Chain(CanResize delegate, CanResize next) {
            super(delegate);
            this.next = next;
        }

        @Override
        public void resize(Dimension d) {
            getDelegate().resize(d);
            if (this.next != null) {
                next.resize(d);
            }
        }
        
        public void append(CanResize next) {
            if (next == null) {
                return;
            }
            if (this.delegate == null || this.delegate == NOOP) {
                this.shiftUpOnEmptyDelegate();
                if (this.delegate == null || this.delegate == NOOP) {
                    this.delegate = next;
                    return;
                }
            }
            if (this.next != null) {
                this.next = new Chain(this.next, next);
                return;
            }
            this.next = next;
        }

        static void shiftUpOnEmptyDelegate(CanResize resizer) { 
            if (resizer == null || !(resizer instanceof Chain) ) {
                return;
            }
            Chain chained = (Chain)resizer;
            chained.shiftUpOnEmptyDelegate(); 
        }
        
        void shiftUpOnEmptyDelegate() { // balance the chain so no unnecessary NOOP's 
            if (this.next == null) {
                if (this.delegate == null || this.delegate == NOOP) {
                    return;
                }
                if (this.delegate instanceof Chain) {
                    shiftUpOnEmptyDelegate(this.delegate);
                }
                return;
            }
            if (this.delegate == null || this.delegate != NOOP) {
                return;
            }
            // assertion: delegate is NOOP or null and next is not null
            this.delegate = this.next;
            this.next = null;
            if (this.delegate != null && (this.delegate instanceof Chain)) {
                Chain chained = (Chain)this.delegate;
                this.delegate = chained.delegate;
                this.next = chained.next;
                this.shiftUpOnEmptyDelegate();
            }
        }

        public void remove(CanResize resizer) {
            if (resizer == null) {
                return;
            }

            if (this.delegate == resizer) {
                this.delegate = NOOP;
                shiftUpOnEmptyDelegate();
                return;
            }

            if (this.next == resizer) {
                this.next = null;
                return;
            }

            if (this.delegate != null && this.delegate != NOOP && (this.delegate instanceof Chain)) {
                Chain chained = (Chain)this.delegate;
                chained.remove(resizer);
                shiftUpOnEmptyDelegate();
            }

            if (this.next != null && this.next != NOOP && (this.next instanceof Chain)) {
                Chain chained = (Chain)this.next;
                chained.remove(resizer);
                shiftUpOnEmptyDelegate();
            }
        }

    }

    public static class OnResizeService implements CanResize {

        Consumer<Dimension> onResize = null; 

        @Override
        public void resize(Dimension size) {
            if (onResize != null) {
                onResize.accept(size);
            }
        }

        public void setOnResize(Consumer<Dimension> onResize) {
            this.onResize = onResize;
        }
    }

    static abstract class ExpandOnResize  implements CanResize {

        Rectangle anchorComponent;
        private Position  targetOffsetPosition;
        private Rectangle targetComponent;
        private CanResize targetToResize;

        public ExpandOnResize(Rectangle anchorComponent, Position targetOffsetPosition, Rectangle targetComponent,
                CanResize targetToResize) {
            super();
            this.anchorComponent = anchorComponent;
            this.targetOffsetPosition = targetOffsetPosition;
            this.targetComponent = targetComponent;
            this.targetToResize = targetToResize;
        }

        @Override
        abstract public void resize(Dimension d);

        protected Rectangle getTargetComponent() {
            return targetComponent;
        }

        protected Rectangle getTargetToResize() {
            return targetComponent;
        }

        protected Position getTargetOffsetPosition() {
            return targetOffsetPosition;
        }

        boolean resizing = false;

        protected void resizeTarget(Dimension d) {
            if (resizing) {
                return;
            }
            try {
                resizing = true;
                if (targetToResize instanceof Dimension) {
                    if (d.isSameDimension((Dimension) targetToResize)) {
                        return;
                    }
                }
                targetToResize.resize(d);
            } finally {
                resizing = false;
            }
        }
        
    }

    public static class ExpandDown  extends ExpandOnResize {

        public ExpandDown(Rectangle anchorComponent, Position targetOffsetPosition, Rectangle targetComponent,
                CanResize targetToResize) {
            super(anchorComponent, targetOffsetPosition, targetComponent, targetToResize);
        }

        public ExpandDown(Component<?> container, Position targetOffsetPosition, BoundedComponent<?> targetComponent) {
            this(container, targetOffsetPosition, targetComponent, targetComponent);
        }

        @Override
        public void resize(Dimension d) {
            Dimension newSizeForTarget = new Dimension.Simple(anchorComponent.getHeight() - getTargetComponent().getRow() - getTargetOffsetPosition().getRow()  ,
                                                              getTargetComponent().getWidth());
            resizeTarget(newSizeForTarget);
        }

    }

    public static class ExpandDownByPercent  extends ExpandOnResize {

        int percent = 50;
        
        public ExpandDownByPercent(Rectangle anchorComponent, 
                                   Position targetOffsetPosition, 
                                   Rectangle targetComponent,
                                   CanResize targetToResize, 
                                   int percent) {
            super(anchorComponent, targetOffsetPosition, targetComponent, targetToResize);
            this.percent = percent;
        }
        
        public ExpandDownByPercent(Component<?> container, 
                                   Position targetOffsetPosition, 
                                   BoundedComponent<?> targetComponent, 
                                   int percent) {
            this(container, targetOffsetPosition, targetComponent, targetComponent, percent);
        }

        @Override
        public void resize(Dimension d) {
            
            int max = anchorComponent.getHeight() - getTargetComponent().getRow() - getTargetOffsetPosition().getRow();
            int byPercent = (int)(anchorComponent.getHeight()*((double)percent / 100d));

            Dimension newSizeForTarget = new Dimension.Simple(Math.min(max, byPercent),
                                                              getTargetComponent().getWidth());
            resizeTarget(newSizeForTarget);
        }

    }
    
    public static class ExpandRightByPercent  extends ExpandOnResize {

        int percent = 50;

        public ExpandRightByPercent(Rectangle anchorComponent, 
                                    Position targetOffsetPosition, 
                                    Rectangle targetComponent,
                                    CanResize targetToResize, 
                                    int percent) {
            super(anchorComponent, targetOffsetPosition, targetComponent, targetToResize);
            this.percent = percent;
        }

        public ExpandRightByPercent(Component<?> container, 
                                    Position targetOffsetPosition, 
                                    BoundedComponent<?> targetComponent, 
                                    int percent) {
            this(container, targetOffsetPosition, targetComponent, targetComponent, percent);
        }

        @Override
        public void resize(Dimension d) {
            
            int max = anchorComponent.getWidth()  - getTargetComponent().getCol() - getTargetOffsetPosition().getCol();
            int byPercent = (int)(anchorComponent.getWidth()*((double)percent / 100d));

            Dimension newSizeForTarget = new Dimension.Simple(getTargetComponent().getHeight(),
                                                              Math.min(max, byPercent));
            resizeTarget(newSizeForTarget);
        }

    }

    public static class ExpandRight extends ExpandOnResize {

        public ExpandRight(Rectangle anchorComponent, Position targetOffsetPosition, Rectangle targetComponent,
                CanResize targetToResize) {
            super(anchorComponent, targetOffsetPosition, targetComponent, targetToResize);
        }
        
        public ExpandRight(Component<?> container, Position targetOffsetPosition, BoundedComponent<?> targetComponent) {
            super(container, targetOffsetPosition, targetComponent, targetComponent);
        }

        public ExpandRight(Component<?> container, BoundedComponent<?> targetComponent) {
            super(container, Position.ZERO, targetComponent, targetComponent);
        }

        @Override
        public void resize(Dimension d) {
            Dimension newSizeForTarget = new Dimension.Simple(getTargetComponent().getHeight(),
                                                              anchorComponent.getWidth()  - getTargetComponent().getCol() - getTargetOffsetPosition().getCol());

            resizeTarget(newSizeForTarget);
        }
        
    }

    public static class ExpandDownAndRight extends ExpandOnResize {


        public ExpandDownAndRight(Rectangle anchorComponent, Position targetOffsetPosition, Rectangle targetComponent,
                CanResize targetToResize) {
            super(anchorComponent, targetOffsetPosition, targetComponent, targetToResize);
        }
        
        public ExpandDownAndRight(Component<?> container, Position targetOffsetPosition, BoundedComponent<?> targetComponent) {
            super(container, targetOffsetPosition, targetComponent, targetComponent);
        }

        @Override
        public void resize(Dimension d) {
            Dimension newSizeForTarget = new Dimension.Simple(anchorComponent.getHeight() - getTargetComponent().getRow() - getTargetOffsetPosition().getRow(),
                                                              anchorComponent.getWidth()  - getTargetComponent().getCol() - getTargetOffsetPosition().getCol());

            resizeTarget(newSizeForTarget);
        }
        
    }
    
}
