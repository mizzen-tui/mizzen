package mizzen.geom;

import java.util.function.Consumer;
import java.util.function.Supplier;

import mizzen.components.BoundedComponent;
import mizzen.components.Component;
import mizzen.geom.position.Position;

/**
 * Specifies how to resize itself. Also the shape of a resize rule (Composite.appendOnResizeRule,
 * ScrollableComponent.appendOnResizeRule): the rule gets the new size of the component it is
 * attached to, and is called by that component's own resize - e.g. (size) -> child.resize(...).
 * <p/>
 * One method only, so a lambda will do: keep it that way.
 */
@FunctionalInterface
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
        
        public CanResize getNext() {
            return next;
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

    /**
     * A resize rule that grows a child to fill its parent, down and/or right, from where the child
     * is. It belongs to the parent (anchorComponent: the parent's inside) and runs when the parent is
     * resized; it reads the parent's size and the child's position, and resizes the child.
     * <pre>
     *     panel.onResizeExpandDown(list);                 // the list reaches the panel's bottom
     *     panel.onResizeExpandDown(top, 40).atLeast(9);   // 40% of the panel's height, never under 9 rows
     * </pre>
     * It leaves room for the siblings after the child (roomAfter, set by the parent): in a stack, a
     * list that fills down stops short of the buttons under it instead of pushing them off the edge.
     */
    public static abstract class ExpandOnResize  implements CanResize {

        Rectangle anchorComponent;
        private Position  targetOffsetPosition;
        private Rectangle targetComponent;
        private CanResize targetToResize;
        private Supplier<Dimension> roomAfter = () -> Dimension.ZERO;
        private int atLeast = 0;

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

        /** the size it grows never goes under this: rows when growing down, columns when growing right (both for down and right) */
        public ExpandOnResize atLeast(int atLeast) {
            this.atLeast = atLeast;
            return this;
        }

        /** the rows below and the columns to the right that the siblings after the child need (default none) */
        public ExpandOnResize leaveRoomAfter(Supplier<Dimension> roomAfter) {
            this.roomAfter = roomAfter;
            return this;
        }

        /** from the child's row to the parent's bottom, less the rows the siblings below it need */
        protected int heightToBottom() {
            return anchorComponent.getHeight() - getTargetComponent().getRow() - getTargetOffsetPosition().getRow() - roomAfter.get().getHeight();
        }

        /** from the child's column to the parent's right edge, less the columns the siblings right of it need */
        protected int widthToRight() {
            return anchorComponent.getWidth() - getTargetComponent().getCol() - getTargetOffsetPosition().getCol() - roomAfter.get().getWidth();
        }

        protected int notUnderAtLeast(int size) {
            return Math.max(atLeast, size);
        }

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
            resizeTarget(new Dimension.Simple(notUnderAtLeast(heightToBottom()), getTargetComponent().getWidth()));
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
            int byPercent = (int)(anchorComponent.getHeight()*((double)percent / 100d));
            resizeTarget(new Dimension.Simple(notUnderAtLeast(Math.min(heightToBottom(), byPercent)), getTargetComponent().getWidth()));
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
            int byPercent = (int)(anchorComponent.getWidth()*((double)percent / 100d));
            resizeTarget(new Dimension.Simple(getTargetComponent().getHeight(), notUnderAtLeast(Math.min(widthToRight(), byPercent))));
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
            resizeTarget(new Dimension.Simple(getTargetComponent().getHeight(), notUnderAtLeast(widthToRight())));
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
            resizeTarget(new Dimension.Simple(notUnderAtLeast(heightToBottom()), notUnderAtLeast(widthToRight())));
        }
        
    }
    
}
