package dtb.terminalIO.examples.sampledata;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import dtb.terminalIO.controlcodes.color.ColorParsingUtils;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.textsegments.ScreenLineSegment;
import dtb.terminalIO.textsegments.ScreenLineSegment.UTIL.SegmentsFromStringResult;

public class ReadSampleFiles {

    public static String readStreamToString(InputStream stream) throws IOException {
        BufferedInputStream in = new BufferedInputStream(stream);
        StringBuilder sb = new StringBuilder();
        while (in.available() > 0) {
            int ch = in.read();
            sb.append((char) ch);
        }
        return sb.toString();
    }

    public static String readSampleFile(String relativeFileName) throws IOException {
        try (InputStream stream = ReadSampleFiles.class.getResourceAsStream(relativeFileName);) {
            return readStreamToString(stream);
        }
    }

    public static String readSampleFile(String relativeFileName, boolean stripColors) throws IOException {
        String contents = readSampleFile(relativeFileName);
        if (stripColors) {
            contents = ColorParsingUtils.removeColorCodes(contents);
        }
        return contents;
    }

    public static void main(String args[]) throws Exception {
        // System.out.println( readSampleFile("colour-banner.txt", false ));
        String text = readSampleFile("colour-banner.txt", false);
        // System.out.println(text);
        long start = System.currentTimeMillis();
        List<ColorParsingUtils.ColorParseSegment> result = ColorParsingUtils.parseColors(text);
        long elapsed = System.currentTimeMillis() - start;
        System.out.println(result.size() + "    elapsed " + elapsed);

        start = System.currentTimeMillis();

        SegmentsFromStringResult r = ScreenLineSegment.UTIL.textSegmentsFromStringWithEmbeddedColor(text, 0, 0, new Rectangle.Simple(0, 0, 100, 300));

        elapsed = System.currentTimeMillis() - start;
        System.out.println(r.size() + "    elapsed " + elapsed);

        // for ( ColorParseSegment r : result) {
        // System.out.println(r);
        // }
    }

}
