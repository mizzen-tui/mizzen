
package mizzen.components.theme;

import mizzen.components.BlockChars;

import static mizzen.components.theme.Theme.rgb;
import static mizzen.components.theme.Theme.rgbBold;
import static mizzen.components.theme.Theme.rgbTheme;

final class RoundedTheme {
    private RoundedTheme() {}

    // Palette: names describe the actual colours, not their roles.
    private static final int DARK_CHARCOAL = 0x1F2933;
    private static final int DEEP_NAVY = 0x102A43;
    private static final int DARK_SLATE = 0x2A3744;
    private static final int CHARCOAL_SLATE = 0x2B3846;
    private static final int SLATE_BLUE = 0x34495E;
    private static final int MEDIUM_SLATE_BLUE = 0x3E5266;
    private static final int SLATE_GREY = 0x52606D;
    private static final int MEDIUM_GREY = 0x7B8794;
    private static final int BLUE_GREY = 0x8796A5;
    private static final int LIGHT_BLUE_GREY = 0x9AA5B1;
    private static final int DARK_BLUE_GREY = 0x354556;
    private static final int PALE_BLUE_GREY = 0xD9E2EC;
    private static final int OFF_WHITE = 0xF0F4F8;
    private static final int SOFT_TEAL = 0x7BD3C8;
    private static final int PALE_TEAL = 0x9FE0D8;
    private static final int SKY_BLUE = 0x8BC4F9;
    private static final int BRIGHT_CYAN = 0x6FD6E8;
    private static final int LAVENDER = 0xC3B5F5;
    private static final int AMBER = 0xF0B429;
    private static final int SOFT_GOLD = 0xF7C978;
    private static final int PALE_GOLD = 0xF0C75E;
    private static final int SOFT_ORANGE = 0xF5A86B;
    private static final int SOFT_RED = 0xF29B9B;
    private static final int LIGHT_GREEN = 0x8EDB9B;

    static Theme create() {
        return rgbTheme("Rounded", DARK_CHARCOAL,
                PALE_BLUE_GREY, SOFT_TEAL,
                OFF_WHITE, DARK_SLATE,
                OFF_WHITE, MEDIUM_SLATE_BLUE,
                DEEP_NAVY, AMBER,
                SLATE_GREY, MEDIUM_GREY,
                SOFT_RED, LIGHT_GREEN,
                OFF_WHITE, SLATE_BLUE)
                .documents(DARK_CHARCOAL, PALE_TEAL, PALE_BLUE_GREY,
                        OFF_WHITE, LAVENDER, SKY_BLUE,
                        SOFT_GOLD, DARK_SLATE, BLUE_GREY)
                .withStatusColor(rgb(LIGHT_GREEN, DARK_CHARCOAL))
                .withWarningColor(rgb(AMBER, DARK_CHARCOAL))
                .withSurfaceColor(rgb(PALE_BLUE_GREY, CHARCOAL_SLATE))
                .withAccentColor(rgbBold(AMBER, DARK_CHARCOAL))
                .withStripeColor(rgb(PALE_BLUE_GREY, DARK_BLUE_GREY))
                .drawing(DARK_CHARCOAL, BRIGHT_CYAN, PALE_GOLD, LAVENDER,
                        LIGHT_GREEN, SOFT_RED, SKY_BLUE, SOFT_ORANGE, LIGHT_BLUE_GREY)
                .withBlockChars(BlockChars.ROUNDED)
                .withFocusedBlockChars(BlockChars.ROUNDED)
                .withButtonFocus(Theme.ButtonFocus.ACCENT)
                .dialogs(d -> d.withInputColor(rgb(OFF_WHITE, DARK_CHARCOAL)));
    }
}
