package mizzen.components.input;

import mizzen.events.EventHandler;

/**
 * Something a form can bind to a field of a data object: its value read and written, and its changes
 * heard. Most inputs are one (TextInput, CheckBoxInput, DropdownListBox, ...); a component that isn't
 * an input itself can still act as one: ToFormInput.
 * <p/>
 * The same pattern as Focusable / ToFocusable. A component of your own binds the same way: implement
 * this, or ToFormInput, or register one for it (form.FormInputs).
 * <pre>
 *     binder.bind("name", nameInput);       // a TextInput is a FormInput&lt;String&gt;
 * </pre>
 */
public interface FormInput<V> extends SettableHasValue<V> {

    /**
     * handler runs when the user changes the value. One handler: a second call replaces it, as every
     * input's onChange does - a form bound again (rebuilt, or another form) takes over instead of piling
     * up handlers. A ToFormInput hands out the same FormInput each time, as asFocusable does.
     */
    void onChange(EventHandler handler);
}
