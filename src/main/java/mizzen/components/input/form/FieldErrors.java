package mizzen.components.input.form;

import mizzen.components.TextComponent;

/**
 * Where a form's errors show - given to a binder (errorsTo), whatever the layout: a status line, a
 * line under each field, a box above the form, several of these.
 * <pre>
 *     binder.errorsTo(FieldErrors.into(statusLine));
 *     binder.errorsTo((field, message) -&gt; ...);        // message null: that field is good again
 * </pre>
 */
@FunctionalInterface
public interface FieldErrors {

    /** field's error - or null: the field is good again */
    void show(String field, String message);

    /** each error in one line of text, and gone when its field is good again */
    static FieldErrors into(TextComponent line) {
        String[] showing = { null };                     // the field whose error the line shows
        return (field, message) -> {
            if (message != null) {
                showing[0] = field;
                line.setTextAndResize(" " + message);
            } else if (field.equals(showing[0])) {
                showing[0] = null;
                line.setTextAndResize(" ");
            }
        };
    }
}
