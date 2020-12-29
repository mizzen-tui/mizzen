package dtb.terminalIO.util.formatters;

import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;

public class HiliteExceptionTextLineFormatter implements TextLineFormatter {

    String state = null;
    Color  bannerColor;
    Color  atColor;

    public HiliteExceptionTextLineFormatter(Color bannerColor, Color atColor) {
        super();
        this.bannerColor = bannerColor;
        this.atColor = atColor;
    }

    public HiliteExceptionTextLineFormatter(Color bannerColor) {
        this(bannerColor, bannerColor);
    }

    public HiliteExceptionTextLineFormatter() {
        this(new Colors.FGBGColors(Colors.BIRed, Colors.On_IBla), Colors.Red);
    }

    @Override
    public String getFormattedText(String line) {

        String colorONStr = "";
        String colorOFFStr = "";

        if (state == null) {
            if (line.startsWith("ERROR")) {
                state = "ERROR";
                colorONStr = bannerColor.asString();
                colorOFFStr = Colors.RCol.asString();
            }

        } else {
            if (state.equals("ERROR")) {
                if (line.trim().length() == 0) {
                    state = "ONEBLANK";
                    return "";
                }
            }

            if (state.equals("ERROR") || state.equals("ONEBLANK") || state.equals("AT")) {
                if (line.contains("at ") || line.contains("Exception") || line.contains(".javaXX:")) {
                    state = "AT";
                    colorONStr = this.atColor.asString();
                    colorOFFStr = Colors.RCol.asString();
                } else {
                    state = null;
                }
            }
        }

        return colorONStr + line + colorOFFStr;
    }

}
