
package mizzen.components.theme;

import static mizzen.components.theme.Theme.rgb;
import static mizzen.components.theme.Theme.rgbTheme;

final class RetroPhosphorTheme {
    private RetroPhosphorTheme() {}

    private static final int BLACK_GREEN = 0x101510;
    private static final int DARK_GREEN = 0x172417;
    private static final int FOREST_GREEN = 0x1C301E;
    private static final int DEEP_GREEN = 0x244027;
    private static final int MUTED_GREEN = 0x567A56;
    private static final int SAGE_GREEN = 0xA3C9A3;
    private static final int PALE_GREEN = 0xC5FFC5;
    private static final int PHOSPHOR_GREEN = 0x8DFF8D;
    private static final int BRIGHT_GREEN = 0x5BE66C;
    private static final int CYAN_GREEN = 0x70E8B4;
    private static final int YELLOW_GREEN = 0xD6E878;
    private static final int AMBER_GREEN = 0xC4B85B;
    private static final int ORANGE = 0xF0A45A;
    private static final int SOFT_RED = 0xF08080;

    static Theme create() {
        return rgbTheme("Retro Phosphor", BLACK_GREEN,
                SAGE_GREEN, PHOSPHOR_GREEN,
                PALE_GREEN, DARK_GREEN,
                PALE_GREEN, FOREST_GREEN,
                BLACK_GREEN, PHOSPHOR_GREEN,
                MUTED_GREEN, MUTED_GREEN,
                SOFT_RED, BRIGHT_GREEN,
                PALE_GREEN, DEEP_GREEN)
                .documents(BLACK_GREEN, CYAN_GREEN, SAGE_GREEN,
                        PALE_GREEN, YELLOW_GREEN, PHOSPHOR_GREEN,
                        AMBER_GREEN, FOREST_GREEN, MUTED_GREEN)
                .withStatusColor(rgb(CYAN_GREEN, BLACK_GREEN))
                .withWarningColor(rgb(ORANGE, BLACK_GREEN))
                .withDialogColor(rgb(SAGE_GREEN, FOREST_GREEN))
                .withSurfaceColor(rgb(SAGE_GREEN, DARK_GREEN))
                .withAccentColor(rgb(PHOSPHOR_GREEN, BLACK_GREEN))
                .withStripeColor(rgb(SAGE_GREEN, DARK_GREEN))
                .drawing(BLACK_GREEN, CYAN_GREEN, YELLOW_GREEN,
                        PHOSPHOR_GREEN, BRIGHT_GREEN, SOFT_RED,
                        SAGE_GREEN, ORANGE, MUTED_GREEN)
                .dialogs(d -> d.withInputColor(rgb(PALE_GREEN, BLACK_GREEN)));
    }
}
