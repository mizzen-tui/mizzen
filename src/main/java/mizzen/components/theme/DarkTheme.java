
package mizzen.components.theme;

import static mizzen.components.theme.Theme.rgb;
import static mizzen.components.theme.Theme.rgbTheme;

final class DarkTheme {
    private DarkTheme() {}

    // Palette: names describe the actual colours, not their roles.
    private static final int DARK_CHARCOAL = 0x22272E;
    private static final int CHARCOAL = 0x2D333B;
    private static final int DARK_SLATE = 0x2C333D;
    private static final int SLATE = 0x373E47;
    private static final int DARK_BLUE = 0x2E4A6E;
    private static final int BLUE = 0x316DCA;
    private static final int SKY_BLUE = 0x6CB6FF;
    private static final int PALE_BLUE = 0x96D0FF;
    private static final int BRIGHT_CYAN = 0x76E3EA;
    private static final int LIGHT_BLUE_GREY = 0xCDD9E5;
    private static final int SILVER_BLUE_GREY = 0xADBAC7;
    private static final int SLATE_GREY = 0x545D68;
    private static final int MEDIUM_SLATE_GREY = 0x636E7B;
    private static final int MUTED_BLUE_GREY = 0x768390;
    private static final int LIGHT_SLATE_GREY = 0x909DAB;
    private static final int WHITE = 0xFFFFFF;
    private static final int LAVENDER = 0xDCBDFB;
    private static final int CORAL_RED = 0xF47067;
    private static final int LIGHT_GREEN = 0x8DDB8C;
    private static final int SEA_GREEN = 0x6BC9A7;
    private static final int GOLD = 0xDAAA3F;
    private static final int SOFT_ORANGE = 0xF69D50;

    static Theme create() {
        return rgbTheme("Dark", DARK_CHARCOAL,
                SILVER_BLUE_GREY, SKY_BLUE,
                LIGHT_BLUE_GREY, CHARCOAL,
                LIGHT_BLUE_GREY, SLATE,
                WHITE, BLUE,
                SLATE_GREY, MEDIUM_SLATE_GREY,
                CORAL_RED, LIGHT_GREEN,
                LIGHT_BLUE_GREY, DARK_BLUE)
                .documents(DARK_CHARCOAL, PALE_BLUE, LIGHT_BLUE_GREY,
                        LIGHT_BLUE_GREY, LAVENDER, SKY_BLUE,
                        SOFT_ORANGE, CHARCOAL, MUTED_BLUE_GREY)
                .withStatusColor(rgb(SEA_GREEN, DARK_CHARCOAL))
                .withWarningColor(rgb(GOLD, DARK_CHARCOAL))
                .withDialogColor(rgb(SILVER_BLUE_GREY, CHARCOAL))
                .withStripeColor(rgb(SILVER_BLUE_GREY, DARK_SLATE))
                .drawing(DARK_CHARCOAL, BRIGHT_CYAN, GOLD, LAVENDER,
                        LIGHT_GREEN, CORAL_RED, SKY_BLUE, SOFT_ORANGE, LIGHT_SLATE_GREY)
                .dialogs(d -> d.withInputColor(rgb(LIGHT_BLUE_GREY, DARK_CHARCOAL)));
    }
}
