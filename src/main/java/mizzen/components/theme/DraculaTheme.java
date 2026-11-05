
package mizzen.components.theme;

import static mizzen.components.theme.Theme.rgb;
import static mizzen.components.theme.Theme.rgbTheme;

final class DraculaTheme {
    private DraculaTheme() {}

    private static final int DARK_CHARCOAL = 0x282A36;
    private static final int SLATE = 0x44475A;
    private static final int PALE_WHITE = 0xF8F8F2;
    private static final int MUTED_BLUE_GREY = 0x6272A4;
    private static final int CYAN = 0x8BE9FD;
    private static final int GREEN = 0x50FA7B;
    private static final int ORANGE = 0xFFB86C;
    private static final int PINK = 0xFF79C6;
    private static final int PURPLE = 0xBD93F9;
    private static final int RED = 0xFF5555;
    private static final int YELLOW = 0xF1FA8C;
    private static final int BLACK = 0x000000;

    static Theme create() {
        return rgbTheme("Dracula", DARK_CHARCOAL,
                PALE_WHITE, PURPLE,
                PALE_WHITE, SLATE,
                PALE_WHITE, SLATE,
                BLACK, PURPLE,
                MUTED_BLUE_GREY, MUTED_BLUE_GREY,
                RED, GREEN,
                PALE_WHITE, MUTED_BLUE_GREY)
                .documents(DARK_CHARCOAL, CYAN, PURPLE,
                        PALE_WHITE, PINK, CYAN,
                        ORANGE, SLATE, MUTED_BLUE_GREY)
                .withStatusColor(rgb(GREEN, DARK_CHARCOAL))
                .withWarningColor(rgb(YELLOW, DARK_CHARCOAL))
                .withDialogColor(rgb(PALE_WHITE, SLATE))
                .withSurfaceColor(rgb(PALE_WHITE, SLATE))
                .withAccentColor(rgb(PURPLE, DARK_CHARCOAL))
                .withStripeColor(rgb(PALE_WHITE, SLATE))
                .drawing(DARK_CHARCOAL, CYAN, YELLOW, PURPLE,
                        GREEN, RED, MUTED_BLUE_GREY, ORANGE, PINK)
                .dialogs(d -> d.withInputColor(rgb(PALE_WHITE, DARK_CHARCOAL)));
    }
}
