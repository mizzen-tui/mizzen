package mizzen.components;

import mizzen.traverse.HasParentAdapter;

public class HasParentComponentAdapter extends HasParentAdapter<Component<?>, ParentContainer>{

    public HasParentComponentAdapter() {
        super();
    }

    @Override
    public ParentContainer getParent(Component<?> component) {
        return component.getParentContainer();
    }

}
