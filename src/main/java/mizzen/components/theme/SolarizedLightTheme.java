
package mizzen.components.theme;

import static mizzen.components.theme.Theme.rgb;
import static mizzen.components.theme.Theme.rgbTheme;

final class SolarizedLightTheme {
    private SolarizedLightTheme() {}

    private static final int WARM_CREAM = 0xFDF6E3;
    private static final int PALE_CREAM = 0xEEE8D5;
    private static final int WHITE = 0xFFFFFF;
    private static final int DARK_TEAL = 0x073642;
    private static final int DEEP_TEAL = 0x002B36;                // Solarized's darkest base (base03)
    private static final int SLATE_GREY = 0x586E75;
    private static final int BLUE_GREY = 0x657B83;
    private static final int LIGHT_BLUE_GREY = 0x93A1A1;
    private static final int BLUE = 0x268BD2;
    private static final int CYAN = 0x2AA198;
    private static final int GREEN = 0x859900;
    private static final int YELLOW = 0xB58900;
    private static final int ORANGE = 0xCB4B16;
    private static final int RED = 0xDC322F;
    private static final int MAGENTA = 0xD33682;
    private static final int VIOLET = 0x6C71C4;

    static Theme create() {
        return rgbTheme("Solarized Light", WARM_CREAM,
                SLATE_GREY, BLUE,
                DARK_TEAL, PALE_CREAM,
                DARK_TEAL, PALE_CREAM,
                WHITE, BLUE,
                LIGHT_BLUE_GREY, BLUE_GREY,
                RED, GREEN,
                WARM_CREAM, BLUE)               // selected: its accent - PALE_CREAM was the fields' own colour, so nothing showed
                .documents(WARM_CREAM, BLUE, BLUE_GREY,
                        DARK_TEAL, VIOLET, CYAN,
                        ORANGE, PALE_CREAM, LIGHT_BLUE_GREY)
                .withStatusColor(rgb(CYAN, WARM_CREAM))
                .withWarningColor(rgb(YELLOW, WARM_CREAM))
                .withDialogColor(rgb(DARK_TEAL, WHITE))
                .withSurfaceColor(rgb(SLATE_GREY, PALE_CREAM))
                .withAccentColor(rgb(BLUE, WARM_CREAM))
                .withStripeColor(rgb(SLATE_GREY, PALE_CREAM))
                .withImageBackingColor(rgb(WARM_CREAM, DEEP_TEAL))   // pictures in Solarized's dark base: light pixels show
                .drawing(WARM_CREAM, CYAN, YELLOW, VIOLET,
                        GREEN, RED, BLUE, ORANGE, MAGENTA);
    }
}
