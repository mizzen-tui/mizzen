
package mizzen.components.theme;

import static mizzen.components.theme.Theme.rgb;
import static mizzen.components.theme.Theme.rgbTheme;

final class SolarizedDarkTheme {
    private SolarizedDarkTheme() {}

    private static final int DEEP_TEAL = 0x002B36;
    private static final int DARK_TEAL = 0x073642;
    private static final int SLATE_GREY = 0x586E75;
    private static final int BLUE_GREY = 0x657B83;
    private static final int LIGHT_BLUE_GREY = 0x839496;
    private static final int PALE_BLUE_GREY = 0x93A1A1;
    private static final int CREAM = 0xEEE8D5;
    private static final int BLUE = 0x268BD2;
    private static final int CYAN = 0x2AA198;
    private static final int GREEN = 0x859900;
    private static final int YELLOW = 0xB58900;
    private static final int ORANGE = 0xCB4B16;
    private static final int RED = 0xDC322F;
    private static final int MAGENTA = 0xD33682;
    private static final int VIOLET = 0x6C71C4;

    static Theme create() {
        return rgbTheme("Solarized Dark", DEEP_TEAL,
                PALE_BLUE_GREY, YELLOW,
                CREAM, DARK_TEAL,
                CREAM, SLATE_GREY,
                DEEP_TEAL, CYAN,
                BLUE_GREY, LIGHT_BLUE_GREY,
                RED, GREEN,
                CREAM, DARK_TEAL)
                .documents(DEEP_TEAL, BLUE, PALE_BLUE_GREY,
                        CREAM, VIOLET, CYAN,
                        ORANGE, DARK_TEAL, LIGHT_BLUE_GREY)
                .withStatusColor(rgb(CYAN, DEEP_TEAL))
                .withWarningColor(rgb(YELLOW, DEEP_TEAL))
                .withDialogColor(rgb(PALE_BLUE_GREY, DARK_TEAL))
                .withSurfaceColor(rgb(PALE_BLUE_GREY, DARK_TEAL))
                .withAccentColor(rgb(YELLOW, DEEP_TEAL))
                .withStripeColor(rgb(PALE_BLUE_GREY, DARK_TEAL))
                .drawing(DEEP_TEAL, CYAN, YELLOW, VIOLET,
                        GREEN, RED, BLUE, ORANGE, MAGENTA)
                .dialogs(d -> d.withInputColor(rgb(CREAM, DEEP_TEAL)));
    }
}
