package mizzen.components.input;

import mizzen.events.EventHandler;
import mizzen.events.Registration;

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

    // setValue(null) means "nothing": empty text, off, nothing selected - it never throws, and getValue
    // gives null back (or "" for text, false for yes/no). A value the input can't show (not among its
    // choices, not in the tree) still fails at once: that is a mistake, "nothing" isn't.

    /**
     * handler runs when the user changes the value - alongside any others (an app's, a form's): each
     * call adds one (OnChangeListeners does it). The Registration takes this one out again; most
     * listeners never need it, living as long as the component. null takes every listener out.
     * A ToFormInput hands out the same FormInput each time, as asFocusable does.
     */
    Registration onChange(EventHandler handler);
}
