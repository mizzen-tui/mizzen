package dtb.terminalIO.events.focus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.TerminalEvent.IS_Event_TYPE;
import dtb.terminalIO.traverse.FindPathToTarget;
import dtb.terminalIO.traverse.HasChildren;
import dtb.terminalIO.traverse.HasChildrenAdapter;

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
     * Utility focus methods helpful when setting or unsetting focus
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

        
        /**
         * Traverse this object and all its child (and grand-child .. ) objects looking for
         * focusTarget.  
         * 
         * If focusTarget found,  then it returns path (a list of objects)
         * from focusManager to focusTarget
         */
        public static List<Focusable> findTargetAsPath(FocusManager focusManager, Focusable focusTarget) {

            List<Focusable> pathToTarget = new FindPathToTarget<Focusable>(new FocusManagerHasChildrenAdapter()).findPath(focusManager, focusTarget);

            return pathToTarget;
        }
        

        
        private static void addFocusableChildren(List<Focusable> focusableContainer, Object component) {
            if (component instanceof Focusable) {
                focusableContainer.add((Focusable)component);
                return;
            }

            if (component instanceof ToFocusable) {
                focusableContainer.add(((ToFocusable)component).asFocusable());
                return;
            }
            if (component instanceof HasChildren) {
                for (Object o : ((HasChildren<?>)component).getChildren()) {
                    addFocusableChildren(focusableContainer, o);
                }
            }
        }
        
        public static FocusManager buildFocusManagerFromComponent(Object component, EventBus eventBus) {
            if (component instanceof FocusManager) {
                return (FocusManager) component;
            }
            List<Focusable> focusables = new ArrayList<Focusable>();
            addFocusableChildren(focusables, component);
            if (focusables.size()==0) {
                return null;
            }
            if (focusables.size()==1 && focusables.get(0) instanceof FocusManager) {
                return (FocusManager) focusables.get(0);
            }
            
            DefaultFocusManager focusManager = new DefaultFocusManager();
            for (Focusable focusable : focusables) {
                focusManager.addFocusable(focusable, eventBus);
            }
            
            return focusManager;
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

            return new ConditionalEventHandler.Impl(canHandlePredicate, (te) -> {

                @SuppressWarnings("unchecked")
                TerminalEvent<Focusable> fe = (TerminalEvent<Focusable>)te;

                Focusable focusTarget = fe.getEventData();

                List<Focusable> pathToTarget = findTargetAsPath(focusManager, focusTarget);

                if (pathToTarget == null) {
                    // we should not get here because canHandlePredicate
                    // should dealt with this..
                    return;
                }

                // at this point target is within our tree of focusable's
                // so time to traverse the path in order to "select" (focus) it

                if ( ! focusManager.hasFocus()) {
                    // FIXME: dont know how to deal with this scenario yet
                } 
                
                // sanity check
                if (pathToTarget.get(0) != focusManager) {
                    throw new IllegalStateException("pathToTarget does not start with root focusManager");
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
                                    targetsParentFocusManager.setChildAsCurrentFocus(null, te.getEventBus());
                                } else {
                                    // odd that we get here ..
                                    focusTarget.setFocus(false, te.getEventBus());
                                }
                            }
                        }
                    }
                }
                
            });

        }
    }
    

}
