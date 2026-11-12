package mizzen.components.plot.image;

import java.io.IOException;

import mizzen.components.input.Button;
import mizzen.components.plot.image.ImageViewerFactory.ImageRenderRequest;
import mizzen.components.plot.image.animate.AnimatedGifImageIterator;
import mizzen.geom.Dimension;
import junit.framework.TestCase;

/** An animation shows its Options button unless told withoutOptions(): a viewer that only shows. */
public class MovingPicturesViewerOptionsTest extends TestCase {

    private static final String GIF = "src/main/resources/mizzen/examples/sampledata/images/bouncing-ball.gif";

    public void testOptionsByDefault() throws IOException {
        try (MovingPicturesViewer viewer = viewer()) {
            assertEquals(1, buttons(viewer));
        }
    }

    public void testWithoutOptionsHasNoButton() throws IOException {
        try (MovingPicturesViewer viewer = viewer().withoutOptions().withoutOptions()) {   // twice is harmless
            assertEquals(0, buttons(viewer));
        }
    }

    private static MovingPicturesViewer viewer() throws IOException {
        ImageRenderRequest request = new ImageRenderRequest();
        request.setDesiredImgSizeInCharacters(new Dimension.Simple(10, 20));
        return new MovingPicturesViewer(new AnimatedGifImageIterator(GIF), request);
    }

    private static long buttons(MovingPicturesViewer viewer) {
        return viewer.getWrappedComponent().getChildren().stream().filter((c) -> c instanceof Button).count();
    }
}
