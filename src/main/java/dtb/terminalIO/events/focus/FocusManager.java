package dtb.terminalIO.events.focus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import dtb.terminalIO.components.AbstractWrappedComponentWithFocusManager;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.ComponentWithFocusManager;
import dtb.terminalIO.components.HasName;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.TerminalEvent.IS_Event_TYPE;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.traverse.FindPathToTarget;
import dtb.terminalIO.traverse.HasChildren;
import dtb.terminalIO.traverse.HasChildrenAdapter;
import dtb.terminalIO.traverse.Traverser;
import dtb.terminalIO.util.Logging;
import dtb.terminalIO.util.StringUtils;
import dtb.terminalIO.util.TerminalDimensions;

/**
 * A FocusManager is basically a 'Focusable' eventHandler that routes 
 * any incoming events that it receives to a target 'child' Focusable.
 * 
 * <p/>
 * 
 * The idea is to be able to decide that we have a current EventHandler ( usually a Component that accepts input)
 * that should receive the current ( usually keyboard ) events.
 * 
 * <p/>
 * 
 * This is useful, for example, when many input Components (example a form) are rendered on the canvas and a specific component
 * is processing all keyboard events.
 * 
 */
public interface FocusManager extends Focusable {
    
    /**
     * Sets current event handler focus to focusableChild
     */
    public void setChildAsCurrentFocus(Focusable focusableChild, EventBus eventBus);

    /**
     * @return child focusable that has current focus. null if none defined.
     */
    public Focusable getCurrentChildFocus();
    
    public Collection<Focusable> getChildFocusables();

    /**
     * What kind of focus manager this is - decides how it answers enter / advance:
     * GROUP (fields TAB through), WIDGET (one stop, e.g. list box), TRAP (modal, focus can't leave).
     * Wrapper components ask their inner manager; anything else is a GROUP.
     */
    default FocusPolicy getFocusPolicy() {
        FocusManager delegate = UTIL.providedFocusManager(this);
        return delegate != null ? delegate.getFocusPolicy() : FocusPolicy.GROUP;
    }

    /**
     * Can TAB / SHIFT-TAB land in this focus manager? false = skip it and everything in it (like HTML
     * tabindex=-1); its parts can still be clicked or reached with their hot keys. Example: the
     * scroll buttons (^ v PgUp PgDn) at the bottom of a scrollable panel.
     * Wrapper components ask their inner manager; anything else is a TAB stop.
     */
    default boolean isTabStop() {
        FocusManager delegate = UTIL.providedFocusManager(this);
        return delegate != null ? delegate.isTabStop() : true;
    }

    /**
     * Is key one of this focus manager's navigation keys? Returns FORWARD for its "next" key
     * (usually TAB), BACKWARD for its "previous" key (usually SHIFT-TAB), null for any other key.
     * Wrapper components ask their inner manager.
     */
    default FocusDirection navDirectionFor(KeyStroke key) {
        FocusManager delegate = UTIL.providedFocusManager(this);
        return delegate != null ? delegate.navDirectionFor(key) : null;
    }

    /**
     * The extra key handlers this focus manager's component registered for itself, on top of focus
     * navigation - e.g. a dropdown's "DOWN opens my list". For a WIDGET, keyboard navigation offers
     * its nav keys to these first, and only moves focus if they don't want the key.
     * null = none. Wrapper components ask their inner manager (and add their own, see
     * ComponentWithFocusManager).
     */
    default ConditionalEventHandler getOwnEventHandler() {
        FocusManager delegate = UTIL.providedFocusManager(this);
        return delegate != null ? delegate.getOwnEventHandler() : null;
    }

    /**
     * For a WIDGET: does moving past its last child go back to its first (and before the first to
     * the last)? E.g. a tab strip: RIGHT on the last tab shows the first. false (default): focus
     * stays at the end. Wrapper components ask their inner manager.
     */
    default boolean wrapsAround() {
        FocusManager delegate = UTIL.providedFocusManager(this);
        return delegate != null && delegate.wrapsAround();
    }

    @Override
    default Focusable enter(FocusDirection dir) {
        return UTIL.enter(this, dir);
    }

    @Override
    default Navigation advance(Focusable focusedLeaf, FocusDirection dir) {
        return UTIL.advance(this, focusedLeaf, dir);
    }

    /**
     * Adapts a FocusManager to look like a HasChildren object.
     * - useful when we want to use our generic traverse methods to
     * - traverse the Focusable's within a FocusManager 
     */
    static class FocusManagerToHasChildren implements HasChildren<Focusable> {

        FocusManager focusManager;

        FocusManagerToHasChildren(FocusManager focusManager) {
            this.focusManager = focusManager;
        }

        @Override
        public Collection<? extends Focusable> getChildren() {
            return this.focusManager.getChildFocusables();
        }
    }

    static class FocusManagerHasChildrenAdapter implements HasChildrenAdapter<Focusable> {

        @Override
        public HasChildren<? extends Focusable> asHasChildren(Focusable parent) {
            if (parent instanceof FocusManager) {
                return new FocusManagerToHasChildren((FocusManager)parent);
            }
            return new HasChildren.Empty<Focusable>();
        }

    }


    /**
     * Utility focus methods helpful when .. 
     * 
     *   setting or unSetting focus
     *   building focusManagers from components
     *   
     *
     */
    public static class UTIL {

        /**
         * Wrapper components (ComponentWithFocusManager, CompositeWithFocusManager, ...) are focus
         * managers that hand the real work to an inner DefaultFocusManager. Returns that inner
         * manager, or null if fm is not such a wrapper.
         */
        static FocusManager providedFocusManager(FocusManager fm) {
            if (fm instanceof FocusManagerProvider) {
                FocusManager provided = ((FocusManagerProvider) fm).getFocusManager();
                if (provided != null && provided != fm) {
                    return provided;
                }
            }
            return null;
        }

        /**
         * Is focusable this node, or somewhere inside it? E.g. "is field C inside group [B,C]?".
         * Answered by walking up from focusable through its parent focus managers.
         *
         * Note: a wrapper and its inner manager count as the same node, because a child can
         * point at either one as its parent (ComponentWithFocusManager adds its children to the
         * inner manager before it registers itself as the "actual" parent).
         */
        public static boolean containsFocusable(Focusable node, Focusable focusable) {
            for (Focusable ancestorOrSelf = focusable; ancestorOrSelf != null;
                           ancestorOrSelf = ancestorOrSelf.getParentFocusManager()) {
                if (isSameNode(node, ancestorOrSelf)) {
                    return true;
                }
            }
            return false;
        }

        /** a and b are the same node in the focus tree - also a wrapper component and its inner manager */
        static boolean isSameNode(Focusable a, Focusable b) {
            if (a == b) {
                return true;
            }
            if (a instanceof FocusManager && providedFocusManager((FocusManager) a) == b) {
                return true;
            }
            return b instanceof FocusManager && providedFocusManager((FocusManager) b) == a;
        }

        /**
         * Move focus from wherever it is to target (a component somewhere below root). Used by mouse
         * clicks / RequestFocusEvent and by keyboard navigation.
         *
         * Every focus manager on the way is told through setChildAsCurrentFocus, so the usual
         * notifications fire: unFocus(new) to what loses focus, focus(old) to what gains it (hilite,
         * bring-to-front, dropdown hand-off ... depend on these). It works in two passes so that only
         * the right components are notified:
         *   1. Focus managers on the path that don't have focus yet, deepest first, just record which child
         *      to use (no notifications - they don't have focus).
         *   2. Then top-down: the focus managers that already have focus pass it on, and focus() travels
         *      straight down the recorded path to target.
         * Without pass 1, a focus manager receiving focus would first hand it to the child it remembered
         * (or its first child) - that child would briefly get focus (flicker; a window wrongly brought
         * to front).
         *
         * reassertFocus: first re-send setFocus(true) down the path that already has focus, so windows
         * that stay focused re-hilite / come back to front (what mouse clicks have always done).
         * Skipped when the root has no current child - there is nothing to re-assert, and doing it
         * would make the root pick its first child.
         *
         * Returns false if target is not inside root.
         */
        public static boolean moveFocus(FocusManager root, Focusable target, EventBus eventBus, boolean reassertFocus) {
            List<Focusable> path = findTargetAsPath(root, target);     // root, ..., target
            if (path == null || path.size() < 2 || path.get(0) != root) {
                return false;
            }
            DefaultFocusManager rootManager = rootDefaultFocusManager(root);
            Focusable previous = rootManager != null && root.hasFocus() ? findCurrentFocus(root) : null;
            if (reassertFocus && root.hasFocus() && root.getCurrentChildFocus() != null) {
                root.setFocus(true, eventBus);
            }
            // pass 1: focus managers without focus record their child, deepest first
            for (int i = path.size() - 2; i >= 0; i--) {
                FocusManager focusManager = (FocusManager) path.get(i);
                if (focusManager.hasFocus()) {
                    break;
                }
                focusManager.setChildAsCurrentFocus(path.get(i + 1), eventBus);
            }
            if (!root.hasFocus()) {
                root.setFocus(true, eventBus);       // nothing had focus: give it, down the recorded path
            }
            // pass 2: top-down; focus managers that already have focus pass it on
            for (int i = 0; i < path.size() - 1; i++) {
                ((FocusManager) path.get(i)).setChildAsCurrentFocus(path.get(i + 1), eventBus);
            }
            // single owner: record the newly focused component, and make sure the previous one really
            // let go - if it still claims focus (missed its unFocus), switch it off
            if (rootManager != null) {
                rootManager.recordFocusedComponent(followCurrentChildren(root));
                Focusable now = rootManager.getFocusedComponent();
                if (previous != null && previous != now && previous.hasFocus() && !containsFocusable(previous, now)) {
                    previous.setFocus(false, eventBus);
                }
            }
            return true;
        }

        /**
         * Focus is moving INTO this focus manager (e.g. TAB from the field before it).
         * Returns the component inside it that focus should land on, without changing anything:
         *   GROUP  - first component when moving forward, last one when moving backward
         *   WIDGET - the child it had last time (else the first)
         *   TRAP   - like GROUP
         * Returns null if nothing in here can take focus (empty / all disabled / not a TAB stop) -
         * skip this focus manager.
         */
        public static Focusable enter(FocusManager focusManager, FocusDirection dir) {
            if (!focusManager.isEnabled() || !focusManager.isTabStop()) {
                return null;
            }
            List<Focusable> children = new ArrayList<Focusable>(focusManager.getChildFocusables());
            FocusDirection order = dir;
            if (focusManager.getFocusPolicy() == FocusPolicy.WIDGET) {
                // resume the remembered child; fall back to the first
                Focusable remembered = focusManager.getCurrentChildFocus();
                if (remembered != null && children.contains(remembered)) {
                    Focusable target = remembered.enter(dir);
                    if (target != null) {
                        return target;
                    }
                }
                order = FocusDirection.FORWARD;
            }
            for (int i = 0; i < children.size(); i++) {
                Focusable child = children.get(order == FocusDirection.FORWARD ? i : children.size() - 1 - i);
                Focusable target = child.enter(dir);
                if (target != null) {
                    return target;
                }
            }
            return null;    // nothing focusable here: skip me
        }

        /**
         * A nav key (e.g. TAB) was pressed while focusedLeaf, somewhere inside this focus manager, has
         * focus. Decides where focus goes next, without changing anything:
         *   MOVED(target) - next / previous child in here (entered via its own enter())
         *   DONE          - no more children that way; let the parent focus manager decide
         *   STAY          - WIDGET at its first / last child, or a TRAP with nowhere else to go:
         *                   keep focus where it is
         * A TRAP, the root focus manager (no parent), or a WIDGET that wrapsAround(), wraps around
         * instead.
         */
        public static Navigation advance(FocusManager focusManager, Focusable focusedLeaf, FocusDirection dir) {
            List<Focusable> children = new ArrayList<Focusable>(focusManager.getChildFocusables());
            int from = -1;
            for (int i = 0; i < children.size(); i++) {
                if (containsFocusable(children.get(i), focusedLeaf)) {
                    from = i;
                    break;
                }
            }
            FocusPolicy policy = focusManager.getFocusPolicy();
            // a root focus manager (no parent) wraps around, like a trap
            boolean wraps = policy == FocusPolicy.TRAP || focusManager.getParentFocusManager() == null
                         || (policy == FocusPolicy.WIDGET && focusManager.wrapsAround());
            int step = dir == FocusDirection.FORWARD ? 1 : -1;
            int j = from;
            for (int n = 0; n < children.size(); n++) {
                j += step;
                if (j < 0 || j >= children.size()) {
                    if (!wraps) {
                        return policy == FocusPolicy.GROUP ? Navigation.DONE : Navigation.STAY;
                    }
                    j = (j + children.size()) % children.size();
                }
                // when wrapping, the last step may come back to the child we started in:
                // re-entering it from its first/last focusable is legitimate (no dead end)
                Focusable target = children.get(j).enter(dir);
                if (target != null && target != focusedLeaf) {
                    return Navigation.movedTo(target);
                }
            }
            // focus never leaves a WIDGET through its own keys, nor a TRAP at all
            return policy == FocusPolicy.GROUP ? Navigation.DONE : Navigation.STAY;
        }

        public static boolean isAncestorOf(Focusable maybeAncestor, Focusable maybeDescendant) {
            if (maybeAncestor == null || maybeDescendant == null || (!(maybeAncestor instanceof FocusManager))) {
                return false;
            }
            if (maybeAncestor == maybeDescendant.getParentFocusManager()) {
                return true;
            }
            return isAncestorOf(maybeAncestor, maybeDescendant.getParentFocusManager());
        }
        
        
        /**
         * utility method to unfocus old focusable and focus new focusable and then optionally 
         * run a function to perform "whatever" desired action. Usually the ("whatever" desired) action is 
         * to update the screen (refresh the canvas).
         * 
         * @param focusableChild
         * @param oldFocusableChild
         * @param eventBus
         * @param onFocusChanged
         */
        public static void setNewFocusable(Focusable focusableChild, Focusable oldFocusableChild, EventBus eventBus, BiConsumer<Focusable, EventBus> onFocusChanged) {
            

            // guard against doing anything if we are dealing with a disabled focusableChild
            if (focusableChild != null && !focusableChild.isEnabled()) {
                Logging.info(HasName.UTIL.getName(focusableChild)+ " focus request denied because focus object is disabled");
                return;
            }

            // unfocus the previous (old)
            if (oldFocusableChild != null && oldFocusableChild != focusableChild && ! isAncestorOf(oldFocusableChild, focusableChild)) {
                Logging.info(HasName.UTIL.getName(oldFocusableChild)+ " unfocus  will change to: " +  HasName.UTIL.getName(focusableChild, "NONE-SELECTED"));
                oldFocusableChild.unFocus(focusableChild, eventBus);  //setFocus(false, eventBus);
            }
            
            // focus the new
            if ( focusableChild != null && !focusableChild.hasFocus()) {    
                Logging.info(HasName.UTIL.getName(focusableChild)+ " focus  from previous: " + HasName.UTIL.getName(oldFocusableChild, "NONE-SELECTED"));
                focusableChild.focus(oldFocusableChild, eventBus);
            }
            
            // run action ( typically to update the canvas )
            if (onFocusChanged != null) {
                onFocusChanged.accept(focusableChild, eventBus);
            }
        }

        /**
         * Which component has keyboard focus inside focusManager? null if focusManager doesn't have focus.
         * For the ROOT focus manager this is its record of THE focused component (single owner) - as long
         * as that component is still attached. Otherwise: follow each focus manager's current child downwards.
         */
        public static Focusable findCurrentFocus(FocusManager focusManager) {
            if ( ! focusManager.hasFocus()) {
                return null;
            }
            if (focusManager.getParentFocusManager() == null) {
                DefaultFocusManager root = rootDefaultFocusManager(focusManager);
                Focusable owner = root != null ? root.getFocusedComponent() : null;
                if (owner != null && containsFocusable(focusManager, owner)) {
                    return owner;
                }
            }
            return followCurrentChildren(focusManager);
        }

        // focus as given by each focus manager's current child, from focusManager downwards
        private static Focusable followCurrentChildren(FocusManager focusManager) {
            if ( ! focusManager.hasFocus()) {
                return null;
            }
            
            Focusable focusableChild = focusManager.getCurrentChildFocus();
            
            if (focusableChild == null) {
                return focusManager;
            }
            
            if (!(focusableChild instanceof FocusManager)) {
                return focusableChild;
            }
            
            return followCurrentChildren((FocusManager)focusableChild);
        }

        /** the ROOT (no parent) focus manager above anyManager, as its DefaultFocusManager; null if none */
        public static DefaultFocusManager rootDefaultFocusManager(FocusManager anyManager) {
            FocusManager top = anyManager;
            while (top.getParentFocusManager() != null) {
                top = top.getParentFocusManager();
            }
            DefaultFocusManager[] found = { null };
            DefaultFocusManager.applyIfDefaultFocusManager(top, root -> found[0] = root);
            return found[0];
        }

        /** after focus moved inside anyManager: the root records the focused component again */
        static void recordFocusFromCurrentChildren(FocusManager anyManager) {
            DefaultFocusManager root = rootDefaultFocusManager(anyManager);
            if (root != null && root.hasFocus()) {
                root.recordFocusedComponent(followCurrentChildren(root));
            }
        }
        
        public static Focusable focusableFromComponent(Component<?> component) {
            if (component instanceof Focusable) {
                return (Focusable) component;
            }
            if (component instanceof ToFocusable) {
                return ((ToFocusable) component).asFocusable();
            }
            return  null;
        }

        /**
         * Traverse this object and all its child (and grand-child .. ) objects looking for
         * focusTarget.  
         * 
         * If focusTarget found,  then it returns path (a list of objects)
         * from focusManager to focusTarget
         * 
         */
        public static List<Focusable> findTargetAsPath(FocusManager focusManager, Focusable focusTarget) {

            FindPathToTarget<Focusable> finder = new FindPathToTarget<Focusable>(new FocusManagerHasChildrenAdapter());
            
            // look for path via tree traversal technique ( traditional method )
            List<Focusable> pathToTarget = finder.findPath(focusManager, focusTarget);

            if (pathToTarget != null && pathToTarget.size() > 0) {
                return pathToTarget;
            }

            // look for path by looping up from target to root
            pathToTarget = finder.findPath(focusManager, focusTarget, new HasParentFocusableAdapter());
            if (pathToTarget != null && pathToTarget.size() > 0) {
                return pathToTarget;
            }

            return null;
        }
        
        public static FocusManager getRootFocusManager(Focusable focusable) {
            List<FocusManager> path = pathToRootFocusManager(focusable);
            if (path.size()>0) {
                return path.get(path.size()-1);
            }
            return null;
        }

        public static List<FocusManager> pathToRootFocusManager(Focusable focusable) {
            if (focusable == null) {
                throw new NullPointerException("pathToRootFocusManager() ERROR: focusable parameter null!");
            }
            if (focusable.getParentFocusManager() == null) {
                return Collections.emptyList();
            }
            List<FocusManager> path = new ArrayList<FocusManager>();
            FocusManager  focusManager =  focusable.getParentFocusManager();
            path.add(focusManager);
            
            while (focusManager.getParentFocusManager() != null) {
                focusManager = focusManager.getParentFocusManager();
                path.add(focusManager);
            }
            return path;
        }


        private static void addFocusableChildren(List<Focusable> focusableContainer, Object component) {

            if (ToFocusable.UTIL.isFocusable(component)) {
                focusableContainer.add( ToFocusable.UTIL.toFocusable(component) );
                return;
            }

            // component is not a focusable - maybe it has children and maybe some 
            // of them are focusable's - let's recursively find out add them if yes
            addFocusableFromComponentChildren(focusableContainer, component);
        }

        private static void addFocusableFromComponentChildren(List<Focusable> focusableContainer, Object component) {

            if (component instanceof HasChildren) {
                for (Object o : ((HasChildren<?>)component).getChildren()) {
                    addFocusableChildren(focusableContainer, o);
                }
            }
        }


        /**
         * Given component ( which could represent a graph of components ), we will build
         * a focusManager that manages the graph of components within Component
         * @return
         */
        public static FocusManager buildFocusManagerFromComponent(Object component, EventBus eventBus) {
            return buildFocusManagerFromComponent(component, eventBus, ()-> new DefaultFocusManager().setNameFromOwner(component));
        }

        public static FocusManager buildFocusManagerFromComponent(Object component, 
                                                                  EventBus eventBus, 
                                                                  Supplier<DefaultFocusManager> defaultFocusManagerBuilder ) {
            if (component instanceof FocusManager) {
                return (FocusManager) component;
            }
            List<Focusable> focusables = new ArrayList<Focusable>();
            addFocusableChildren(focusables, component);
            if (focusables.size()==0) {
                return null;
            }
//            if (focusables.size()==1 && focusables.get(0) instanceof FocusManager) {
//                return (FocusManager) focusables.get(0);
//            }

            DefaultFocusManager focusManager = defaultFocusManagerBuilder.get();
            for (Focusable focusable : focusables) {
                focusManager.addFocusable(focusable, eventBus);
            }

            return focusManager;
        }
        

        public static FocusManager buildFocusManagerFromComponentChildren(Object component, 
                                                                          EventBus eventBus) {
            return buildFocusManagerFromComponentChildren(component, eventBus, ()-> new DefaultFocusManager().setNameFromOwner(component));
        }

        public static FocusManager buildFocusManagerFromComponentChildren(Object component, 
                                                                          EventBus eventBus, 
                                                                          Supplier<DefaultFocusManager> defaultFocusManagerBuilder ) {

            List<Focusable> focusables = new ArrayList<Focusable>();
            addFocusableFromComponentChildren(focusables, component);

            if (focusables.size()==0) {
                return null;
            }

            if (focusables.size()==1 && focusables.get(0) instanceof FocusManager) {
                return (FocusManager) focusables.get(0);
            }

            DefaultFocusManager focusManager = defaultFocusManagerBuilder.get();
            for (Focusable focusable : focusables) {
                focusManager.addFocusable(focusable, eventBus);
            }

            return focusManager;
        }

        public static Collection<Focusable> focusablesFromComponent(Object component) {
                if (component instanceof Focusable) {
                    return Collections.singleton((Focusable)component);
                }
                if (component instanceof ToFocusable) {
                    return Collections.singleton(((ToFocusable)component).asFocusable());
                }
            
                List<Focusable> focusables = new ArrayList<Focusable>();
                addFocusableFromComponentChildren(focusables, component);
                
                return focusables;

        }

        public static List<Focusable> getFocusablesFromComponent(Object component) {
            List<Focusable> focusables = new ArrayList<Focusable>();
            addFocusableChildren(focusables, component);
            return focusables;
        }
        

        public static Focusable findOrBuildFocusableFromComponent(Object component, EventBus eventBus) {
            return findOrBuildFocusableFromComponent(component, eventBus, ()-> new DefaultFocusManager().setNameFromOwner(component));
        }

        public static Focusable findOrBuildFocusableFromComponent(Object component, EventBus eventBus, Supplier<DefaultFocusManager> defaultFocusManagerBuilder) {
            
            if ( ToFocusable.UTIL.isFocusable(component) ) {
                return ToFocusable.UTIL.toFocusable(component);
            }

            List<Focusable> focusables = new ArrayList<Focusable>();
            addFocusableChildren(focusables, component);
            if (focusables.size()==0) {
                return null; // <-- why am I doing this .. very annoying!!!
            }
            if (focusables.size()==1) {
                return (Focusable) focusables.get(0);
            }

            DefaultFocusManager focusManager = defaultFocusManagerBuilder.get();
            for (Focusable focusable : focusables) {
                focusManager.addFocusable(focusable, eventBus);
            }
            return focusManager;
        }

        public static void verifyPathsToFocusables(FocusManager focusManager, Component<?> component) {


            // this is a hack.. because ComponentWithFocusManager and AbstractWrappedComponentWithFocusManager
            //                  do not play nicely with this method..  
            Predicate<Component<?>> skipProxiedManagers = (c)-> {
                if (c.getParentContainer() == null) {
                    return true;
                }
                if (c.getParentContainer() instanceof ComponentWithFocusManager ) {
                    return false;
                }
                if (c.getParentContainer() instanceof AbstractWrappedComponentWithFocusManager ) {
                    return false;
                }
                return true;
            };

            skipProxiedManagers = (c)-> true;

            List<Focusable> focusables = Traverser.asList(component, (c)-> ToFocusable.UTIL.isFocusable(c) )
                                                  .stream()
                                                  .filter(skipProxiedManagers)
                                                  .map((c)-> ToFocusable.UTIL.toFocusable(c))
                                                  .collect(Collectors.toList());

            if (focusables.isEmpty()) {
                return;
            }

            List<String> failedOn = new ArrayList<String>();

            for (Focusable f : focusables) {
                List<Focusable> paths = findTargetAsPath(focusManager, f);
                if (paths == null || paths.size()==0) {
                    if (f instanceof Component<?>) {
                        paths = findTargetAsPath(focusManager, f);
                        String fAsComponentPathStr = Component.UTIL.pathToCanvasAsString((Component<?>) f);
                        failedOn.add(fAsComponentPathStr);
                    } else {
                        failedOn.add("failed on " + f);
                    }
                }
            }

            if (failedOn.size()>0) {
                String divider = StringUtils.repeat('-', (int)Math.max((int)TerminalDimensions.create().getWidth()/1.5,20));
                StringJoiner sj = new StringJoiner("\n"+divider+"\n");
                failedOn.forEach((s)-> sj.add(s));
                sj.add("--- Reachable Focusables -------");

                sj.add(traverseAllFocusablesToString(focusManager));

                throw new IllegalStateException("FocusManager.verifyPathsToFocusables(): Failed to find focus path for:\n"+sj.toString());
            }

        }

        public static String traverseAllFocusablesToString(Focusable focusable)  { 
            StringBuilder sb = new StringBuilder();

            Traverser<Focusable> traverser = new Traverser<Focusable>(new FocusManagerHasChildrenAdapter());
            int depth[] = new int[] {0};
            traverser.traverse(focusable, (f)->{
                                                      if (depth[0]>0) {
                                                          sb.append(StringUtils.repeat("  ", depth[0]));
                                                      }
                                                      sb.append("   -----> ").append(HasName.UTIL.getName(f)).append("\n");
                                                      depth[0]++;
                                                      return Traverser.CONTINUE_TRAVERSE;
                                                  },
                                              (f)->{
                                                  depth[0]--;
                                                  }) ;
            return sb.toString();
        }

        /**
         * Builds a focus event handler for a FocusManager to handle
         * 
         * <p/>
         * 
         * TerminalEvent.TYPE.REQUEST_FOCUS and TerminalEvent.TYPE.REQUEST_RELEASE_FOCUS event
         * types.
         * 
         * <p/>
         * 
         * If any of a FocusManager's children (components) make a request for focus (TYPE.REQUEST_FOCUS)
         * or make a request to unfocus (TYPE.REQUEST_RELEASE_FOCUS), this event handler
         * will look for where the child components live within the hierarchy of the focus
         * Manager and sets the appropriate focus or unfocus state of the child and its parent and
         * ancestors.
         * 
         */
        public static ConditionalEventHandler buildRequestFocusEventHandler(FocusManager focusManager) {

            IS_Event_TYPE IS_REQUEST_FOCUS         = new IS_Event_TYPE(TerminalEvent.TYPE.REQUEST_FOCUS);
            IS_Event_TYPE IS_REQUEST_RELEASE_FOCUS = new IS_Event_TYPE(TerminalEvent.TYPE.REQUEST_RELEASE_FOCUS);

            Predicate<TerminalEvent<?>> canHandlePredicate = new Predicate<TerminalEvent<?>>() {

                @Override
                public boolean test(TerminalEvent<?> te) {
                    if ( ! IS_REQUEST_FOCUS.test(te) && ! IS_REQUEST_RELEASE_FOCUS.test(te)) {
                        return false;
                    }

                    @SuppressWarnings("unchecked")
                    TerminalEvent<Focusable> fe = (TerminalEvent<Focusable>)te;

                    Focusable focusTarget = fe.getEventData();

                    // find a path from focusManager to the child
                    List<Focusable> pathToTarget = findTargetAsPath(focusManager, focusTarget);
                    
                    // TODO: is it possible to implement a child find?
                    if (pathToTarget == null || pathToTarget.size() == 0) {
                        // no path?  then cannot handle this event
                        return false;
                    }
                    // looks like we can handle this event
                    // NOTE:  what a waste .. I'm throwing result away
                    //        to be recalc'd in the handle method - oh well.
                    //        I cannot save state - reentrant issues otherwise.
                    
                    
                    // Logging.info("REQUEST_FOCUS - handled");
                    
                    return true;
                }
            };

            ConditionalEventHandler.Impl requestFocusHandler = new ConditionalEventHandler.Impl(canHandlePredicate, (te) -> {

                @SuppressWarnings("unchecked")
                TerminalEvent<Focusable> fe = (TerminalEvent<Focusable>)te;

                Focusable focusTarget = fe.getEventData();

                List<Focusable> pathToTarget = findTargetAsPath(focusManager, focusTarget);

                if (pathToTarget == null) {
                    // we should not get here because canHandlePredicate
                    // should have dealt with this already..
                    return;
                }

                // at this point target is within our tree of focusable's
                // so time to traverse the path in order to "select" (focus) it

                if ( ! focusManager.hasFocus()) {
                    // FIXME: dont know how to deal with this scenario yet
                } 

                // sanity check
                if (pathToTarget.get(0) != focusManager) {
                    throw new IllegalStateException(": pathToTarget does not start with root focusManager("+HasName.UTIL.getName(focusManager)+")");
                }

                boolean hasGoodPath = pathToTarget.size()>1;
                
                if (hasGoodPath) {
                    if (IS_REQUEST_FOCUS.test(te)) {
                        // reassertFocus=true: re-sends setFocus(true) down the focused path first, so
                        // ancestors that stay focused re-hilite / bringToFront (z-order may have changed)
                        moveFocus(focusManager, focusTarget, te.getEventBus(), true);
                    } else {
                        // un focus (release focus)
                        int lastIndex = pathToTarget.size()-1; 
                        if (pathToTarget.get(lastIndex) == focusTarget) {
                            if (lastIndex == 0 || !(pathToTarget.get(lastIndex-1) instanceof FocusManager)) {
                                focusTarget.setFocus(false, te.getEventBus());
                            } else {
                                FocusManager targetsParentFocusManager = (FocusManager)pathToTarget.get(lastIndex-1);
                                if (targetsParentFocusManager.getCurrentChildFocus() == focusTarget) {
//                                    targetsParentFocusManager.setChildAsCurrentFocus(null, te.getEventBus());
//                                } else {
//                                    // odd that we get here ..
                                    focusTarget.setFocus(false, te.getEventBus());
                                }
                            }
                        }
                    }
                }
                
            });
            
            requestFocusHandler.setName("requestFocusHandler");
            return requestFocusHandler;

        }
    }

}
