package mizzen.components.theme;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import mizzen.app.TUIApp;
import mizzen.components.Component;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.controlcodes.color.Colors.FGBGColors;
import mizzen.core.Canvas;
import mizzen.traverse.HasChildrenAdapter;

/**
 * The colours a component takes when it was given none: a fixed set, one per kind of thing - text,
 * headings, inputs, buttons, borders, the background. A theme never changes once made; a different one
 * is a new one (with..).
 * <pre>
 *     new TextComponent("Total");                              // no colour given: the theme's text colour
 *     new TextComponent(Colors.BRed, "Overdue");               // given one: always red, whatever the theme
 *     new TextComponent(" ").setThemeColor(Theme::errorColor); // the theme's error colour, whichever theme
 * </pre>
 * Which theme applies to a component, nearest first: one set on it or an ancestor (setTheme), else its
 * app's (TUIApp.setTheme), else the default (Theme.setDefault). The colours carry the theme's background,
 * so what a component leaves empty is the theme's background too.
 * <pre>
 *     Theme.setDefault(Theme.CLASSIC);                         // the whole program
 *     tApp.setTheme(Theme.LIGHT);                              // one app
 *     sidePanel.setTheme(Theme.DARK.withHeadingColor(Colors.BYel));   // one part of the screen
 * </pre>
 * Components look their theme up once and keep it (getTheme). Setting a theme anywhere bumps version:
 * each looks again, and draws again, on its next draw. A component that moves forgets its own (forget).
 */
public final class Theme {

    /** the colours as they were before themes: a component given no colour looks as it always has */
    public static final Theme LEGACY = new Theme("Legacy", Colors.DEFAULT_COLOR, Colors.DEFAULT_COLOR, Colors.BYel,
            Colors.DEFAULT_COLOR, Colors.DEFAULT_COLOR, Colors.DEFAULT_COLOR, Colors.DEFAULT_COLOR,
            Colors.DEFAULT_DISABLED_COLOR, Colors.BIRed, Colors.IGre, Colors.DEFAULT_COLOR);

    /** light text on black, cyan headings, dark grey input fields, light grey buttons */
    public static final Theme DARK = onBackground("Dark", Colors.On_Bla, Colors.Whi, Colors.BCya,
            on(Colors.BWhi, Colors.On_IBla), on(Colors.Bla, Colors.On_Whi), on(Colors.Bla, Colors.On_IWhi),
            Colors.IBla, Colors.IBla, Colors.BIRed, Colors.BIGre, on(Colors.Bla, Colors.On_ICya));

    /** dark text on white, blue headings and buttons */
    public static final Theme LIGHT = onBackground("Light", Colors.On_IWhi, Colors.Bla, Colors.Blu,
            on(Colors.Bla, Colors.On_Whi), on(Colors.BWhi, Colors.On_Blu), on(Colors.BWhi, Colors.On_IBlu),
            Colors.IBla, Colors.IBla, Colors.Red, Colors.Gre, on(Colors.BWhi, Colors.On_Blu));

    /** the classic blue screen: white on blue, yellow headings, cyan fields, green buttons */
    public static final Theme CLASSIC = onBackground("Classic", Colors.On_Blu, Colors.Whi, Colors.BYel,
            on(Colors.Bla, Colors.On_Cya), on(Colors.Bla, Colors.On_Gre), on(Colors.BWhi, Colors.On_Gre),
            Colors.BWhi, Colors.IBla, Colors.BIRed, Colors.BIGre, on(Colors.Bla, Colors.On_Cya));

    private static final List<Theme> STANDARD = Collections.unmodifiableList(Arrays.asList(LEGACY, DARK, LIGHT, CLASSIC));

    private static volatile Theme defaultTheme = LEGACY;
    private static volatile int version = 1;

    static {
        Canvas.setDefaultEmptyColor(() -> defaultTheme.backgroundColor());   // a screen given no empty colour: the default theme's background
    }

    private String name;
    private Color backgroundColor;      // what is left empty: the screen, a panel's gaps
    private Color textColor;            // text, and anything not some other kind of thing
    private Color headingColor;
    private Color inputColor;           // text inputs, check boxes, dropdowns
    private Color buttonColor;
    private Color buttonFocusedColor;
    private Color borderColor;          // lines: borders, dividers
    private Color disabledColor;
    private Color errorColor;
    private Color successColor;
    private Color selectedColor;        // the picked row of a list, tree or table

    private Theme(String name, Color backgroundColor, Color textColor, Color headingColor, Color inputColor, Color buttonColor,
                  Color buttonFocusedColor, Color borderColor, Color disabledColor, Color errorColor, Color successColor,
                  Color selectedColor) {
        this.name = name;
        this.backgroundColor = backgroundColor;
        this.textColor = textColor;
        this.headingColor = headingColor;
        this.inputColor = inputColor;
        this.buttonColor = buttonColor;
        this.buttonFocusedColor = buttonFocusedColor;
        this.borderColor = borderColor;
        this.disabledColor = disabledColor;
        this.errorColor = errorColor;
        this.successColor = successColor;
        this.selectedColor = selectedColor;
    }

    // a theme on one background: the plain foregrounds given get it; colours given whole (inputs, buttons) keep their own
    private static Theme onBackground(String name, Colors.BgColor background, Colors.FgColor text, Colors.FgColor heading,
                                      Color input, Color button, Color buttonFocused, Colors.FgColor border,
                                      Colors.FgColor disabled, Colors.FgColor error, Colors.FgColor success, Color selected) {
        return new Theme(name, on(text, background), on(text, background), on(heading, background), input, button,
                buttonFocused, on(border, background), on(disabled, background), on(error, background),
                on(success, background), selected);
    }

    private static Color on(Colors.FgColor foreground, Colors.BgColor background) {
        return new FGBGColors(foreground, background);
    }

    // ---- the colours

    public String name()               { return name; }
    public Color backgroundColor()     { return backgroundColor; }
    public Color textColor()           { return textColor; }
    public Color headingColor()        { return headingColor; }
    public Color inputColor()          { return inputColor; }
    public Color buttonColor()         { return buttonColor; }
    public Color buttonFocusedColor()  { return buttonFocusedColor; }
    public Color borderColor()         { return borderColor; }
    public Color disabledColor()       { return disabledColor; }
    public Color errorColor()          { return errorColor; }
    public Color successColor()        { return successColor; }
    public Color selectedColor()       { return selectedColor; }

    @Override
    public String toString() {
        return name;
    }

    // ---- making another: the same, with one thing different

    public Theme withName(String name)                 { return with((t) -> t.name = name); }
    public Theme withBackgroundColor(Color color)      { return with((t) -> t.backgroundColor = color); }
    public Theme withTextColor(Color color)            { return with((t) -> t.textColor = color); }
    public Theme withHeadingColor(Color color)         { return with((t) -> t.headingColor = color); }
    public Theme withInputColor(Color color)           { return with((t) -> t.inputColor = color); }
    public Theme withButtonColor(Color color)          { return with((t) -> t.buttonColor = color); }
    public Theme withButtonFocusedColor(Color color)   { return with((t) -> t.buttonFocusedColor = color); }
    public Theme withBorderColor(Color color)          { return with((t) -> t.borderColor = color); }
    public Theme withDisabledColor(Color color)        { return with((t) -> t.disabledColor = color); }
    public Theme withErrorColor(Color color)           { return with((t) -> t.errorColor = color); }
    public Theme withSuccessColor(Color color)         { return with((t) -> t.successColor = color); }
    public Theme withSelectedColor(Color color)        { return with((t) -> t.selectedColor = color); }

    private Theme with(Consumer<Theme> change) {
        Theme copy = new Theme(name, backgroundColor, textColor, headingColor, inputColor, buttonColor, buttonFocusedColor,
                borderColor, disabledColor, errorColor, successColor, selectedColor);
        change.accept(copy);
        return copy;
    }

    // ---- the themes there are, and which applies

    /** the themes that come with the library - e.g. to offer in a list */
    public static List<Theme> standard() {
        return STANDARD;
    }

    /** the theme for components with none from an ancestor or their app */
    public static Theme getDefault() {
        return defaultTheme;
    }

    /** the theme for components with none from an ancestor or their app - normally set once, at start-up */
    public static void setDefault(Theme theme) {
        defaultTheme = theme == null ? LEGACY : theme;
        changed();
    }

    /** goes up whenever which theme applies may have changed: a component looks again when it differs from what it saw */
    public static int version() {
        return version;
    }

    /** a theme was set: every component that has looked one up looks again, and draws again, on its next draw */
    public static synchronized void changed() {
        version++;
    }

    /**
     * The theme for component: the nearest set on it or an ancestor (HasOwnTheme), else its app's, else
     * the default. A walk up the tree: components keep the answer (getTheme) until version changes.
     */
    public static Theme lookUp(Component<?> component) {
        Object at = component;
        while (at != null) {
            if (at instanceof HasOwnTheme) {
                Theme own = ((HasOwnTheme) at).getOwnTheme();
                if (own != null) {
                    return own;
                }
            }
            at = at instanceof Component ? ((Component<?>) at).getParentContainer() : null;
        }
        TUIApp app = TUIApp.of(component);
        if (app != null && app.getTheme() != null) {
            return app.getTheme();
        }
        return defaultTheme;
    }

    /** marks component and everything it holds dirty: drawn again, in the colours that now apply */
    public static void repaint(Component<?> component) {
        component.markAsDirty();
        for (Component<?> child : new HasChildrenAdapter.Simple<Component<?>>().asHasChildren(component).getChildren()) {
            repaint(child);
        }
    }

    /** component moved: it and what it holds forget the theme they found - they look again, from where they are now */
    public static void forget(Component<?> component) {
        if (component instanceof HasOwnTheme) {
            ((HasOwnTheme) component).forgetTheme();
        }
        for (Component<?> child : new HasChildrenAdapter.Simple<Component<?>>().asHasChildren(component).getChildren()) {
            forget(child);
        }
    }

    /** a component that can carry a theme for itself and what it holds */
    public interface HasOwnTheme {
        /** the theme set on this component itself; null: none (its ancestors', app's or the default applies) */
        Theme getOwnTheme();

        /** forget the theme it found: it looks again on next use (it moved) */
        void forgetTheme();
    }
}
