package mizzen.components.input.form;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import mizzen.components.HasName;
import mizzen.components.input.FormInput;
import mizzen.components.input.ToFormInput;

/**
 * Where a form finds the FormInput for a component that can't bind by itself - someone else's component,
 * or a different binding for one of ours. Most components need none: they are a FormInput, or have
 * asFormInput().
 * <p/>
 * A binder looks for a field's FormInput in this order, first match wins:
 * <pre>
 *     1. given for the field         binder.bind("phones", table, t -&gt; new TableRowsInput&lt;&gt;(model))
 *     2. the binder's own registry   binder.register(Type.class, factory)   - for one form
 *     3. the global registry         FormInputs.register(ColourPicker.class, p -&gt; new ColourPickerInput(p))
 *     4. the component itself        it is a FormInput, or a ToFormInput
 * </pre>
 * None: binding fails at once (when the form is bound, not when someone types), saying which to add.
 * In a registry the most specific class wins: the component's own class, then its superclasses, each
 * with the interfaces it declares.
 */
public class FormInputs {

    private static final FormInputs GLOBAL = new FormInputs(null);

    private final FormInputs parent;
    private final Map<Class<?>, Function<Object, FormInput<?>>> factories = new LinkedHashMap<Class<?>, Function<Object, FormInput<?>>>();

    FormInputs(FormInputs parent) {
        this.parent = parent;
    }

    /** a binder's own registry: its entries first, then the global ones */
    static FormInputs forBinder() {
        return new FormInputs(GLOBAL);
    }

    /** for every form: components of this type bind through the FormInput the factory makes */
    public static <C> void register(Class<C> type, Function<? super C, ? extends FormInput<?>> factory) {
        GLOBAL.add(type, factory);
    }

    <C> void add(Class<C> type, Function<? super C, ? extends FormInput<?>> factory) {
        synchronized (factories) {
            factories.put(type, (component) -> factory.apply(type.cast(component)));
        }
    }

    // this registry's factory for the type (most specific first), else the parent's
    private Function<Object, FormInput<?>> find(Class<?> type) {
        synchronized (factories) {
            for (Class<?> c = type; c != null; c = c.getSuperclass()) {
                Function<Object, FormInput<?>> factory = factories.get(c);
                if (factory != null) {
                    return factory;
                }
                for (Class<?> declared : c.getInterfaces()) {
                    factory = factories.get(declared);
                    if (factory != null) {
                        return factory;
                    }
                }
            }
        }
        return parent == null ? null : parent.find(type);
    }

    /** the component has a FormInput: registered, or its own */
    boolean canResolve(Object component) {
        return find(component.getClass()) != null || component instanceof FormInput || component instanceof ToFormInput;
    }

    /** the component's FormInput - steps 2 to 4 above; fails fast if there is none */
    FormInput<?> resolve(String field, Object component) {
        Function<Object, FormInput<?>> factory = find(component.getClass());
        if (factory != null) {
            return factory.apply(component);
        }
        if (component instanceof FormInput) {
            return (FormInput<?>) component;
        }
        if (component instanceof ToFormInput) {
            return ((ToFormInput<?>) component).asFormInput();
        }
        String type = component.getClass().getSimpleName();
        throw new IllegalArgumentException("No FormInput for " + type + " " + HasName.UTIL.getName(component)
                + " (field \"" + field + "\"): make it a FormInput, give it asFormInput() (ToFormInput),"
                + " register one - FormInputs.register(" + type + ".class, ...) or binder.register(...) -"
                + " or give one to bind(\"" + field + "\", component, ...)");
    }
}
