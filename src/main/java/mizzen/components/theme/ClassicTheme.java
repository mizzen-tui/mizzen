package mizzen.components.theme;


import static mizzen.components.theme.Theme.rgb;
import static mizzen.components.theme.Theme.rgbTheme;

final class ClassicTheme {
    private ClassicTheme() {}

    static Theme create() {
        return rgbTheme("Classic", 0x1D3268,

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
    }
}
