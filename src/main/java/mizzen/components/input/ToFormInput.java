package mizzen.components.input;

/**
 * A component that isn't an input itself but can act as one for a form - e.g. a picker or a viewer
 * whose current item is the value. The same pattern as ToFocusable: hand out the same FormInput each time.
 * <pre>
 *     binder.bind("colour", colourPicker);  // the binder calls asFormInput()
 * </pre>
 */
public interface ToFormInput<V> {

    FormInput<V> asFormInput();
}
