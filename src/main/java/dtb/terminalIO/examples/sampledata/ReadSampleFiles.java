package dtb.terminalIO.examples.sampledata;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import dtb.terminalIO.controlcodes.color.ColorParsingUtils;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.textsegments.ScreenLineSegment;
import dtb.terminalIO.textsegments.ScreenLineSegment.UTIL.SegmentsFromStringResult;

public class ReadSampleFiles {

    /** the whole stream as UTF-8 text (the sample files use characters such as █ and ⠿) */
    public static String readStreamToString(InputStream stream) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        for (int n; (n = stream.read(buffer)) != -1; ) {
            bytes.write(buffer, 0, n);
        }
        return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
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
