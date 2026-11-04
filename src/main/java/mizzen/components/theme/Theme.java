package mizzen.components.theme;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import mizzen.app.TUIApp;
import mizzen.components.BlockChars;
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
            .withEmphasisColor(Colors.Cya).withLinkColor(Colors.UCya).withCodeColor(Colors.Yel).withDimColor(Colors.IBla)
            .withStatusColor(Colors.IGre).withWarningColor(Colors.Yel).withDialogColor(legacyDialogColor())
            .withDrawingColors(Colors.BCya, Colors.BYel, Colors.BPur, Colors.BGre, Colors.BRed, Colors.BBlu, Colors.Yel, Colors.IBla)
            .dialogs((d) -> d.withHeadingColor(new Colors.FGBGColors(Colors.Blu, Colors.On_Whi))   // BYel can't be read on white
                    .withLinkColor(new Colors.FGBGColors(Colors.Pur, Colors.On_Whi))
                    .withWarningColor(new Colors.FGBGColors(Colors.BIBla, Colors.On_Yel))   // filled: a yellow line is lost on the grey
                    .withErrorColor(new Colors.FGBGColors(Colors.BRed, Colors.On_Whi))
                    .withStrongColor(new Colors.FGBGColors(Colors.Bla, Colors.On_Whi))   // a form's labels: BWhi is lost on white
                    .withInputColor(new Colors.FGBGColors(Colors.Bla, Colors.On_IWhi))   // a white field on the grey
                    .withDrawingColors(onWhite(Colors.Cya), onWhite(Colors.Yel), onWhite(Colors.Pur), onWhite(Colors.Gre),
                                       onWhite(Colors.Red), onWhite(Colors.Blu), onWhite(Colors.Yel), onWhite(Colors.IBla)));

    // Drawing colours (charts, plots) keep one order of hues in every theme, each one readable on its background:
    //     0 cyan   1 yellow   2 purple   3 green   4 red   5 blue   6 orange   7 grey

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
            .documents(0x22272E, 0x96D0FF, 0xCDD9E5, 0xCDD9E5, 0xDCBDFB, 0x6CB6FF, 0xF69D50, 0x2D333B, 0x768390)
            .withStatusColor(rgb(0x6BC9A7, 0x22272E)).withWarningColor(rgb(0xDAAA3F, 0x22272E))
            .withDialogColor(rgb(0xADBAC7, 0x2D333B)).withStripeColor(rgb(0xADBAC7, 0x2C333D))
            .drawing(0x22272E, 0x76E3EA, 0xDAAA3F, 0xDCBDFB, 0x8DDB8C, 0xF47067, 0x6CB6FF, 0xF69D50, 0x909DAB)
            .dialogs((d) -> d.withInputColor(rgb(0xCDD9E5, 0x22272E)));    // fields sunk to the screen's shade: the dialog has theirs

    /** paper: dark text on off-white, blue accent, fields and buttons a shade or two darker */
    public static final Theme LIGHT = rgbTheme("Light", 0xF6F8FA,
            0x24292F, 0x0969DA,
            0x24292F, 0xEAEEF2,
            0x24292F, 0xE4E8EC,                                          // button: a light step - large bordered ones too
            0xFFFFFF, 0x0969DA,
            0xAFB8C1, 0x8C959F,
            0xCF222E, 0x1A7F37,
            0x24292F, 0xDDF4FF)
            .documents(0xF6F8FA, 0x0550AE, 0x24292F, 0x1F2328, 0x8250DF, 0x0969DA, 0x953800, 0xEAEEF2, 0x6E7781)
            .withStatusColor(rgb(0x1B7C83, 0xF6F8FA)).withWarningColor(rgb(0x9A6700, 0xF6F8FA))
            .withDialogColor(rgb(0x24292F, 0xFFFFFF)).withStripeColor(rgb(0x24292F, 0xE6EBF0))
            .drawing(0xF6F8FA, 0x0891B2, 0xCA8A04, 0x9333EA, 0x16A34A, 0xDC2626, 0x2563EB, 0xEA580C, 0x6B7280);   // strong: dots are thin

    /** the classic blue screen, softened: navy, warm yellow headings, fields a lighter blue, green buttons */
    public static final Theme CLASSIC = rgbTheme("Classic", 0x1D3268,
            0xD7DCE8, 0xF2D06B,
            0xFFFFFF, 0x2A4585,
            0xFFFFFF, 0x2B5E5A,                                          // button: a quieter green
            0xFFFFFF, 0x3C9A76,
            0x7A8CC4, 0x7F8BB0,
            0xFF8A80, 0x9BE29B,
            0xFFFFFF, 0x34539E)
            .documents(0x1D3268, 0x9FD3FF, 0xFFFFFF, 0xFFFFFF, 0xC9B8FF, 0x8FD6FF, 0xF2D06B, 0x2A4585, 0x8E9CC9)
            .withStatusColor(rgb(0x8FE3C2, 0x1D3268)).withWarningColor(rgb(0xFFB86B, 0x1D3268))
            .withDialogColor(rgb(0xD7DCE8, 0x2A4585)).withStripeColor(rgb(0xD7DCE8, 0x274283))
            .drawing(0x1D3268, 0x7FE3F0, 0xF2D06B, 0xC9B8FF, 0x9BE29B, 0xFF8A80, 0x8FB8FF, 0xFFB86B, 0xB4BED8)
            .dialogs((d) -> d.withInputColor(rgb(0xFFFFFF, 0x1D3268)));

    /**
     * Rounded corners and a neutral slate palette with an amber accent - a base to copy for an app's own look: take it,
     * change the colours (rgb), name it, register it.
     * <pre>
     *     Theme mine = Theme.ROUNDED.withName("Harbour")
     *             .withBackgroundColor(Theme.rgb(0xE6EEF2, 0x1D4F4A))
     *             .withSurfaceColor(Theme.rgb(0x22313A, 0xF4F7F8))
     *             .withAccentColor(Theme.rgb(0xF2C94C, 0x1D4F4A));
     *     Theme.register(mine);                    then theme=Harbour on the command line finds it
     * </pre>
     */
    public static final Theme ROUNDED = rgbTheme("Rounded", 0x1F2933,
            0xD9E2EC, 0x7BD3C8,                                          // text, heading (teal)
            0xF0F4F8, 0x2A3744,                                          // input
            0xF0F4F8, 0x3E5266,                                          // button
            0x102A43, 0xF0B429,                                          // focused button: the amber accent
            0x52606D, 0x7B8794,                                          // border, disabled
            0xF29B9B, 0x8EDB9B,                                          // error, success
            0xF0F4F8, 0x34495E)                                          // selected
            .documents(0x1F2933, 0x9FE0D8, 0xD9E2EC, 0xF0F4F8, 0xC3B5F5, 0x8BC4F9, 0xF7C978, 0x2A3744, 0x8796A5)
            .withStatusColor(rgb(0x8EDB9B, 0x1F2933)).withWarningColor(rgb(0xF0B429, 0x1F2933))
            .withSurfaceColor(rgb(0xD9E2EC, 0x2B3846)).withAccentColor(rgbBold(0xF0B429, 0x1F2933))
            .withStripeColor(rgb(0xD9E2EC, 0x354556))
            .drawing(0x1F2933, 0x6FD6E8, 0xF0C75E, 0xC3B5F5, 0x8EDB9B, 0xF29B9B, 0x8BC4F9, 0xF5A86B, 0x9AA5B1)
            .withBlockChars(BlockChars.ROUNDED).withFocusedBlockChars(BlockChars.ROUNDED).withButtonFocus(ButtonFocus.ACCENT)
            .dialogs((d) -> d.withInputColor(rgb(0xF0F4F8, 0x1F2933)));    // fields sunk to the screen's shade

    private static final List<Theme> STANDARD = Collections.unmodifiableList(Arrays.asList(LEGACY, DARK, LIGHT, CLASSIC, ROUNDED));
    private static final List<Theme> REGISTERED = new java.util.concurrent.CopyOnWriteArrayList<Theme>();

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
    private Color statusColor;          // what just happened: a status line, a result
    private Color selectedColor;        // the picked row of a list, tree or table
    private Color warningColor;         // take care - not yet an error
    private Color dialogColor;          // a dialog's panel: a surface raised off the background (none: surfaceColor)
    private Color surfaceColor;         // a card's or a dialog's panel: raised off the background
    private Color accentColor;          // the one bright colour: focus, a brand mark
    private Color stripeColor;          // every other row of a striped table; null: no stripes

    // the theme inside a dialog: this one laid on dialogColor (onSurface), then changed as the theme says (dialogs)
    private Function<Theme, Theme> dialogChanges = Function.identity();
    private Theme dialogTheme;
    private Theme surfaceTheme;                                   // worked out once (surfaceTheme)

    // documents (markdown, help text): each falls back to a colour above when a theme leaves it out
    private Color subheadingColor;      // a level 2 heading, list markers
    private Color minorHeadingColor;    // level 3 and below
    private Color strongColor;          // **bold**
    private Color emphasisColor;        // *italic*
    private Color linkColor;
    private Color codeColor;            // `code`
    private Color dimColor;             // quotes, rules, the lines of a table in a document

    private List<Color> drawingColors;  // charts and plots: series i in drawingColor(i)

    /** how a focused button shows: BRACKETS [OK] (the standard themes), or ACCENT - in the focused-button colour, no brackets */
    public enum ButtonFocus { BRACKETS, ACCENT }

    private ButtonFocus buttonFocus;     // null: BRACKETS
    private BlockChars blockChars;       // lines and corners of boxes, borders, tables; null: BlockChars.NICE
    private BlockChars focusedBlockChars;// a focused box's; null: BlockChars.NICE_DOUBLE

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
        return new Theme(name, rgb(text, background), rgb(text, background), rgbBold(heading, background),
                rgb(inputText, inputField), rgb(buttonText, button), rgb(focusedText, focused), rgb(border, background),
                rgb(disabled, background), rgb(error, background), rgb(success, background), rgb(selectedText, selected));
    }

    // the document colours, on background - inline code on its own field
    private Theme documents(int background, int subheading, int minorHeading, int strong, int emphasis, int link,
                            int code, int codeField, int dim) {
        return withSubheadingColor(rgbBold(subheading, background)).withMinorHeadingColor(rgbBold(minorHeading, background))
                .withStrongColor(rgbBold(strong, background)).withEmphasisColor(rgb(emphasis, background))
                .withLinkColor(rgb(link, background)).withCodeColor(rgb(code, codeField)).withDimColor(rgb(dim, background));
    }

    // the drawing colours, each on background
    private Theme drawing(int background, int... hues) {
        Color[] colors = new Color[hues.length];
        for (int i = 0; i < hues.length; i++) {
            colors[i] = rgb(hues[i], background);
        }
        return withDrawingColors(colors);
    }

    private static Color onWhite(Color color) {
        return new Colors.FGBGColors((Colors.FgColor) color, Colors.On_Whi);
    }

    /**
     * As rgb, in bold - an RGB colour is only a colour; headings and **bold** need the attribute too.
     * <pre>
     *     Theme.rgbBold(0xF2C94C, 0x1D4F4A)        bold yellow on teal
     * </pre>
     */
    public static Color rgbBold(int foreground, int background) {
        return ColorFromCodes.instance(new int[] { 0, 1, 38, 2, foreground >> 16 & 0xFF, foreground >> 8 & 0xFF, foreground & 0xFF,
                                                   48, 2, background >> 16 & 0xFF, background >> 8 & 0xFF, background & 0xFF });
    }

    /**
     * A colour from 24-bit values - foreground on background, each 0xRRGGBB - as the themes here are made. It starts with a
     * reset, as the named colours do, so nothing before it (bold, underline) carries on into what is drawn in it.
     * <pre>
     *     Theme.rgb(0xE6EEF2, 0x1D4F4A)            near-white on teal
     * </pre>
     */
    public static Color rgb(int foreground, int background) {
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
    public Color statusColor()         { return statusColor != null ? statusColor : successColor; }
    public Color selectedColor()       { return selectedColor; }
    public Color warningColor()        { return warningColor != null ? warningColor : errorColor; }
    public Color dialogColor()         { return dialogColor != null ? dialogColor : surfaceColor(); }
    public Color surfaceColor()        { return surfaceColor != null ? surfaceColor : backgroundColor; }
    public Color accentColor()         { return accentColor != null ? accentColor : headingColor; }
    public Color stripeColor()         { return stripeColor; }
    public Color subheadingColor()     { return subheadingColor != null ? subheadingColor : headingColor; }
    public Color minorHeadingColor()   { return minorHeadingColor != null ? minorHeadingColor : headingColor; }
    public Color strongColor()         { return strongColor != null ? strongColor : textColor; }
    public Color emphasisColor()       { return emphasisColor != null ? emphasisColor : textColor; }
    public Color linkColor()           { return linkColor != null ? linkColor : headingColor; }
    public Color codeColor()           { return codeColor != null ? codeColor : textColor; }
    public Color dimColor()            { return dimColor != null ? dimColor : disabledColor; }

    /**
     * The colours to draw data in - a chart's series, a plot's shapes - in one order of hues in every theme, each readable
     * on this theme's background: 0 cyan, 1 yellow, 2 purple, 3 green, 4 red, 5 blue, 6 orange, 7 grey. A chart gives
     * its series or slices these in turn when they are given no colour; a drawing can pick one by its number:
     * <pre>
     *     chart.series("cpu").series("memory");               cyan, then yellow
     *     pen.drawingColor(4).draw(mars);                     a red, in any theme
     * </pre>
     */
    public List<Color> drawingColors() {
        return drawingColors != null ? drawingColors
                : Collections.unmodifiableList(Arrays.asList(headingColor, linkColor(), successColor, errorColor, warningColor(), textColor));
    }

    /**
     * The characters lines and boxes are drawn with - borders, frames, tables, a tree's branches - unless a component was
     * given its own (setBlockChars). focusedBlockChars: a focused box's (by default double lines).
     * <pre>
     *     Theme.DARK.withBlockChars(BlockChars.ROUNDED).withFocusedBlockChars(BlockChars.ROUNDED)    ╭──╮ boxes
     * </pre>
     */
    /** how a focused button shows (Button): BRACKETS, or ACCENT - buttonFocusedColor, or an outlined button's accentColor */
    public ButtonFocus buttonFocus() {
        return buttonFocus != null ? buttonFocus : ButtonFocus.BRACKETS;
    }

    public BlockChars blockChars() {
        return blockChars != null ? blockChars : BlockChars.NICE;
    }

    public BlockChars focusedBlockChars() {
        return focusedBlockChars != null ? focusedBlockChars : BlockChars.NICE_DOUBLE;
    }

    /** drawing colour i, counting round: drawingColor(9) is drawingColor(1) when there are 8 */
    public Color drawingColor(int i) {
        List<Color> colors = drawingColors();
        return colors.get(Math.floorMod(i, colors.size()));
    }

    /**
     * The theme inside a dialog: this one, with dialogColor as its background and every colour that is drawn on the
     * background (text, headings, borders, errors...) laid on dialogColor instead. Fields, buttons and the selected row
     * keep their own colours; one with none of its own (Legacy) takes dialogColor.
     * <p/>
     * A dialog's panel takes it, so whatever goes in a dialog sits on the dialog's colour:
     * <pre>
     *     new TextComponent("Saved.")          text colour, on the dialog's background
     * </pre>
     * A theme may change it further - Legacy's headings are blue in a dialog, as its yellow can't be read on white.
     */
    /**
     * The theme on a card (Card): this one, with surfaceColor as its background and everything drawn on the background
     * laid on surfaceColor instead - as dialogTheme does for a dialog. A theme with no surface of its own: itself (a card
     * is then a bordered panel on the background).
     */
    public Theme surfaceTheme() {
        if (surfaceColor == null) {
            return this;
        }
        if (surfaceTheme == null) {
            surfaceTheme = onSurface(surfaceColor);
        }
        return surfaceTheme;
    }

    public Theme dialogTheme() {
        if (dialogTheme == null) {
            dialogTheme = dialogChanges.apply(onSurface(dialogColor()));
        }
        return dialogTheme;
    }

    // this theme on surface: the colours drawn on the background take surface's background; the rest stay, unless none
    private Theme onSurface(Color surface) {
        Theme on = with((t) -> {
            t.backgroundColor = surface;
            t.textColor = laidOn(textColor, surface);
            t.headingColor = laidOn(headingColor, surface);
            t.borderColor = laidOn(borderColor, surface);
            t.disabledColor = laidOn(disabledColor, surface);
            t.errorColor = laidOn(errorColor, surface);
            t.successColor = laidOn(successColor, surface);
            t.statusColor = laidOn(statusColor, surface);
            t.warningColor = laidOn(warningColor, surface);
            t.accentColor = laidOn(accentColor, surface);
            t.subheadingColor = laidOn(subheadingColor, surface);
            t.minorHeadingColor = laidOn(minorHeadingColor, surface);
            t.strongColor = laidOn(strongColor, surface);
            t.emphasisColor = laidOn(emphasisColor, surface);
            t.linkColor = laidOn(linkColor, surface);
            t.dimColor = laidOn(dimColor, surface);
            if (drawingColors != null) {
                Color[] drawing = new Color[drawingColors.size()];
                for (int i = 0; i < drawing.length; i++) {
                    drawing[i] = laidOn(drawingColors.get(i), surface);
                }
                t.drawingColors = Collections.unmodifiableList(Arrays.asList(drawing));
            }
            t.inputColor = orSurface(inputColor, surface);
            t.buttonColor = orSurface(buttonColor, surface);
            t.buttonFocusedColor = orSurface(buttonFocusedColor, surface);
            t.selectedColor = orSurface(selectedColor, surface);
            t.codeColor = orSurface(codeColor, surface);
            t.dialogChanges = Function.identity();
        });
        return on;
    }

    // color's foreground on surface's background; no colour (left out, or none of its own): surface itself
    private static Color laidOn(Color color, Color surface) {
        return color == null || color == Colors.DEFAULT_COLOR ? surface : color.mergeWith(surface);
    }

    private static Color orSurface(Color color, Color surface) {
        return color == null || color == Colors.DEFAULT_COLOR ? surface : color;
    }

    // Legacy's dialog: black on white; bold, or the focused button, white on black
    private static Color legacyDialogColor() {
        Color color = new Colors.FGBGColors(Colors.Bla, Colors.On_Whi);
        color.setBoldTransform((c) -> new Colors.FGBGColors(Colors.BIWhi, Colors.On_Bla));
        color.setInvertTransform((c) -> new Colors.FGBGColors(Colors.BIWhi, Colors.On_Bla));
        return color;
    }

    // how this theme's dialogs differ from it laid on dialogColor
    private Theme dialogs(Function<Theme, Theme> changes) {
        return with((t) -> t.dialogChanges = changes);
    }

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
    public Theme withStatusColor(Color color)          { return with((t) -> t.statusColor = color); }
    public Theme withSelectedColor(Color color)        { return with((t) -> t.selectedColor = color); }
    public Theme withWarningColor(Color color)         { return with((t) -> t.warningColor = color); }
    public Theme withDialogColor(Color color)          { return with((t) -> t.dialogColor = color); }
    public Theme withSurfaceColor(Color color)         { return with((t) -> t.surfaceColor = color); }
    public Theme withAccentColor(Color color)          { return with((t) -> t.accentColor = color); }
    public Theme withStripeColor(Color color)          { return with((t) -> t.stripeColor = color); }
    public Theme withButtonFocus(ButtonFocus focus)    { return with((t) -> t.buttonFocus = focus); }
    public Theme withBlockChars(BlockChars chars)      { return with((t) -> t.blockChars = chars); }
    public Theme withFocusedBlockChars(BlockChars chars) { return with((t) -> t.focusedBlockChars = chars); }
    public Theme withDrawingColors(Color... colors)    { return with((t) -> t.drawingColors = Collections.unmodifiableList(Arrays.asList(colors.clone()))); }
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
        copy.statusColor = statusColor;
        copy.warningColor = warningColor;
        copy.dialogColor = dialogColor;
        copy.surfaceColor = surfaceColor;
        copy.accentColor = accentColor;
        copy.stripeColor = stripeColor;
        copy.drawingColors = drawingColors;
        copy.blockChars = blockChars;
        copy.buttonFocus = buttonFocus;
        copy.focusedBlockChars = focusedBlockChars;
        copy.dialogChanges = dialogChanges;
        change.accept(copy);
        return copy;
    }

    // ---- the themes there are, and which applies

    /** the themes that come with the library - e.g. to offer in a list */
    public static List<Theme> standard() {
        return STANDARD;
    }

    /** the theme with that name, case aside - the standard ones, then those registered (theme=Dark); null: none */
    public static Theme named(String name) {
        for (Theme theme : all()) {
            if (theme.name().equalsIgnoreCase(name)) {
                return theme;
            }
        }
        return null;
    }

    /**
     * An app's own theme, found by its name from now on (named, theme=NAME on the command line) and offered with the
     * standard ones (all). Registering a theme of the same name again replaces it.
     */
    public static void register(Theme theme) {
        REGISTERED.removeIf((t) -> t.name().equalsIgnoreCase(theme.name()));
        REGISTERED.add(theme);
    }

    /** the standard themes, then the ones registered - e.g. to offer in a list */
    public static List<Theme> all() {
        List<Theme> all = new java.util.ArrayList<Theme>(STANDARD);
        all.addAll(REGISTERED);
        return Collections.unmodifiableList(all);
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
