package mizzen.controlcodes.color;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mizzen.textsegments.CleanString;

public class ColorParsingUtils {

    public static class ColorParseSegment {
        Color color;
        CleanString simpleText;

        public ColorParseSegment(Color color, CleanString simpleText) {
            super();
            this.color = color;
            this.simpleText = simpleText;
        }

        public int length() {
            return this.simpleText != null ? this.simpleText.length() : 0;
        }

        @Override
        public String toString() {
            return "ColorParseSegment {color=" + color + ", simpleText=" + simpleText + "}";
        }
    }

    public static interface ColorColorSegmentBuilder {
        boolean newColorSegment(Color color, CleanString simpleText);
    }

    public static String colorCODE_patternStr = "\\e\\[[0-9,;]*m";
    public static Pattern colorCODE_pattern = Pattern.compile(colorCODE_patternStr);

    public static boolean hasColorCodes(String source) {
        Matcher matcher = colorCODE_pattern.matcher(source);
        boolean matchFound = matcher.find();
        return matchFound;
    }

    public static String removeColorCodes(String source) {

        Matcher matcher = colorCODE_pattern.matcher(source);
        boolean matchFound = matcher.find();
        if (!matchFound) {
            return source;
        }
        // have to use StringBuffer because matcher requirements
        StringBuffer sb = new StringBuffer();
        while (matchFound) {
            matcher.appendReplacement(sb, "");
            matchFound = matcher.find();
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    static final int PARSE_TEXT = 1;
    static final int PARSE_COLOR_START = 2;
    static final int PARSE_COLOR_CODE = 3;
    static final int PARSE_COLOR_END = 4;

    /***
     * Parses a string and extracts color codes.
     * </p>
     * This code only works correctly for single line strings.
     * </p>
     * Consider the result as undefined for multi-line Strings.
     *
     * @param initialColor
     * @param colorCodedStr
     * @param colorColorSegmentBuilder
     */
    public static Color parseColorsAndBuildResult(Color initialColor, String colorCodedStr,
            ColorColorSegmentBuilder colorColorSegmentBuilder) {

        int PARSE_STATE = PARSE_TEXT;
        int codesBuffer[] = new int[50];
        int codesBufferLen = 0;

        StringBuilder sb = new StringBuilder(50);
        sb.setLength(0);
        CleanString simpleText = null;
        Color color = null;
        int colorCodedStrLength = colorCodedStr.length();
        for (int i = 0; i < colorCodedStrLength; i++) {

            char ch = colorCodedStr.charAt(i);

            switch (PARSE_STATE) {
            case PARSE_TEXT:
                if (ch == '\u001B') {
                    if (sb.length()>0) {
                        simpleText = new CleanString.Simple(sb.toString());
                        boolean continueParsing = colorColorSegmentBuilder.newColorSegment(color, simpleText);
                        if (!continueParsing) {
                            return color;
                        }
                    }
                    PARSE_STATE = PARSE_COLOR_START;
                    simpleText = null;
                   // color = null;
                    sb = new StringBuilder();
                    sb.append(ch);
                } else {
                    sb.append(ch);
                }
                break;

            case PARSE_COLOR_START:
                if (ch == '[') {
                    break; // ignore
                } else {
                    if (Character.isDigit(ch)) {
                        PARSE_STATE = PARSE_COLOR_CODE;
                        codesBufferLen = 0;
                        sb = new StringBuilder();
                    } else {
                        if (ch == 'm') {
                            color = color == null? Colors.RCol : color;
                            sb = new StringBuilder();
                            PARSE_STATE = PARSE_TEXT;
                            break;
                        } else {
                            PARSE_STATE = PARSE_TEXT;
                        }
                    }
                    sb.append(ch);
                }
                break;

            case PARSE_COLOR_CODE:
                if (ch == ';' || ch == 'm') {
                    String num = sb.toString();
                    sb.append(ch);
                    if (num.length() == 0 && ch!='m') {
                        PARSE_STATE = PARSE_TEXT;
                        break;
                    }
                    // special case - badly formed RCOL
                    if (num.length() == 0 && ch!='m') {
                        color = Colors.RCol;
                        sb = new StringBuilder();
                        PARSE_STATE = PARSE_TEXT;
                        break;
                    } else {
                        int code = Integer.parseInt(num);
                        codesBuffer[codesBufferLen] = code;
                        codesBufferLen++;
                        sb = new StringBuilder();
                        if (ch == 'm' || codesBufferLen >= codesBuffer.length) {
                            PARSE_STATE = PARSE_COLOR_END;
                        }
                    }
                } else {
                    sb.append(ch);
                    if (!Character.isDigit(ch)) {
                        PARSE_STATE = PARSE_TEXT;
                    }
                }
                break;

            case PARSE_COLOR_END:
                int[] codes = new int[codesBufferLen];
                System.arraycopy(codesBuffer, 0, codes, 0, codesBufferLen);
                color = Colors.ColorFromCodes.instance(codes);
                PARSE_STATE = PARSE_TEXT;

                if (ch == '\u001B') { // this can happen if we have color codes back to back.
                    simpleText = new CleanString.Simple(sb.toString());
                    boolean continueParsing = colorColorSegmentBuilder.newColorSegment(color,
                            CleanString.UTIL.EMPTY_CLEAN_STRING);
                    if (!continueParsing)
                        return color;
                    PARSE_STATE = PARSE_COLOR_START;
                    simpleText = null;
                    //color = null;
                    sb = new StringBuilder();
                }

                sb.append(ch);
                break;

            default:
                throw new IllegalStateException("Should not get here!");
            }
            
        }

        if (sb.length() > 0) {
            colorColorSegmentBuilder.newColorSegment(color, new CleanString.Simple(sb.toString()));
        }

        return color;

    }

    /***
     * Parses a string and extracts color codes.
     * </p>
     * This code only works correctly for single line strings.
     * </p>
     * Consider the result as undefined for multi-line Strings.
     *
     * @param colorCodedStr
     * @param colorColorSegmentBuilder
     */
    public static Color parseColorsAndBuildResult(String colorCodedStr, ColorColorSegmentBuilder colorColorSegmentBuilder) {
        return parseColorsAndBuildResult(null, colorCodedStr, colorColorSegmentBuilder);
    }

    public static List<ColorParseSegment> parseColors(String colorCodedStr) {
        final List<ColorParseSegment> result = new ArrayList<ColorParseSegment>();

        parseColorsAndBuildResult(null, colorCodedStr, new ColorColorSegmentBuilder() {
            @Override
            public boolean newColorSegment(Color color, CleanString simpleText) {
                result.add(new ColorParseSegment(color, simpleText));
                return true; // true means keep parsing
            }
        });

        return result;

    }

}
