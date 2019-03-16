package dtb.terminalIO.examples.sampledata;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.ControlCode.ColorParseSegment;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.textsegments.ScreenLineSegment;
import dtb.terminalIO.textsegments.ScreenLineSegment.UTIL.SegmentsFromStringResult;

public class ReadSampleFiles {

    public static String readStreamToString(InputStream stream) throws IOException {
        BufferedInputStream in = new BufferedInputStream(stream);
        StringBuffer sb = new StringBuffer();
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
            contents = ControlCode.removeColorCodes(contents);
        }
        return contents;
    }

    public static void main(String args[]) throws Exception {
        // System.out.println( readSampleFile("acme-white-200.txt", false ));
        String text = readSampleFile("acme-white-200.txt", false);
        // System.out.println(text);
        long start = System.currentTimeMillis();
        List<ColorParseSegment> result = ControlCode.parseColors(text);
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
