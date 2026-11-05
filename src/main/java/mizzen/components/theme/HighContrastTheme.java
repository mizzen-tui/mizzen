
package mizzen.components.theme;

import static mizzen.components.theme.Theme.rgb;
import static mizzen.components.theme.Theme.rgbTheme;

final class HighContrastTheme {
    private HighContrastTheme() {}

    private static final int BLACK = 0x000000;
    private static final int WHITE = 0xFFFFFF;
    private static final int DARK_GREY = 0x303030;
    private static final int LIGHT_GREY = 0xC0C0C0;
    private static final int YELLOW = 0xFFFF00;
    private static final int GREEN = 0x00FF00;
    private static final int RED = 0xFF5555;
    private static final int CYAN = 0x00FFFF;
    private static final int BLUE = 0x5599FF;
    private static final int MAGENTA = 0xFF55FF;
    private static final int ORANGE = 0xFFAA00;

    static Theme create() {
        return rgbTheme("High Contrast", BLACK,
                WHITE, YELLOW,
                WHITE, DARK_GREY,
                WHITE, DARK_GREY,
                BLACK, YELLOW,
                WHITE, LIGHT_GREY,
                RED, GREEN,
                BLACK, CYAN)
                .documents(BLACK, CYAN, YELLOW,
                        WHITE, MAGENTA, CYAN,
                        ORANGE, DARK_GREY, LIGHT_GREY)
                .withStatusColor(rgb(GREEN, BLACK))
                .withWarningColor(rgb(YELLOW, BLACK))
                .withDialogColor(rgb(WHITE, BLACK))
                .withSurfaceColor(rgb(WHITE, DARK_GREY))
                .withAccentColor(rgb(YELLOW, BLACK))
                .withStripeColor(rgb(WHITE, DARK_GREY))
                .drawing(BLACK, CYAN, YELLOW, MAGENTA,
                        GREEN, RED, BLUE, ORANGE, LIGHT_GREY);
    }
}
