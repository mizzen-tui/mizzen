package mizzen.components.modal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

import mizzen.app.TUIApp;
import mizzen.components.BorderedComponent;
import mizzen.components.ChildComponentManager;
import mizzen.components.Component;
import mizzen.components.GetComponentAtOffset;
import mizzen.components.HasName;
import mizzen.components.TextComponent;
import mizzen.components.composite.Composite;
import mizzen.components.composite.HorizontalPanel;
import mizzen.components.composite.VerticalPanel;
import mizzen.components.input.Button;
import mizzen.components.input.TextInput;
import mizzen.components.input.form.Form;
import mizzen.components.theme.Theme;
import mizzen.controlcodes.color.Color;
import mizzen.events.ConditionalEventHandler;
import mizzen.events.EventBus;
import mizzen.events.EventHandler;
import mizzen.events.RequestUpdateCanvasEvent;
import mizzen.events.TerminalEvent;
import mizzen.events.focus.RequestFocusEvent;
import mizzen.events.focus.ToFocusable;
import mizzen.events.keyboard.KeyInputEventHandler;
import mizzen.events.keyboard.KeyStroke;
import mizzen.events.keyboard.Keys;
import mizzen.events.mouse.ApplicationMouseEvent;
import mizzen.events.mouse.Draggable;
import mizzen.geom.Dimension;
import mizzen.geom.Rectangle;
import mizzen.geom.position.Position;
import mizzen.traverse.HasChildrenAdapter;
import mizzen.util.StringUtils;

/**
 * A modal dialog: an icon and a message at the top, anything in the middle (inputs, a Form), buttons
 * at the bottom. Built step by step, then shown; while it is open, the keys are its own.
 * <pre>
 *     Dialog.info("Saved 3 files.").ok().show(bus, parent);
 *
 *     Dialog.question("Delete 3 backups?").title("Delete")
 *             .button("delete", "Delete", (d) -&gt; deleteBackups())
 *             .cancel()
 *             .show(bus, parent);
 * </pre>
 * A button closes the dialog once its action has run - unless the action calls d.showError(..) (the
 * message shows and it stays open), a submit button finds a Form in it invalid, or the button was
 * marked staysOpen() (a Reset). A disabled button (enabledWhen) can't be pressed.
 * <pre>
 *     TextInput word = new TextInput(10);
 *     Dialog.warning("Runs on the live server.").title("Back up production")
 *             .add("Type BACKUP to continue: ", word)
 *             .button("run", "Run", (d) -&gt; startBackup())
 *                 .enabledWhen(() -&gt; "BACKUP".equals(word.getText()))
 *             .cancel()
 *             .show(bus, parent);
 * </pre>
 * Buttons have a name and a label: the name finds one (d.button("run")) and never changes; the label
 * may be translated. The first button is the dialog's own: Enter in a text field of the dialog presses
 * it. cancel() is Esc. Focus starts in the first text field, else on the first button.
 */
public class Dialog {

    public enum Icon { QUESTION, INFO, WARNING, ERROR }

    /** a button to make when the dialog is shown */
    private static final class ButtonPlan {
        final String name;
        final String label;
        final Consumer<Dialog> action;
        final boolean submit;
        KeyStroke key;
        BooleanSupplier enabledWhen;
        boolean staysOpen;

        ButtonPlan(String name, String label, Consumer<Dialog> action, boolean submit) {
            this.name = name;
            this.label = label;
            this.action = action;
            this.submit = submit;
        }
    }

    private static final Consumer<Dialog> NOTHING = (d) -> { };
    private static final String CANCEL = "cancel";

    private final Icon icon;
    private final String message;
    private String title = "";
    private final List<Component<?>> middle = new ArrayList<Component<?>>();
    private final List<ButtonPlan> plans = new ArrayList<ButtonPlan>();
    private Runnable onClose = () -> { };

    // once shown
    private final List<Button> buttons = new ArrayList<Button>();
    private ModalPanel panel;
    private VerticalPanel layout;
    private TextComponent errorLine;
    private ChildComponentManager<?> parent;
    private ModalEventHandler modalEvents;
    private EventBus bus;
    private boolean errorRaised;
    private boolean closed;
    private boolean draggable;                 // draggable(): its title moves it
    private boolean dragging;                  // a drag by the title is on: the mouse is the dialog's wherever it goes

    private Dialog(Icon icon, String message) {
        this.icon = icon;
        this.message = message;
    }

    public static Dialog info(String message) {
        return new Dialog(Icon.INFO, message).title("Info");
    }

    public static Dialog warning(String message) {
        return new Dialog(Icon.WARNING, message).title("Warning");
    }

    public static Dialog error(String message) {
        return new Dialog(Icon.ERROR, message).title("Error");
    }

    public static Dialog question(String message) {
        return new Dialog(Icon.QUESTION, message);
    }

    /** the words on its top border; none by default for a question, the kind of dialog for the others */
    public Dialog title(String title) {
        this.title = title == null ? "" : title;
        return this;
    }

    /**
     * Its top border, with the title, can be dragged with the mouse to move it - e.g. off a picture its options change.
     * It needs a title (it fails when shown without one). Dialogs are not draggable unless asked: the others take the
     * mouse exactly as before.
     * <pre>
     *     Dialog.info("Image Render options").draggable().add(controls).ok().show(bus, parent);
     * </pre>
     */
    public Dialog draggable() {
        this.draggable = true;
        return this;
    }

    /** a component in the middle, under the message - an input, a panel, a Form */
    public Dialog add(Component<?> component) {
        middle.add(component);
        return this;
    }

    /** label and input on one line in the middle: add("Name: ", nameInput) */
    public Dialog add(String label, Component<?> input) {
        HorizontalPanel line = new HorizontalPanel();
        line.add(new TextComponent(label));
        line.add(input);
        return add(line);
    }

    /**
     * a Form for data in the middle, in the dialog theme's colours - rows adds its rows. A submit button checks
     * it before its action runs.
     * <pre>
     *     .form(login, (f) -&gt; f.text("User", "user").required().password("Password", "password").required())
     * </pre>
     * A Form made elsewhere can go in with add(form): it keeps its own colours.
     */
    public <T> Dialog form(T data, Consumer<Form<T>> rows) {
        Form<T> form = Form.of(data).labelColor(null).inputColor(null).noteColor(null).headingColor(null).errorColor(null);
        rows.accept(form);
        return add(form);
    }

    /** a button: action runs when it is pressed, then the dialog closes (see the class comment) */
    public Dialog button(String name, String label, Consumer<Dialog> action) {
        plans.add(new ButtonPlan(name, label, action, false));
        return this;
    }

    /** a button that only closes the dialog */
    public Dialog button(String name, String label) {
        return button(name, label, NOTHING);
    }

    /** a button that checks each Form in the middle first: its action runs only if they are all valid */
    public Dialog submit(String name, String label, Consumer<Dialog> action) {
        plans.add(new ButtonPlan(name, label, action, true));
        return this;
    }

    /** an OK button, named "ok", that closes the dialog */
    public Dialog ok() {
        return button("ok", "OK");
    }

    /** a Cancel button, named "cancel": Esc presses it */
    public Dialog cancel() {
        return cancel("Cancel", NOTHING);
    }

    public Dialog cancel(Consumer<Dialog> action) {
        return cancel("Cancel", action);
    }

    /** a cancel button with another label, e.g. cancel("No", ..) */
    public Dialog cancel(String label, Consumer<Dialog> action) {
        button(CANCEL, label, action);
        return key(Keys.ESC_KEYSTROKE);
    }

    /** the button just added is also pressed by key, e.g. key(KeyStroke.caseInsensitive('y')) */
    public Dialog key(KeyStroke key) {
        lastPlan("key").key = key;
        return this;
    }

    /** the button just added can be pressed only while condition is true - checked after each key in the dialog */
    public Dialog enabledWhen(BooleanSupplier condition) {
        lastPlan("enabledWhen").enabledWhen = condition;
        return this;
    }

    /** the button just added leaves the dialog open after its action, e.g. a Reset */
    public Dialog staysOpen() {
        lastPlan("staysOpen").staysOpen = true;
        return this;
    }

    /** runs once the dialog has closed, however it closed */
    public Dialog onClose(Runnable onClose) {
        this.onClose = onClose;
        return this;
    }

    private ButtonPlan lastPlan(String setting) {
        if (plans.isEmpty()) {
            throw new IllegalStateException(setting + "(..) goes after a button(..)");
        }
        return plans.get(plans.size() - 1);
    }

    // ---- while it is open

    /** the button named name; fails if there is none */
    public Button button(String name) {
        for (int i = 0; i < plans.size(); i++) {
            if (plans.get(i).name.equals(name) && i < buttons.size()) {
                return buttons.get(i);
            }
        }
        throw new IllegalArgumentException("the dialog has no button named " + name + " (it has " + names() + ")");
    }

    /** shows text in red above the buttons; called from a button's action, it keeps the dialog open */
    public Dialog showError(String text) {
        errorRaised = true;
        if (errorLine == null) {
            errorLine = new TextComponent(text).setThemeColor(Theme::errorColor);
            layout.insert(layout.getChildren().size() - 1, Position.ZERO, errorLine);   // just above the buttons
        } else {
            errorLine.setTextAndResize(text);
        }
        return this;
    }

    public boolean isOpen() {
        return panel != null && !closed;
    }

    /** closes it; nothing happens if it is already closed */
    public void close() {
        if (closed || panel == null) {
            return;
        }
        closed = true;
        parent.remove(panel);
        panel.asFocusable().setFocus(false, bus);
        bus.removeEventHandler(modalEvents);
        bus.raiseEvent(new RequestUpdateCanvasEvent(panel));
        onClose.run();
    }

    // ---- showing it

    /**
     * builds it and shows it over the screen of the app that component is on - its main panel, centred -
     * with the keys and the mouse its own until it closes. Any component on that screen will do: it only
     * says which app (for an embedded app, its own panel, not the host's).
     * <pre>
     *     Dialog.error("Can't read " + file).ok().show(saveButton);
     * </pre>
     * Fails if component is on no app's screen yet.
     */
    public Dialog show(Component<?> component) {
        TUIApp app = TUIApp.of(component);
        if (app == null) {
            throw new IllegalStateException("Dialog.show(component): " + HasName.UTIL.getName(component)
                    + " is on no TUIApp's screen - add it first, or use show(app)");
        }
        return show(app);
    }

    /** builds it and shows it over app's main panel, centred - for app code with no component at hand */
    public Dialog show(TUIApp app) {
        return show(app.getEventBus(), app.getMainApplicationPanel());
    }

    /**
     * builds it and shows it over parent, centred, with the keys its own until it closes. parent is
     * normally the app's main panel - show(component) and show(app) find it. Another panel clips the
     * dialog to itself, may grow to hold it, and lays it out if it is a Vertical/HorizontalPanel.
     */
    public Dialog show(EventBus bus, ChildComponentManager<?> parent) {
        if (panel != null) {
            throw new IllegalStateException("a Dialog is shown once: build another");
        }
        if (plans.isEmpty()) {
            throw new IllegalStateException("a dialog needs a button to close it - e.g. ok() or cancel()");
        }
        this.bus = bus;
        this.parent = parent;
        if (draggable && title.isEmpty()) {
            throw new IllegalStateException("a draggable dialog needs a title to drag it by - title(..)");
        }
        // only a draggable dialog's panel is Draggable: the others take the mouse exactly as before
        panel = draggable ? new MovedByItsTitle() : new ModalPanel();   // colours: the dialog theme (ModalPanel), so nothing here gives one
        panel.setThemeColor(Theme::textColor);             // the frame and title: text on the dialog's colour
        panel.setBorder(true);
        if (!title.isEmpty()) {
            panel.setTitle(" " + title + " ");
        }
        panel.setFillOnClear(true);
        panel.setName("dialog");

        layout = new VerticalPanel().setVerticalSpacer(1);
        layout.add(messageWithIcon());
        for (Component<?> component : middle) {
            layout.add(component);
        }
        layout.add(buttonRow());
        panel.add(new Position.Simple(1, 1), layout);

        Position at = parent instanceof Rectangle ? ((Rectangle) parent).centered(panel, Position.ZERO) : Position.ZERO;
        parent.add(at, panel);
        parent.setDropShadow(true, panel);

        modalEvents = new ModalEventHandler(bus).stopsMouseOutside(this::isOnDialog);
        modalEvents.appendEventHandler(new KeyInputEventHandler(Keys.ESC_KEYSTROKE, (te) -> escape()));
        modalEvents.appendEventHandler(thenRefresh(panel.asEventHandler()));
        for (TextInput field : textFields()) {
            field.onEnter((te) -> pressDefault());
        }
        refreshEnabled();

        List<TextInput> fields = textFields();
        bus.raiseEvent(new RequestUpdateCanvasEvent(panel));
        bus.raiseEvent(new RequestFocusEvent(ToFocusable.UTIL.toFocusable(fields.isEmpty() ? firstEnabledButton() : fields.get(0))));
        return this;
    }

    /**
     * Is what is drawn at spot (on the canvas) part of this dialog? Its panel and everything in it, and a popup
     * one of its components opened - a dropdown's list is drawn on the root panel and may stick out past the
     * dialog's edge, but it belongs to its dropdown (Component.UTIL.isPartOf). The lookup starts at the top of the
     * dialog's tree, where popups are drawn, as the mouse handling does.
     */
    private boolean isOnDialog(Position spot) {
        if (dragging) {
            return true;                                  // a drag by its title goes on, and ends, wherever the mouse is
        }
        Component<?> top = panel;
        while (top.getParentContainer() instanceof Component) {
            top = (Component<?>) top.getParentContainer();
        }
        Position topAt = Component.UTIL.getCanvasOffsetFor(top);
        if (topAt == null) {
            return false;                                 // not on the canvas
        }
        Component<?> under = GetComponentAtOffset.UTIL.getComponentAtOffset(top, spot.offsetAdd(topAt.negate()));
        return under != null && Component.UTIL.isPartOf(under, panel);
    }

    private Component<?> messageWithIcon() {
        String shown = Dimension.UTIL.stringToDimension(message).getHeight() == 1 ? "\n" + message : message;   // level with the icon's middle
        HorizontalPanel top = new HorizontalPanel().setHorizontalSpacer(2);
        top.setFillOnClear(true);
        top.add(iconComponent(icon));
        TextComponent text = new TextComponent(shown);
        text.setFillOnClear(true);
        top.add(text);
        return top;
    }

    private Component<?> buttonRow() {
        HorizontalPanel row = new HorizontalPanel().setHorizontalSpacer(3);
        row.setFillOnClear(true);
        row.setName("dialog.buttons");
        TextComponent leftPadding = new TextComponent("");
        row.add(leftPadding);
        for (ButtonPlan plan : plans) {
            Button button = new Button(plan.label);
            button.setName(plan.name);
            button.setBorder(true);
            if (plan.key != null) {
                button.setHotKey(plan.key);
            }
            button.onChange((te) -> press(plan));
            buttons.add(button);
            row.add(button);
        }
        int widest = 0;                                    // centred under what is above
        for (Component<?> component : middle) {
            widest = Math.max(widest, component.getWidth());
        }
        widest = Math.max(widest, messageWidth());
        int padding = (widest - row.getWidth()) / 2;
        if (padding > 0) {
            leftPadding.setTextAndResize(StringUtils.repeat(" ", padding));
        }
        return row;
    }

    private int messageWidth() {
        return Dimension.UTIL.stringToDimension(message).getWidth() + 7;   // the icon and its gap
    }

    // a button pressed: its action, then close - unless it reported an error, a form is invalid, or it stays open
    private void press(ButtonPlan plan) {
        if (closed) {
            return;
        }
        if (plan.submit && !formsValid()) {
            return;
        }
        errorRaised = false;
        plan.action.accept(this);
        if (!errorRaised && !plan.staysOpen) {
            close();
        }
    }

    private boolean formsValid() {
        boolean valid = true;
        for (Component<?> component : middle) {
            if (component instanceof Form) {
                Form<?> form = (Form<?>) component;
                form.binder().useEventBusIfNone(bus);
                valid &= form.validateAll();
            }
        }
        return valid;
    }

    // Esc: the cancel button, if there is one - its action runs; else just close
    /**
     * The panel of a draggable dialog: a drag that starts on its top row - the frame, with the title - moves it, by as
     * much as the mouse moves (the spot grabbed stays under the mouse), kept on the screen and not above or left of
     * the panel it is shown on. While a drag is on, the dialog takes every mouse event (isOnDialog), so one that gets
     * ahead of the dialog, and the button coming up there, still reach the drag and end it.
     */
    private final class MovedByItsTitle extends ModalPanel implements Draggable {
        private Position grab;                            // the mouse less the panel's corner, on the canvas

        @Override
        public boolean onDragStart(ApplicationMouseEvent mouse) {
            Position corner = Component.UTIL.getCanvasOffsetFor(this);
            if (!(parent instanceof Composite) || corner == null || mouse.getMousePosition().getRow() != corner.getRow()) {
                return false;                             // not on the top row: the contents' own business
            }
            grab = mouse.getMousePosition().offsetAdd(corner.negate());
            dragging = true;
            return true;
        }

        @Override
        public boolean onDrag(ApplicationMouseEvent mouse) {
            if (!dragging) {
                return false;
            }
            Composite under = (Composite) parent;
            Position corner = Component.UTIL.getCanvasOffsetFor(this);
            Rectangle at = under.childAsRectangleOffset(this);
            Rectangle seen = seenArea(under);
            Position wanted = mouse.getMousePosition().offsetAdd(grab.negate());
            int row = within(wanted.getRow(), Math.max(seen.getRow(), corner.getRow() - at.getRow()),
                             seen.getRow() + seen.getHeight() - getHeight());
            int col = within(wanted.getCol(), Math.max(seen.getCol(), corner.getCol() - at.getCol()),
                             seen.getCol() + seen.getWidth() - getWidth());
            if (row != corner.getRow() || col != corner.getCol()) {
                under.moveChildTo(new Position.Simple(at.getRow() + row - corner.getRow(), at.getCol() + col - corner.getCol()), this);
                under.markAsDirty();
                mouse.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(this));
            }
            return true;
        }

        @Override
        public boolean onDragStop(ApplicationMouseEvent mouse) {
            dragging = false;
            grab = null;
            return true;
        }
    }

    /**
     * Where a dialog shown on under can be seen, on the canvas: the panels around under clip it (under itself grows to
     * hold it). The screen when under is the top panel.
     */
    private static Rectangle seenArea(Composite under) {
        Component<?> c = under;
        Rectangle seen = null;
        while (c.getParentContainer() instanceof Component) {
            c = (Component<?>) c.getParentContainer();
            seen = seen == null ? canvasRectangle(c) : intersection(seen, canvasRectangle(c));
        }
        return seen != null ? seen : canvasRectangle(under);
    }

    private static Rectangle canvasRectangle(Component<?> c) {
        Position at = Component.UTIL.getCanvasOffsetFor(c);
        return new Rectangle.Simple(at.getRow(), at.getCol(), c.getHeight(), c.getWidth());
    }

    private static Rectangle intersection(Rectangle a, Rectangle b) {
        int row = Math.max(a.getRow(), b.getRow());
        int col = Math.max(a.getCol(), b.getCol());
        int bottom = Math.min(a.getRow() + a.getHeight(), b.getRow() + b.getHeight());
        int right = Math.min(a.getCol() + a.getWidth(), b.getCol() + b.getWidth());
        return new Rectangle.Simple(row, col, Math.max(0, bottom - row), Math.max(0, right - col));
    }

    // value kept within least .. most; least wins if most is below it (a dialog bigger than the screen)
    private static int within(int value, int least, int most) {
        return Math.max(least, Math.min(most, value));
    }

    private void escape() {
        for (int i = 0; i < plans.size(); i++) {
            if (CANCEL.equals(plans.get(i).name)) {
                press(plans.get(i));
                return;
            }
        }
        close();
    }

    // Enter in a text field: the first button, if it can be pressed
    private void pressDefault() {
        if (!buttons.isEmpty() && buttons.get(0).isEnabled()) {
            press(plans.get(0));
        }
    }

    // the dialog's own handling, then the buttons' enabledWhen checked again - a key may have changed what they test
    private ConditionalEventHandler thenRefresh(ConditionalEventHandler handler) {
        return new ConditionalEventHandler() {
            @Override
            public EventHandler canHandle(TerminalEvent<?> te) {
                return handler.canHandle(te) != null ? this : null;
            }

            @Override
            public void handle(TerminalEvent<?> te) {
                handler.handle(te);
                refreshEnabled();
            }
        };
    }

    private void refreshEnabled() {
        for (int i = 0; i < plans.size(); i++) {
            BooleanSupplier condition = plans.get(i).enabledWhen;
            Button button = buttons.get(i);
            if (condition != null && condition.getAsBoolean() != button.isEnabled()) {
                button.setEnabled(condition.getAsBoolean(), bus);
            }
        }
    }

    private Button firstEnabledButton() {
        for (Button button : buttons) {
            if (button.isEnabled()) {
                return button;
            }
        }
        return buttons.get(0);
    }

    private List<TextInput> textFields() {
        List<TextInput> fields = new ArrayList<TextInput>();
        for (Component<?> component : middle) {
            collectTextFields(component, fields);
        }
        return fields;
    }

    private static void collectTextFields(Component<?> component, List<TextInput> fields) {
        if (component instanceof TextInput) {
            fields.add((TextInput) component);
            return;
        }
        for (Component<?> child : new HasChildrenAdapter.Simple<Component<?>>().asHasChildren(component).getChildren()) {
            collectTextFields(child, fields);
        }
    }

    private String names() {
        List<String> names = new ArrayList<String>();
        for (ButtonPlan plan : plans) {
            names.add(plan.name);
        }
        return names.toString();
    }

    // ---- looks

    // the icon's colour: what kind of message it is, in the dialog theme
    static Function<Theme, Color> iconColor(Icon icon) {
        switch (icon) {
            case QUESTION: return Theme::headingColor;
            case INFO:     return Theme::linkColor;
            case WARNING:  return Theme::warningColor;
            case ERROR:    return Theme::errorColor;
            default:       throw new IllegalArgumentException("no colour for " + icon);
        }
    }

    private static Component<?> iconComponent(Icon icon) {
        char mark = icon == Icon.QUESTION ? '?' : icon == Icon.INFO ? 'i' : icon == Icon.WARNING ? '!' : 'X';
        Function<Theme, Color> color = iconColor(icon);
        BorderedComponent box = new BorderedComponent(new TextComponent(" " + mark + " ").setThemeColor(color));
        box.setThemeColor(color);
        box.setName("icon-" + mark);
        return box;
    }
}
