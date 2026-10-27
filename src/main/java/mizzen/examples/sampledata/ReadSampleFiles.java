package mizzen.examples.sampledata;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

import mizzen.controlcodes.color.ColorParsingUtils;
import mizzen.geom.Rectangle;
import mizzen.textsegments.ScreenLineSegment;
import mizzen.textsegments.ScreenLineSegment.UTIL.SegmentsFromStringResult;

public class ReadSampleFiles {

    /** where the bundled demo images are: a classpath resource folder, not a directory on disk */
    public static final String IMAGES = "/mizzen/examples/sampledata/images/";

    /**
     * A bundled resource as a file, for readers that only read files (e.g. the animated GIF
     * reader): copied to a temporary folder, keeping its name. resource: a resource name
     * ("/dtb/.../ketch.gif") or a bundled image's name ("ketch.gif"); null if there is no such resource.
     */
    public static File resourceAsFile(String resource) {
        String name = ReadSampleFiles.class.getResource(resource) != null ? resource : IMAGES + resource;
        try (InputStream in = ReadSampleFiles.class.getResourceAsStream(name)) {
            if (in == null) {
                return null;
            }
            File dir = Files.createTempDirectory("dtb-demo").toFile();
            File copy = new File(dir, new File(name).getName());
            dir.deleteOnExit();
            copy.deleteOnExit();
            Files.copy(in, copy.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return copy;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

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
