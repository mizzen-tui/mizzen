package mizzen.components.input.form;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import mizzen.components.Component;
import mizzen.components.TextComponent;
import mizzen.components.input.CheckBoxInput;
import mizzen.components.input.DisplayableValue;
import mizzen.components.input.DropdownListBox;
import mizzen.components.input.IntegerInputWithIncrDecrButtons;
import mizzen.components.input.MultiLineTextInput;
import mizzen.components.input.RadioGroup;
import mizzen.components.input.TextInput;
import mizzen.components.scroll.ScrollableComponent;
import mizzen.geom.Dimension;

/**
 * The rows a form is built of - the same in the form itself, a section and an inline group. Each
 * row makes an input, binds it to a field of the form's object by name, and gives a handle back
 * (form.row(field)). Labels and options can come from the object: functions asked again after each
 * change.
 * <pre>
 *     .text("Name", "name").required()
 *     .choice("Country", "country", COUNTRIES)
 *     .choice(c -&gt; c.inUS() ? "State" : "Province", "region", c -&gt; regionsOf(c.country))
 *     .row("Colour", "colour", new ColourPicker())          // any input of your own
 *     .add(anyComponent)                                   // anything else
 * </pre>
 * T: the object's type; B: the layout's own type, so the calls chain.
 */
public interface FormLayout<T, B extends FormLayout<T, B>> {

    /** the form this layout is part of */
    Form<T> form();

    B self();

    /** places a row - label, the component shown, the error line - and makes its handle */
    FormRow placeRow(String field, String labelText, Component<?> shown, Object input, TextComponent error);

    /** a line where the inputs start - a hint */
    B note(String text);

    /** any component, as it is */
    B add(Component<?> component);

    default B heading(String text) {
        return add(new TextComponent(form().headingColor, text));
    }

    default B text(String label, String field) {
        return row(label, field, form().textInput());
    }

    default B password(String label, String field) {
        TextInput input = form().textInput();
        input.setAsPassword();
        return row(label, field, input);
    }

    /** an Integer / int field: + and - step it */
    default B number(String label, String field) {
        return row(label, field, new IntegerInputWithIncrDecrButtons(form().inputColor, "", "-", "+"));
    }

    default B checkBox(String label, String field) {
        return row(label, field, new CheckBoxInput(form().inputColor));
    }

    /** several lines, scrolling; rows high */
    default B area(String label, String field, int rows) {
        MultiLineTextInput input = new MultiLineTextInput(form().inputColor);
        ScrollableComponent box = new ScrollableComponent(form().inputColor, new Dimension.Simple(rows + 2, form().inputWidth + 2), input);
        box.removeFooter();                              // too narrow for the scroll buttons; the keys scroll it
        form().addRow(this, field, label, box, input);
        return self();
    }

    /** any input - a FormInput, a ToFormInput, or one registered (FormInputs) */
    default B row(String label, String field, Component<?> input) {
        form().addRow(this, field, label, input, input);
        return self();
    }

    // ---- choices: options given, or found (an enum field's values); fixed, supplied, or from the object

    /** a dropdown of options not given here: an enum field's values (later: @Options); none - fails at once */
    default B choice(String label, String field) {
        return choice(label, field, form().optionsOf(field));
    }

    default <O> B choice(String label, String field, List<O> options) {
        return choiceRow(label, null, field, (t) -> options);
    }

    /** options asked for once, when the form is built (e.g. loaded) */
    default <O> B choice(String label, String field, Supplier<List<O>> options) {
        List<O> loaded = options.get();
        return choiceRow(label, null, field, (t) -> loaded);
    }

    /** options from the object, asked again after each change - e.g. the cities of the picked province */
    default <O> B choice(String label, String field, Function<T, List<O>> options) {
        return choiceRow(label, null, field, options);
    }

    /** the label from the object too - e.g. "State" or "Province" */
    default <O> B choice(Function<T, String> label, String field, Function<T, List<O>> options) {
        return choiceRow(label.apply(form().data), label, field, options);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    default <O> B choiceRow(String label, Function<T, String> labelFrom, String field, Function<T, List<O>> options) {
        DropdownListBox<Object> input = new DropdownListBox<Object>(form().inputColor,
                FormRow.displayable(options.apply(form().data)).toArray(new DisplayableValue[0]));
        FormRow row = form().addRow(this, field, label, input, input);
        row.shownOptions = options.apply(form().data);
        row.optionsFrom = (Function) options;
        row.labelFrom = (Function) labelFrom;
        return self();
    }

    /** one button a choice, in a row: an enum field's values */
    default B radio(String label, String field) {
        return radio(label, field, form().optionsOf(field));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    default <O> B radio(String label, String field, List<O> options) {
        DisplayableValue[] items = FormRow.displayable(options).toArray(new DisplayableValue[0]);
        return row(label, field, RadioGroup.horizontal(items));
    }

    /**
     * A row that shows the object rather than editing it - a picture, a total, a summary: update
     * runs now and after each change.
     * <pre>
     *     .view("Total", totalText, (o) -&gt; totalText.setTextAndResize(money(o.total())))
     * </pre>
     */
    default B view(String label, Component<?> shown, Consumer<T> update) {
        form().addView(this, label, shown, update);
        return self();
    }

    /**
     * A line of text worked out from the object - shown, not edited; worked out again after each change.
     * <pre>
     *     .view("Part", (i) -&gt; i.part.name + " at " + money(i.part.price))
     * </pre>
     */
    default B view(String label, Function<T, String> text) {
        TextComponent shown = new TextComponent(form().labelColor, "");
        return view(label, shown, (o) -> shown.setTextAndResize(text.apply(o)));
    }

    // ---- about the row just added

    /** the row just added must have a value: not empty */
    default B required() {
        form().require(form().lastRow());
        return self();
    }

    /** the row just added: its value must pass test, else message shows under it */
    default <V> B validate(Predicate<V> test, String message) {
        form().validate(form().lastRow(), test, message);
        return self();
    }

    // ---- groups

    /** rows side by side: City [____] Postal [___] */
    default B inline(Consumer<Inline<T>> rows) {
        Inline<T> inline = new Inline<T>(form());
        rows.accept(inline);
        return add(inline.panel);
    }

    /** rows in a titled box */
    default B section(String title, Consumer<Section<T>> rows) {
        Section<T> section = new Section<T>(form(), title);
        rows.accept(section);
        return add(section.panel);
    }

    /** a row of buttons: submit checks the form first, button doesn't */
    default B buttons(Consumer<Buttons<T>> buttons) {
        Buttons<T> row = new Buttons<T>(form());
        buttons.accept(row);
        return add(row.panel);
    }
}
