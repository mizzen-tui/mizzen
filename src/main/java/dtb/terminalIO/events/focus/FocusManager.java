package dtb.terminalIO.events.focus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
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
import dtb.terminalIO.events.focus.ToFocusable.FocusableComponentPair;
import dtb.terminalIO.traverse.FindPathToTarget;
import dtb.terminalIO.traverse.HasChildren;
import dtb.terminalIO.traverse.HasChildrenAdapter;
import dtb.terminalIO.traverse.Traverser;
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
         * utility method to unfocus old focusable and focus new focusable and then optionally 
         * run a function to perform "whatever" desired action. Usually the action is to update the
         * screen (refresh the canvas).
         * 
         * @param focusableChild
         * @param oldFocusableChild
         * @param eventBus
         * @param onFocusChanged
         */
        public static void setNewFocusable(Focusable focusableChild, Focusable oldFocusableChild, EventBus eventBus, BiConsumer<Focusable, EventBus> onFocusChanged) {
            
            // unfocus the previous (old)
            if (oldFocusableChild != null) {
                oldFocusableChild.setFocus(false, eventBus);
            }
            
            // focus the new
            if ( focusableChild != null) {    
                focusableChild.setFocus(true, eventBus);
            }
            
            // run action ( typically to update the canvas )
            if (onFocusChanged != null) {
                onFocusChanged.accept(focusableChild, eventBus);
            }
        }

        public static Focusable findCurrentFocus(FocusManager focusManager) {
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
            
            return findCurrentFocus((FocusManager)focusableChild);
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

            // look for path by looping up target to root
            pathToTarget = finder.findPath(focusManager, focusTarget, new HasParentFocusableAdapter());
            if (pathToTarget != null && pathToTarget.size() > 0) {
                return pathToTarget;
            }

            return null;
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
            return buildFocusManagerFromComponent(component, eventBus, ()-> new DefaultFocusManager());
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
            return buildFocusManagerFromComponentChildren(component, eventBus, ()-> new DefaultFocusManager());
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


        public static Focusable findOrBuildFocusableFromComponent(Object component, EventBus eventBus) {
            return findOrBuildFocusableFromComponent(component, eventBus, ()-> new DefaultFocusManager());
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
            Predicate<Component<?>> skipProxiedManagers = (c)->{
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

                FocusManager lastFocusManager = focusManager; 
                boolean hasGoodPath = pathToTarget.size()>1;
                
                if (hasGoodPath) {
                    if (IS_REQUEST_FOCUS.test(te)) {
                        // set focus on focusTarget and all of its focusable parent's
                        focusManager.setFocus(true, te.getEventBus());
                        lastFocusManager = focusManager;
                        for (Focusable focusableChild : pathToTarget.subList(1, pathToTarget.size())) {
                            lastFocusManager.setChildAsCurrentFocus(focusableChild, te.getEventBus());
                            if (focusableChild instanceof FocusManager) {
                                lastFocusManager = (FocusManager) focusableChild;
                            }
                        }
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
