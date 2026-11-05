package mizzen.components.theme;

import mizzen.controlcodes.color.Colors;

import static mizzen.components.theme.Theme.legacyDialogColor;
import static mizzen.components.theme.Theme.onWhite;

final class LegacyTheme {
    private LegacyTheme() {}

    static Theme create() {
        return new Theme("Legacy", Colors.DEFAULT_COLOR, Colors.DEFAULT_COLOR, Colors.BYel,
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

    }
}
