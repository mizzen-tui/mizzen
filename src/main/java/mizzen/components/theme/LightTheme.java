
package mizzen.components.theme;

import static mizzen.components.theme.Theme.rgb;
import static mizzen.components.theme.Theme.rgbTheme;

final class LightTheme {
    private LightTheme() {}

    // Palette: colors used by this theme
    private static final int OFF_WHITE = 0xF6F8FA;
    private static final int WHITE = 0xFFFFFF;
    private static final int CHARCOAL = 0x24292F;
    private static final int NEAR_BLACK = 0x1F2328;
    private static final int PALE_GREY = 0xEAEEF2;
    private static final int LIGHT_GREY = 0xE4E8EC;
    private static final int COOL_LIGHT_GREY = 0xE6EBF0;
    private static final int SILVER_GREY = 0xAFB8C1;
    private static final int MEDIUM_GREY = 0x8C959F;
    private static final int SLATE_GREY = 0x6E7781;
    private static final int DARK_SLATE_GREY = 0x6B7280;
    private static final int BLUE = 0x0969DA;
    private static final int DARK_BLUE = 0x0550AE;
    private static final int PALE_BLUE = 0xDDF4FF;
    private static final int CYAN = 0x0891B2;
    private static final int TEAL = 0x1B7C83;
    private static final int PURPLE = 0x8250DF;
    private static final int VIOLET = 0x9333EA;
    private static final int DARK_ORANGE = 0x953800;
    private static final int AMBER = 0x9A6700;
    private static final int GOLD = 0xCA8A04;
    private static final int ORANGE = 0xEA580C;
    private static final int RED = 0xCF222E;
    private static final int BRIGHT_RED = 0xDC2626;
    private static final int DARK_GREEN = 0x1A7F37;
    private static final int GREEN = 0x16A34A;
    private static final int BRIGHT_BLUE = 0x2563EB;

    static Theme create() {
        return rgbTheme("Light", OFF_WHITE,
                CHARCOAL, BLUE,
                CHARCOAL, PALE_GREY,
                CHARCOAL, LIGHT_GREY,
                WHITE, BLUE,
                SILVER_GREY, MEDIUM_GREY,
                RED, DARK_GREEN,
                CHARCOAL, PALE_BLUE)
                .documents(OFF_WHITE, DARK_BLUE, CHARCOAL, NEAR_BLACK,
                        PURPLE, BLUE, DARK_ORANGE, PALE_GREY, SLATE_GREY)
                .withStatusColor(rgb(TEAL, OFF_WHITE))
                .withWarningColor(rgb(AMBER, OFF_WHITE))
                .withDialogColor(rgb(CHARCOAL, WHITE))
                .withStripeColor(rgb(CHARCOAL, COOL_LIGHT_GREY))
                .drawing(OFF_WHITE, CYAN, GOLD, VIOLET, GREEN,
                        BRIGHT_RED, BRIGHT_BLUE, ORANGE, DARK_SLATE_GREY);
    }
}
