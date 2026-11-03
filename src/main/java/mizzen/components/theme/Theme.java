package mizzen.components.theme;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import mizzen.app.TUIApp;
import mizzen.components.Component;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.controlcodes.color.Colors.ColorFromCodes;
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
 * app's (TUIApp.setTheme) - or, for an app with none that runs inside another (TUIApp.runEmbedded), that
 * one's - else the default (Theme.setDefault). The colours carry the theme's background,
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
            Colors.DEFAULT_DISABLED_COLOR, Colors.BIRed, Colors.IGre, new Colors.FGBGColors(Colors.IYel))
            .withSubheadingColor(Colors.BCya).withMinorHeadingColor(Colors.BGre).withStrongColor(Colors.BWhi)
            .withEmphasisColor(Colors.Cya).withLinkColor(Colors.UCya).withCodeColor(Colors.Yel).withDimColor(Colors.IBla);

    // The themes below are 24-bit colour (most terminals today: PuTTY 0.71+, xterm, Windows Terminal, iTerm2). Each is low
    // contrast between its blocks: a background that is not pure black, white or blue; fields one step off it; buttons
    // two steps; one accent (headings, the focused button). Colours are hex: 0xRRGGBB - tweak here.

    /** charcoal: soft light text, blue accent, fields and buttons a shade or two lighter than the background */
    public static final Theme DARK = rgbTheme("Dark", 0x22272E,       // background
            0xADBAC7, 0x6CB6FF,                                          // text, heading
            0xCDD9E5, 0x2D333B,                                          // input: text on field
            0xCDD9E5, 0x373E47,                                          // button
            0xFFFFFF, 0x316DCA,                                          // focused button: the accent
            0x545D68, 0x636E7B,                                          // border, disabled
            0xF47067, 0x8DDB8C,                                          // error, success
            0xCDD9E5, 0x2E4A6E)                                          // selected: text on row
            .documents(0x22272E, 0x96D0FF, 0xCDD9E5, 0xCDD9E5, 0xDCBDFB, 0x6CB6FF, 0xF69D50, 0x2D333B, 0x768390);

    /** paper: dark text on off-white, blue accent, fields and buttons a shade or two darker */
    public static final Theme LIGHT = rgbTheme("Light", 0xF6F8FA,
            0x24292F, 0x0969DA,
            0x24292F, 0xEAEEF2,
            0x24292F, 0xD8DEE4,
            0xFFFFFF, 0x0969DA,
            0xAFB8C1, 0x8C959F,
            0xCF222E, 0x1A7F37,
            0x24292F, 0xDDF4FF)
            .documents(0xF6F8FA, 0x0550AE, 0x24292F, 0x1F2328, 0x8250DF, 0x0969DA, 0x953800, 0xEAEEF2, 0x6E7781);

    /** the classic blue screen, softened: navy, warm yellow headings, fields a lighter blue, green buttons */
    public static final Theme CLASSIC = rgbTheme("Classic", 0x1D3268,
            0xD7DCE8, 0xF2D06B,
            0xFFFFFF, 0x2A4585,
            0xFFFFFF, 0x2F7A5E,
            0xFFFFFF, 0x3C9A76,
            0x7A8CC4, 0x7F8BB0,
            0xFF8A80, 0x9BE29B,
            0xFFFFFF, 0x34539E)
            .documents(0x1D3268, 0x9FD3FF, 0xFFFFFF, 0xFFFFFF, 0xC9B8FF, 0x8FD6FF, 0xF2D06B, 0x2A4585, 0x8E9CC9);

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

    // documents (markdown, help text): each falls back to a colour above when a theme leaves it out
    private Color subheadingColor;      // a level 2 heading, list markers
    private Color minorHeadingColor;    // level 3 and below
    private Color strongColor;          // **bold**
    private Color emphasisColor;        // *italic*
    private Color linkColor;
    private Color codeColor;            // `code`
    private Color dimColor;             // quotes, rules, the lines of a table in a document

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

    // a theme in 24-bit colour: the plain colours (text, heading, border...) on its background; fields, buttons and the
    // selected row on their own
    private static Theme rgbTheme(String name, int background, int text, int heading, int inputText, int inputField,
                                  int buttonText, int button, int focusedText, int focused, int border, int disabled,
                                  int error, int success, int selectedText, int selected) {
        return new Theme(name, rgb(text, background), rgb(text, background), bold(heading, background),
                rgb(inputText, inputField), rgb(buttonText, button), rgb(focusedText, focused), rgb(border, background),
                rgb(disabled, background), rgb(error, background), rgb(success, background), rgb(selectedText, selected));
    }

    // the document colours, on background - inline code on its own field
    private Theme documents(int background, int subheading, int minorHeading, int strong, int emphasis, int link,
                            int code, int codeField, int dim) {
        return withSubheadingColor(bold(subheading, background)).withMinorHeadingColor(bold(minorHeading, background))
                .withStrongColor(bold(strong, background)).withEmphasisColor(rgb(emphasis, background))
                .withLinkColor(rgb(link, background)).withCodeColor(rgb(code, codeField)).withDimColor(rgb(dim, background));
    }

    // as rgb, in bold - an RGB colour is only a colour; headings and **bold** need the attribute too
    private static Color bold(int foreground, int background) {
        return ColorFromCodes.instance(new int[] { 0, 1, 38, 2, foreground >> 16 & 0xFF, foreground >> 8 & 0xFF, foreground & 0xFF,
                                                   48, 2, background >> 16 & 0xFF, background >> 8 & 0xFF, background & 0xFF });
    }

    // foreground on background, each 0xRRGGBB - starting with a reset, as the named colours do, so nothing before it
    // (bold, underline) carries on into what is drawn in it
    private static Color rgb(int foreground, int background) {
        return ColorFromCodes.instance(new int[] { 0, 38, 2, foreground >> 16 & 0xFF, foreground >> 8 & 0xFF, foreground & 0xFF,
                                                   48, 2, background >> 16 & 0xFF, background >> 8 & 0xFF, background & 0xFF });
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
    public Color subheadingColor()     { return subheadingColor != null ? subheadingColor : headingColor; }
    public Color minorHeadingColor()   { return minorHeadingColor != null ? minorHeadingColor : headingColor; }
    public Color strongColor()         { return strongColor != null ? strongColor : textColor; }
    public Color emphasisColor()       { return emphasisColor != null ? emphasisColor : textColor; }
    public Color linkColor()           { return linkColor != null ? linkColor : headingColor; }
    public Color codeColor()           { return codeColor != null ? codeColor : textColor; }
    public Color dimColor()            { return dimColor != null ? dimColor : disabledColor; }

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
    public Theme withSubheadingColor(Color color)      { return with((t) -> t.subheadingColor = color); }
    public Theme withMinorHeadingColor(Color color)    { return with((t) -> t.minorHeadingColor = color); }
    public Theme withStrongColor(Color color)          { return with((t) -> t.strongColor = color); }
    public Theme withEmphasisColor(Color color)        { return with((t) -> t.emphasisColor = color); }
    public Theme withLinkColor(Color color)            { return with((t) -> t.linkColor = color); }
    public Theme withCodeColor(Color color)            { return with((t) -> t.codeColor = color); }
    public Theme withDimColor(Color color)             { return with((t) -> t.dimColor = color); }

    private Theme with(Consumer<Theme> change) {
        Theme copy = new Theme(name, backgroundColor, textColor, headingColor, inputColor, buttonColor, buttonFocusedColor,
                borderColor, disabledColor, errorColor, successColor, selectedColor);
        copy.subheadingColor = subheadingColor;
        copy.minorHeadingColor = minorHeadingColor;
        copy.strongColor = strongColor;
        copy.emphasisColor = emphasisColor;
        copy.linkColor = linkColor;
        copy.codeColor = codeColor;
        copy.dimColor = dimColor;
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
        // its app's theme; an app with none: the app it is embedded in (a desktop running examples), and so on outward
        TUIApp app = TUIApp.of(component);
        while (app != null) {
            if (app.getTheme() != null) {
                return app.getTheme();
            }
            Object panel = app.getMainApplicationPanel();
            Object outside = panel instanceof Component ? ((Component<?>) panel).getParentContainer() : null;
            app = outside instanceof Component ? TUIApp.of((Component<?>) outside) : null;
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
