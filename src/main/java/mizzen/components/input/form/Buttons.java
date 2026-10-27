package mizzen.components.input.form;

import java.util.function.Consumer;

import mizzen.components.composite.HorizontalPanel;
import mizzen.components.input.Button;

/**
 * A row of buttons in a form. submit checks the whole form first (validateAll) and runs only when
 * it is good - the errors show under their fields otherwise; button just runs.
 * <pre>
 *     .buttons(b -&gt; b.submit("Sign in", f -&gt; signIn(login))
 *                    .button("Cancel", f -&gt; close()))
 * </pre>
 */
public class Buttons<T> {

    private static final int APART = 2;

    private final Form<T> form;
    final HorizontalPanel panel;

    Buttons(Form<T> form) {
        this.form = form;
        this.panel = new HorizontalPanel(form.labelColor, APART);
    }

    public Buttons<T> submit(String text, Consumer<Form<T>> action) {
        Button button = new Button(form.labelColor, text);
        button.onChange((te) -> {
            form.binder().useEventBusIfNone(te.getEventBus());
            if (form.validateAll()) {
                action.accept(form);
            }
        });
        panel.add(button);
        return this;
    }

    public Buttons<T> button(String text, Consumer<Form<T>> action) {
        Button button = new Button(form.labelColor, text);
        button.onChange((te) -> action.accept(form));
        panel.add(button);
        return this;
    }
}
