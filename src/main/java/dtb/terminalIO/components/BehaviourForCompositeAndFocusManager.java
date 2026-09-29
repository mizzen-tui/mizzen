package dtb.terminalIO.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.IntStream;

import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.geom.position.Position;

/**
 * This encapsulates the behaviour when a focus manager and a composite are working together.
 * 
 * <p/>
 * 
 * When a child component is added to a Composite, this object will also add the child component's focusables to the focusmanager
 * 
 * <p/>
 * 
 * When a child component is removed from the composite, this object will also remove the child component's focusables from the focusmanager.
 * <p/>
 * 
 * In this way, we keep the composite and the focusmanager in sync
 * 
 * @author dbalme
 *
 */
public class BehaviourForCompositeAndFocusManager {

    private Composite container;
    private FocusManager focusManager;

    public BehaviourForCompositeAndFocusManager(Composite container, FocusManager focusManager) {
        super();
        this.container = container;
        this.focusManager = focusManager;
    }

    protected Composite getContainer() {
        return container;
    }

    private void addFocusable(Focusable focusable) {
        if (this.focusManager == null) {
            throw new NullPointerException("addFocusable(): focusManager is null - cannot get access to a focusManager for "+ HasName.UTIL.getName(this));
        }
        // bad form here.. 
        ((DefaultFocusManager)this.focusManager).addFocusable(focusable);
        //focusable.setParentFocusManager(this);
    }

    private void insertFocusable(int index, Focusable focusable) {
        if (this.focusManager == null) {
            throw new NullPointerException("addFocusable(): focusManager is null - cannot get access to a focusManager for "+ HasName.UTIL.getName(this));
        }
        // bad form here.. 
        ((DefaultFocusManager)this.focusManager).insertFocusable(index, focusable);
        //focusable.setParentFocusManager(this);
    }

    private void removeFocusable(Focusable focusable) {
        if (this.focusManager == null) {
            throw new NullPointerException("removeFocusable(): focusManager is null - cannot get access to a focusManager for "+ HasName.UTIL.getName(this));
        }
        // bad form here.. 
        ((DefaultFocusManager)this.focusManager).removeFocusable(focusable);
        //focusable.setParentFocusManager(null);
    }

    private static final Focusable[] EMPTY_FOCUSABLES = new Focusable[0];

    // links component to its focusables 
    public static class ComponentToFocusables {
        Component<?> component;
        Focusable[] focusables;

        public ComponentToFocusables(Component<?> component, Collection<Focusable> focusables) {
            super();
            this.component = component;
            if (focusables == null) {
                this.focusables = EMPTY_FOCUSABLES;
            } else {
                this.focusables = focusables.toArray(new Focusable[focusables.size()]);
            }
        }

        public ComponentToFocusables(Component<?> component) {
            this(component, FocusManager.UTIL.focusablesFromComponent(component));
        }
    }
    
    // note - you would think I need a map - nope.. I want a list - don't change it.
    List<ComponentToFocusables> mapChildComponentToFocusables = new ArrayList<ComponentToFocusables>();

    private int findInMap(Component<?> component) {
        for ( int i = 0;  i < mapChildComponentToFocusables.size(); i++) {
            if (component == mapChildComponentToFocusables.get(i).component) {
                return i;
            }
        }
        return -1;
    }

    private boolean isInMap(Component<?> component) {
        return findInMap(component) >= 0;
    }

    public Focusable[] toFocusables(Component<?> component) {
        int idx = findInMap(component);
        if (idx < 0) {
            return EMPTY_FOCUSABLES;
        }
        return mapChildComponentToFocusables.get(idx).focusables;
    }

    public void addComponentAndItsFocusables(Position offset, Component<?> component) {
        getContainer().add(offset, component);
        addComponentFocusablesToFocusManager(component);
    }

    public void addComponentAndItsFocusables(Component<?> component) {
        addComponentAndItsFocusables(Position.ZERO, component);
    }

    public void addComponentAndItsFocusables(int row, int col, Component<?> component) {
        addComponentAndItsFocusables(new Position.Simple(row, col),component);
    }

    // ASSUMPTION:  component has already been added to the container
    public void addComponentFocusablesToFocusManager(Component<?> component) {

        // sanity checks
        int idx = this.getContainer().indexOf(component);
        if (idx < 0) {
            throw new IllegalStateException("incorrect use of addComponentFocusablesToFocusManager().. Component parameter not found in internal container.  You first need to add component to container first!");
        }

        // if component's focusables are already added, then let's remove and later (below) re-add ( because related focusable's could now be different)
        removeComponentFocusablesFromFocusManager(component);

        ComponentToFocusables cf = new ComponentToFocusables(component);

        // find next component with focusables - if any
        OptionalInt nextComponentIdx = IntStream.range(idx+1, getContainer().getChildren().size()).filter( i ->  isInMap(getContainer().getChild(i))).findFirst();

        if ( ! nextComponentIdx.isPresent()) { // there is no next component, so easy (and common) case, we're the last 
            mapChildComponentToFocusables.add(cf);
            for ( Focusable f : cf.focusables) {
                addFocusable(f);
            }
            return;
        }

        // now the fun begins .. we need to figure out where to insert the focusables based on where component
        // is positioned with the childrenlist

        Component<?> nextComponent = getContainer().getChild(nextComponentIdx.getAsInt());
        int nextComponentIdxInMap = findInMap(nextComponent);
        // sanity check
        if (nextComponentIdxInMap < 0) {
            throw new IllegalStateException("LOGIC ERROR!!! nextComponent not in map");
        }

        int focusableIndex = 0;
        if (nextComponentIdxInMap > 0) {
            focusableIndex = this.getContainer().getChildrenList().subList(0, nextComponentIdxInMap)
                                                                  .stream().map((c)-> toFocusables(c).length )
                                                                           .mapToInt(Integer::intValue)
                                                                           .sum();
        }

        mapChildComponentToFocusables.add(nextComponentIdxInMap, cf);
        for ( Focusable f : cf.focusables) {
            insertFocusable(focusableIndex, f);
            focusableIndex++;
        }

    }

    public void removeComponentFocusablesFromFocusManager(Component<?> component) {
        int idx = findInMap(component);
        if (idx == -1) {
            return ;
        }
        ComponentToFocusables cf = mapChildComponentToFocusables.get(idx);
        mapChildComponentToFocusables.remove(cf);
        for ( Focusable f : cf.focusables) {
            removeFocusable(f);
        }
    }

    public void rebuildComponentFocusablesFromFocusManager(Component<?> component) {
    	removeComponentFocusablesFromFocusManager(component);
    	addComponentFocusablesToFocusManager(component);
    	
    }
}
