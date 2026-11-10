package mizzen.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.function.Function;

import mizzen.components.composite.Composite;
import mizzen.components.theme.Theme;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.ColorProvider;
import mizzen.events.EventBus;
import mizzen.events.RequestShowCursorEvent;
import mizzen.geom.Rectangle;
import mizzen.geom.position.Position;
import mizzen.textsegments.IScreenLineSegmentsBuffer;
import mizzen.textsegments.ScreenLineSegment;
import mizzen.traverse.FindPathToTarget;
import mizzen.util.Logging;
import mizzen.util.StringUtils;


/**
 * A component is "something" that maintains its own  {@link IScreenLineSegmentsBuffer}.
 * It knows how to draw itself into its IScreenLineSegmentsBuffer 
 * 
 * <p/>
 * 
 * 
 * TODO:  Need to clean up the inheritance hierarchy of Components.. seems like 
 * I've created 2 parallel hierarchies  - ( AbstractComponent & BoundedComponent)  
 * 
 * @author dbalme
 *
 */
public interface Component<T extends Component<?>> 
                                extends IScreenLineSegmentsBuffer.Provider , 
                                        DirtyFlag, 
                                        Enabled,
                                        ColorProvider, 
                                        Rectangle {

    T setColor(Color drawingcolor);

    /**
     * Which of its theme's colours it draws with when it has none of its own - e.g. Theme::inputColor for a text input.
     * <pre>
     *     new TextComponent("No action picked").setThemeColor(Theme::emphasisColor);
     * </pre>
     */
    Function<Theme, Color> getThemeColor();

    T setThemeColor(Function<Theme, Color> colorFromTheme);

    /**
     * The colour it was given; null: none - getColor() is then its theme's. For code that builds or decorates components:
     * a part is handed this, never getColor() (resolved, it would stay fixed when the theme changes); focus shows itself
     * differently on a component with a colour of its own.
     */
    Color getOwnColor();
    
    void clear();
    
    void setParentContainer(ParentContainer composite);
    
    ParentContainer getParentContainer();

    /**
     * For a popup - a dropdown's open list, a menu - the component it belongs to for keyboard focus;
     * null for anything that is not a popup. A popup is drawn on the root panel, so its container never
     * clips it and never grows to fit it - but for focus it is part of its owner (e.g. TAB from the
     * open list carries on after the dropdown). Set with Component.UTIL.setPopupOwner.
     */
    default Component<?> getPopupOwner() {
        return null;
    }
    
    default ParentContainer getParentContainerOrFail() {
        if (getParentContainer() == null) {
            throw new IllegalStateException(HasName.UTIL.getName(this)+" does not have a parentContainer defined!");
        }
        return getParentContainer();
    }
    
    default boolean hide() {
        if (UTIL.isAttachedDirectlyToCanvas(this)) {
            return true;
        }
        return UTIL.hide(this);
    }
    
    default boolean show() {
        if (UTIL.isAttachedDirectlyToCanvas(this)) {
            return true;
        }
        return UTIL.show(this);
    }

    boolean isEnabled();
    void setEnabled(boolean enabled, EventBus eventBus);

    /**
     * Canvas will have set this offset value if component
     * was added directly to the canvas
     */
    Position getCanvasOffset();
    
    /**
     * Canvas will set this offset value if component
     * is added directly to the canvas
     * @param canvasOffset
     */
    void setCanvasOffset(Position canvasOffset);
    
    default Rectangle getPreferredRectangleForLayout() {
        return this;
    }

    public static class UTIL {

        /**
         * Marks popup as belonging to owner (see getPopupOwner): e.g. a dropdown marks its open list, which it
         * draws on the root panel, as its own. Keyboard focus, and a dialog deciding whether a click is on it,
         * then treat the list as part of the dropdown. owner null: popup is no longer a popup.
         * <p/>
         * Fails if owner is popup itself or is inside popup: walking up from popup would come back to popup
         * and never end. The message shows the walk up from owner.
         */
        public static void setPopupOwner(Component<?> popup, Component<?> owner) {
            if (!(popup instanceof AbstractComponent)) {
                throw new IllegalArgumentException("setPopupOwner(): " + HasName.UTIL.getName(popup)
                        + " is not an AbstractComponent - it cannot be a popup");
            }
            if (owner != null && isPartOf(owner, popup)) {
                throw new IllegalArgumentException("setPopupOwner(): " + HasName.UTIL.getName(owner)
                        + " cannot own " + HasName.UTIL.getName(popup) + " - it is that popup or inside it, so the"
                        + " walk up from the popup would come back to it. The walk up from the owner:\n"
                        + walkUntilVisit(owner, popup, 1));
            }
            ((AbstractComponent<?>) popup).popupOwner = owner;
        }

        /**
         * One step up the component tree: the component that this one belongs to.
         * <p/>
         * Usually that is its container (getParentContainer). A popup is the exception: a dropdown's open
         * list, or a menu, is drawn on the root panel so that no container clips it, yet it belongs to the
         * dropdown or menu button that opened it (getPopupOwner). For a popup the step goes to that owner.
         * Returns null at the top of the tree.
         */
        public static Component<?> ownerOrParent(Component<?> component) {
            if (component.getPopupOwner() != null) {
                return component.getPopupOwner();
            }
            Object up = component.getParentContainer();
            return up instanceof Component ? (Component<?>) up : null;
        }

        /**
         * Answers whether a component is inside another component - its container, its container's container,
         * and so on - or is that component itself. It walks up the tree from component, one ownerOrParent step
         * at a time, until it meets whole (true) or reaches the top (false).
         * <p/>
         * Because the walk goes through popup owners, a popup counts as inside the component that opened it.
         * For example, a dialog holds a dropdown, and the dropdown's open list is drawn on the root panel,
         * outside the dialog; the list still counts as part of the dialog:
         * <pre>
         *   item7  ->  the open list  -(popup owner)->  dropdown  ->  form  ->  dialog panel
         * </pre>
         * Used by Dialog to decide whether a mouse click landed on the dialog.
         * <p/>
         * Fails if the links form a cycle (e.g. a popup was made the owner of its own owner): the walk would
         * never end, so it throws an IllegalStateException that lists the walk.
         */
        public static boolean isPartOf(Component<?> component, Component<?> whole) {
            Set<Component<?>> seen = Collections.newSetFromMap(new IdentityHashMap<Component<?>, Boolean>());
            for (Component<?> c = component; c != null; c = ownerOrParent(c)) {
                if (c == whole) {
                    return true;
                }
                if (!seen.add(c)) {
                    throw new IllegalStateException("isPartOf(): the popup-owner / parent links form a cycle - "
                            + HasName.UTIL.getName(c) + " is its own ancestor. The walk up from "
                            + HasName.UTIL.getName(component) + ":\n" + walkUntilVisit(component, c, 2));
                }
            }
            return false;
        }

        // For error messages: the components met walking up from 'from', one per line, stopping once 'until'
        // has been reached 'visits' times - 1 to show the path to it, 2 to show a cycle round to it again:
        //   field -> form (popup: next is its owner) -> field
        private static String walkUntilVisit(Component<?> from, Component<?> until, int visits) {
            StringJoiner walk = new StringJoiner("\n  -> ", "  ", "");
            int met = 0;
            for (Component<?> w = from; w != null; w = ownerOrParent(w)) {
                walk.add(HasName.UTIL.getName(w) + (w.getPopupOwner() != null ? "   (popup: next is its owner)" : ""));
                if (w == until && ++met == visits) {
                    break;
                }
            }
            return walk.toString();
        }

        /**
         * A popup that had focus is closing: give focus back to its owner (e.g. a menu's button
         * after an item was chosen), like a click on the owner. Does nothing if popup has no owner,
         * or the owner cannot take focus.
         */
        public static void returnFocusToPopupOwner(Component<?> popup, EventBus eventBus) {
            Component<?> owner = popup == null ? null : popup.getPopupOwner();
            mizzen.events.focus.Focusable ownerFocusable = mizzen.events.focus.ToFocusable.UTIL.toFocusable(owner);
            if (ownerFocusable != null && eventBus != null) {
                eventBus.raiseEvent(new mizzen.events.focus.RequestFocusEvent(ownerFocusable));
            }
        }

        
                
        public static boolean isDirty(Component<?> ... c) {
            
            if (c == null || c.length==0) {
                return false;
            }
            
            for (Component<?> child : c) {
                if (child.isDirty()) {
                    return true;
                }
            }
            
            return false;
            
        }
        
        /**
         * Hopefully method name says it all?
         * @param child
         */
        public static void markParentDirtyIfChildDirty(Component<?> child) {
            if ( ! child.isDirty()) {
                return;
            }
            
            Component<?> ancestorNotDirty = findNonDirtyAncestor(child);
            
            if (ancestorNotDirty != null) {
                ancestorNotDirty.markAsDirty();
            }

        }
        
        private static Component<?> findNonDirtyAncestor(Component<?> child) {
            
            while (true) {
            
                ParentContainer parentContainer = child.getParentContainer();
                
                if (parentContainer == null) {
                    return null;
                }
                // child is dirty - and has a parent
                if (!(parentContainer instanceof Component<?>)) {
                    return null;
                }

                Component<?> parent = (Component<?>) parentContainer;
                if ( ! parent.isDirty()) {
                    return parent;
                }
                child = parent;
            }
        }

        
        
        static public boolean isAttachedDirectlyToCanvas(Component<?> component) {
            Position canvasOffset = component.getCanvasOffset();
            
            if (canvasOffset != null) { // not null means attached directly to canvas
                return true;
            }
            return false;
        }
        
        static public Component<?> getRootPanelOf(Component<?> component) {
            List<Component<?>> path = pathToCanvas(component);
            
            if (path == null || path.size()<1) {
                return null;
            }

            return path.get(path.size()-1);
        }
        
        static public List<Component<?>> pathToCanvas(Component<?> component) {

            if (component == null) {
                throw new NullPointerException("pathToCanvas() ERROR: component parameter null!");
            }

            if (isAttachedDirectlyToCanvas(component)) {
                return Collections.singletonList(component);
            }

            List<Component<?>> path = new ArrayList<Component<?>>();
            path.add(component);

            // we don't have an offset, so travel up component's ancestors
            ParentContainer parentContainer = component.getParentContainer();

            if (parentContainer != null && parentContainer instanceof Component<?>) {
                path.addAll(pathToCanvas((Component<?>)parentContainer));
            }

            return path;
        }

        static public boolean isAttachedToCanvas(Component<?> component) {
            List<Component<?>> path = pathToCanvas(component);
            if ( isAttachedDirectlyToCanvas(path.get(path.size()-1))) {
                return true;
            }
            return false;
        }

        // useful for debugging and when throwing some exceptions..
        static public String pathToCanvasAsString(Component<?> component) {
            List<Component<?>> path = pathToCanvas(component);
            StringJoiner sj = new StringJoiner(" -> ");
            for (Component<?> c : path) {
                sj.add( HasName.UTIL.getName(c) );
            }
            
            if ( isAttachedDirectlyToCanvas(path.get(path.size()-1))) {
                sj.add("[ CANVAS ]");
            } else {
                sj.add("[ NOT ATTACHED TO CANVAS ! ]");
            }
            
            return sj.toString();
        }

        public static boolean requestShowCursorAtComponentIfAttached(Component<?> component, Position offset, EventBus eventBus) {
            Position canvasOffset = Component.UTIL.getCanvasOffsetFor(component);

            if (canvasOffset == null) {
               return false;
            }
            requestShowCursorAtComponent(component, offset, eventBus); 
            return true;
        }

        public static void requestShowCursorAtComponent(Component<?> component, Position offset, EventBus eventBus) {

            Position canvasOffset = Component.UTIL.getCanvasOffsetFor(component);

            if (canvasOffset == null) {
                if ( isAttachedToCanvas(component)) {
                     throw new IllegalStateException("requestShowCursor(): Offset not calculated");
                } else {
                    throw new IllegalStateException("requestShowCursor(): Offset not calculated\n" +
                                                      "possible reason\n" +
                                                      pathToCanvasAsString(component));
                }
            }

            Position cursorPos = canvasOffset.offsetAdd(offset);
            
            if (cursorPos == null    ||
                cursorPos.getRow()<0 || 
                cursorPos.getCol()<0) {
                    return;
            }

            eventBus.raiseEvent(new RequestShowCursorEvent(component, cursorPos));
        }
        
        public static Position getCanvasOffsetFor(Component<?> component) {

            Position canvasOffset = component.getCanvasOffset();

            if (canvasOffset != null) {
                return  new Position.Simple(canvasOffset).offsetAdd(component.getPostion_TopLeft());
            }

            // we don't have an offset, so travel up component's ancestors and calculate it

            ParentContainer parentContainer = component.getParentContainer();

            if (parentContainer != null && parentContainer instanceof Component<?>) {
                Position parentCanvasOffset = getCanvasOffsetFor((Component<?>)parentContainer);
                if ( parentCanvasOffset == null) {
                    return null;
                }

                Position componentPositionRelativeToParent = parentContainer.getPositionRelativeToParent(component);

                if (componentPositionRelativeToParent == null) {
                    throw new NullPointerException("Unexpected null offset.  Component " + 
                                                   HasName.UTIL.getName(component) + 
                                                   " does not have a defined offset into parent " + 
                                                   HasName.UTIL.getName(parentContainer) );
                }

                Position newOffset = new Position.Simple(parentCanvasOffset).offsetAdd(componentPositionRelativeToParent).offsetAdd(component.getPostion_TopLeft());   
                return newOffset;
            }
            return null;
        }

        public static Position getOffsetFromComponentContainer(Component<?> component, ParentContainer container) {

            if (component == null || container == null) {
                return null;
            }

            if ( ! new FindPathToTarget<Object>().exists(container, component)) {
                return null;
            }

            if (container.equals(component.getParentContainer())) {
                return container.getPositionRelativeToParent(component);
            }

            // parent is different from container - so let's get parentOffset first ..

            ParentContainer parentContainer = component.getParentContainer();

            if (parentContainer != null && parentContainer instanceof Component<?>) {
               Position parentOffset = getOffsetFromComponentContainer((Component<?>)component.getParentContainer(), container);
               if (parentOffset == null) {
                   return null;
               }

               Position newOffset = new Position.Simple(parentOffset).offsetAdd(parentContainer.getPositionRelativeToParent(component)).offsetAdd(component.getPostion_TopLeft());   
               return newOffset;
            }

            return null;

        }

        private static Component<?> __findComponentInHierarchyWithCompositeParent(Component<?> component) {
            ParentContainer parentContainer = component.getParentContainer();
            if (parentContainer == null) {
                return null;
            }
            if ( ! (parentContainer instanceof Composite) ) {
                
                if ( ! (parentContainer instanceof Component)) {
                    return null;
                }
                return __findComponentInHierarchyWithCompositeParent((Component<?>)parentContainer);
            }
            return component;
        }
        
        
        public static boolean bringToFront(Component<?> component) {
            if (component == null || component.getParentContainer() == null) {
                return false;
            }
            
//            Logging.info("bringToFront "+HasName.UTIL.getName(component));
//            if ("vp-of-textComponents".equals(HasName.UTIL.getName(component))) {
//                Logging.info("The culprit!");
//            }
            
            component = __findComponentInHierarchyWithCompositeParent(component);
            if (component == null) {
                return false;
            }
            
            Composite parentContainer = (Composite) component.getParentContainer();
            
            parentContainer.bringToFront(component);
            
            bringToFront(parentContainer);
            
            return true;
        }
        
        
        public static <T extends Component<?>> boolean hide(T component) {

            ParentContainer parent = component.getParentContainer(); 

            if (component.getParentContainer() == null) {
                return false; // false not visible
            }

            if (parent instanceof ChildComponentManager) {
                ChildComponentManager<?> parentComponentManager = (ChildComponentManager<?>) parent;
                parentComponentManager.hide(component);
                component.markAsDirty();
                return false;  // false means hidden
            }
            
            return true; // has a parent - but parent cannot hide

        }

        public static <T extends Component<?>> boolean show(T component) {

            ParentContainer parent = component.getParentContainer();

            if (component.getParentContainer() == null) {
                return false;
            }

            if (parent instanceof ChildComponentManager) {
                ChildComponentManager<?> parentComponentManager = (ChildComponentManager<?>) parent;
                parentComponentManager.show(component);
                component.markAsDirty();
                return true;
            }
            
            return true; // has a parent, but parent cannot show/hide child; so it must be visible

        }
        
        
        /**
         * When true, component draw methods will check that 
         * built segments do not overlap and segments stay within
         * the bounds of the components defined bounds.
         * Only turn this flag on for debug and / or test sessions.
         * It's expensive to leave it on.
         */
        public static boolean COMPONENT_SEGMENTS_INTEGRITY_CHECK_FLAG = false; 
        
        /** 
         *  WARNING WARNING WARNING - very slow .. use only for testing
         * 
         *  Asserts that the component is truly the size that it claims by traversing all
         *  segments and building a bounding rectangle from it.
         *  Asserts that the component has no overlapping segments
         *  
         * @param component
         */
        public static void assertComponentSegmentsIntegrity(Component<?> component) {
            if (!COMPONENT_SEGMENTS_INTEGRITY_CHECK_FLAG) {
                return;
            }
            ScreenLineSegment.UTIL.assertNoOverlapsInSegments(component.getSegmentsBuffer().getSegments());
            assertComponentBoundsItsSegments(component);
        }

        /** 
         * Same as {@link#assertComponentSegmentsIntegrity(Component)} except segments 
         * do not have be to attached to a component
         * 
         * @param bounds
         * @param segmentsTree
         */
        public static void assertSegmentsIntegrity(Rectangle bounds, TreeSet<ScreenLineSegment> segmentsTree) {
            if (!COMPONENT_SEGMENTS_INTEGRITY_CHECK_FLAG) {
                return;
            }
            ScreenLineSegment.UTIL.assertNoOverlapsInSegments(segmentsTree);
            assertSegmentsWithinBounds(bounds, segmentsTree);
        }

        private static void assertComponentBoundsItsSegments(Component<?> component) {
            Rectangle calcRec = Rectangle.UTIL.calcDimensionsWith(component.getSegmentsBuffer().getSegments());
            if (calcRec.isSameRectangle(Rectangle.ZERO)) {
                return;
            }
            if (!component.contains(calcRec)) {
                // find offending segments
                StringJoiner sj = new StringJoiner("\n");
                component.getSegmentsBuffer().getSegments().stream().filter( s -> !component.contains(s)).forEach(s->sj.add("    -  "+String.valueOf(s)));

                String errorMSG = String.format("Error in %s:\n  calculated segments rect of componment:          %s\n  does not fit inside defined component bounds of: %s\n  # segments : %d\n  offending segments:\n%s\n", 
                                                StringUtils.truncate(HasName.UTIL.getName(component),20), 
                                                String.valueOf(calcRec),
                                                String.valueOf(new Rectangle.Simple(component)),
                                                component.getSegmentsBuffer().getSegments().size(),
                                                sj.toString());

                RuntimeException e = new IllegalStateException(errorMSG);

                throw e;
            }
        }

        private static void assertSegmentsWithinBounds(Rectangle bounds, TreeSet<ScreenLineSegment> segmentsTree) {
            Rectangle calcRec = Rectangle.UTIL.calcDimensionsWith(segmentsTree);
            if (calcRec.isSameRectangle(Rectangle.ZERO)) {
                return;
            }
            if (!bounds.contains(calcRec)) {
                // find offending segments
                StringJoiner sj = new StringJoiner("\n");
                segmentsTree.stream().filter( s -> !bounds.contains(s)).forEach(s->sj.add("    -  "+String.valueOf(s)));

                String errorMSG = String.format("Error in assertComponentBoundsItsSegments:\n  calculated segments rect of segments:          %s\n  does not fit inside defined desired rectangle of: %s\n  # segments : %d\n  offending segments:\n%s\n", 
                                                String.valueOf(calcRec),
                                                String.valueOf(new Rectangle.Simple(bounds)),
                                                segmentsTree.size(),
                                                sj.toString());

                RuntimeException e = new IllegalStateException(errorMSG);

                throw e;
            }
        }
        
    }

}
