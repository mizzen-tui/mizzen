package mizzen.components.plot.image.animate;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class AnimatedGifImageIterator
        implements ImageIterator {

    /*
     * The GIF remains open for the lifetime of this iterator.
     *
     * Unlike the old implementation, we do NOT retain a decoded
     * BufferedImage for every GIF frame.
     */
    private ImageInputStream input;
    private ImageReader reader;

    /*
     * Small metadata objects only.
     */
    private final List<GifFrameInfo> frames =
        new ArrayList<GifFrameInfo>();

    private long totalDurationMillis;

    /*
     * Current fully composed logical GIF canvas.
     *
     * This is the BufferedImage returned to the TUI.
     */
    private BufferedImage canvas;

    /*
     * Required by GIF's restoreToPrevious disposal mode.
     *
     * Usually null.
     */
    private BufferedImage restoreToPrevious;

    /*
     * Frame currently represented by canvas.
     *
     * -1 means no frame has yet been composed.
     */
    private int composedFrameIDX = -1;

    /*
     * Frame currently selected by the animation clock.
     */
    private int currentImageIDX = 0;

    private long animationStartNanos = -1L;

    private boolean looping = true;
    private boolean closed = false;

    private final int canvasWidth;
    private final int canvasHeight;


    /** the GIF in this file, read from it as it plays: the file stays open until close() */
    public AnimatedGifImageIterator(
            String gifPath)
            throws IOException {

        this(ImageIO.createImageInputStream(new File(gifPath)), gifPath);
    }

    /**
     * The GIF in these bytes - for a GIF that isn't a file this process can open, such as one sent by another
     * process. The iterator keeps the bytes (the GIF's compressed size) and decodes each frame as it is shown, as it
     * does from a file.
     * <pre>
     *     ImageIterator movie = new AnimatedGifImageIterator(gifBytes);
     * </pre>
     */
    public AnimatedGifImageIterator(
            byte[] gif)
            throws IOException {

        this(new MemoryCacheImageInputStream(new ByteArrayInputStream(gif)), "GIF from bytes");
    }

    /**
     * The GIF read from this stream, to its end, at once: as AnimatedGifImageIterator(byte[]) from then on. The
     * stream is the caller's: it is not closed here, and may be closed as soon as this returns.
     * <p/>
     * Read at once, not as the GIF plays: looping goes back to the GIF's start, which a stream may not allow, and
     * ImageIO would otherwise cache it in a temporary file.
     */
    public AnimatedGifImageIterator(
            InputStream gif)
            throws IOException {

        this(readToEnd(gif));
    }

    private static byte[] readToEnd(InputStream in) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[16 * 1024];
        for (int n; (n = in.read(buffer)) != -1; ) {
            bytes.write(buffer, 0, n);
        }
        return bytes.toByteArray();
    }

    /**
     * Every constructor ends here: input, already open, is owned by this iterator from now on - closed by close(),
     * or here if the GIF can't be read. source names the GIF in error messages: its path, or "GIF from bytes".
     */
    private AnimatedGifImageIterator(
            ImageInputStream input,
            String source)
            throws IOException {

        ImageInputStream newInput = input;
        ImageReader newReader = null;

        try {

            if (newInput == null) {
                throw new IOException(
                    "Unable to open GIF: " + source);
            }

            Iterator<ImageReader> readers =
                ImageIO.getImageReadersByFormatName(
                    "gif");

            if (!readers.hasNext()) {
                throw new IOException(
                    "No GIF ImageReader available");
            }

            newReader = readers.next();

            newReader.setInput(newInput);

            Dimension logicalSize =
                readLogicalScreenSize(newReader);

            this.canvasWidth =
                logicalSize.width;

            this.canvasHeight =
                logicalSize.height;

            int numFrames =
                newReader.getNumImages(true);

            if (numFrames <= 0) {
                throw new IOException(
                    "GIF contains no image frames: "
                    + source);
            }

            /*
             * Scan only the metadata.
             *
             * No decoded BufferedImages are retained here.
             */
            for (int i = 0;
                    i < numFrames;
                    i++) {

                GifFrameInfo frame =
                    readFrameInfo(
                        newReader,
                        i);

                frames.add(frame);

                totalDurationMillis +=
                    frame.delayMillis;
            }

            this.input = newInput;
            this.reader = newReader;

            /*
             * Ownership has now transferred to this object.
             */
            newInput = null;
            newReader = null;

            this.canvas =
                createEmptyCanvas();

        } finally {

            /*
             * Constructor failed before ownership transfer.
             */
            if (newReader != null) {
                newReader.dispose();
            }

            if (newInput != null) {
                newInput.close();
            }
        }
    }


    public int currentIDX() {
        return currentImageIDX;
    }


    @Override
    public boolean hasNext() {

        if (closed || frames.isEmpty()) {
            return false;
        }

        if (looping) {
            return true;
        }

        return currentImageIDX <
            frames.size() - 1;
    }


    @Override
    public BufferedImage next() {

        ensureOpen();

        try {

            /*
             * First request establishes animation time zero.
             */
            if (animationStartNanos < 0L) {

                animationStartNanos =
                    System.nanoTime();

                currentImageIDX = 0;

                composeToFrame(0);

                return canvas;
            }

            long elapsedNanos =
                System.nanoTime()
                    - animationStartNanos;

            long elapsedMillis =
                elapsedNanos / 1000000L;

            int desiredFrame =
                findFrameForTime(
                    elapsedMillis);

            currentImageIDX =
                desiredFrame;

            /*
             * Important fast path:
             *
             * The TUI may poll every 50 ms while this GIF frame
             * lasts 200 ms. In that case there is absolutely no
             * GIF decoding or disk access here.
             */
            if (desiredFrame ==
                    composedFrameIDX) {

                return canvas;
            }

            composeToFrame(
                desiredFrame);

            return canvas;

        } catch (IOException e) {

            throw new UncheckedIOException(
                "Unable to decode GIF frame",
                e);
        }
    }


    /*
     * Advance the composited canvas until it represents targetFrame.
     *
     * If animation time wrapped around to an earlier frame, reset
     * the canvas and begin the new GIF cycle from frame zero.
     */
    private void composeToFrame(
            int targetFrame)
            throws IOException {

        if (targetFrame ==
                composedFrameIDX) {

            return;
        }

        /*
         * We crossed the GIF loop boundary.
         *
         * Start the new cycle from a clean logical canvas.
         */
        if (targetFrame <
                composedFrameIDX) {

            resetComposition();
        }

        int nextFrame =
            composedFrameIDX + 1;

        while (nextFrame <= targetFrame) {

            composeFrame(nextFrame);

            composedFrameIDX =
                nextFrame;

            nextFrame++;
        }
    }


    /*
     * Decode ONE compressed GIF frame and composite it into canvas.
     *
     * The decoded BufferedImage is deliberately only a local
     * variable. Once this method returns nothing retains it.
     */
    private void composeFrame(
            int frameIndex)
            throws IOException {

        /*
         * Before drawing this frame, apply the disposal method
         * belonging to the previously displayed frame.
         */
        if (frameIndex > 0) {

            GifFrameInfo previousFrame =
                frames.get(
                    frameIndex - 1);

            applyDisposal(
                previousFrame);
        }

        GifFrameInfo frame =
            frames.get(frameIndex);

        /*
         * If THIS frame wants restoreToPrevious, save the canvas
         * exactly as it existed before drawing this frame.
         */
        if ("restoreToPrevious".equals(
                frame.disposalMethod)) {

            restoreToPrevious =
                copyImage(canvas);

        } else {

            restoreToPrevious = null;
        }

        /*
         * This is the actual lazy decode.
         *
         * reader.read() reads/decompresses only when the animation
         * requires this frame.
         */
        BufferedImage rawFrame =
            reader.read(frameIndex);

        Graphics2D g =
            canvas.createGraphics();

        try {

            g.drawImage(
                rawFrame,
                frame.x,
                frame.y,
                null);

        } finally {

            g.dispose();
        }

        /*
         * rawFrame now falls out of scope and may be reclaimed.
         */
    }


    private void applyDisposal(
            GifFrameInfo frame) {

        if ("restoreToBackgroundColor".equals(
                frame.disposalMethod)) {

            clearRectangle(
                canvas,
                frame.x,
                frame.y,
                frame.width,
                frame.height);

        } else if (
            "restoreToPrevious".equals(
                frame.disposalMethod)) {

            if (restoreToPrevious != null) {

                /*
                 * Don't allocate another canvas here.
                 *
                 * Copy the saved state back into the existing
                 * working canvas.
                 */
                replaceCanvasWith(
                    restoreToPrevious);
            }
        }

        /*
         * "none" and "doNotDispose":
         *
         * leave canvas unchanged.
         */
    }


    private void resetComposition() {

        clearImage(canvas);

        restoreToPrevious = null;

        composedFrameIDX = -1;
    }


    private int findFrameForTime(
            long elapsedMillis) {

        if (frames.size() == 1) {
            return 0;
        }

        /*
         * Preserve the behaviour from the previous implementation
         * for malformed/all-zero-delay GIFs.
         */
        if (totalDurationMillis <= 0L) {
            return currentImageIDX;
        }

        long animationTime;

        if (looping) {

            animationTime =
                elapsedMillis
                    % totalDurationMillis;

        } else {

            if (elapsedMillis >=
                    totalDurationMillis) {

                return frames.size() - 1;
            }

            animationTime =
                elapsedMillis;
        }

        long frameEndTime = 0L;

        for (int i = 0;
                i < frames.size();
                i++) {

            frameEndTime +=
                frames.get(i).delayMillis;

            if (animationTime <
                    frameEndTime) {

                return i;
            }
        }

        return frames.size() - 1;
    }


    /*
     * Permanent release of GIF resources.
     *
     * Idempotent -- calling close twice is harmless.
     */
    @Override
    public void close() {

        if (closed) {
            return;
        }

        closed = true;

        if (reader != null) {

            reader.dispose();

            reader = null;
        }

        if (input != null) {

            try {

                input.close();

            } catch (IOException e) {

                throw new UncheckedIOException(
                    "Unable to close GIF input stream",
                    e);

            } finally {

                input = null;
            }
        }

        canvas = null;
        restoreToPrevious = null;
    }


    private void ensureOpen() {

        if (closed) {

            throw new IllegalStateException(
                "AnimatedGifImageIterator "
                + "has been closed");
        }
    }


    /*
     * Read ONLY metadata for a GIF frame.
     */
    private static GifFrameInfo readFrameInfo(
            ImageReader reader,
            int index)
            throws IOException {

        IIOMetadata metadata =
            reader.getImageMetadata(index);

        String metadataFormatName =
            metadata
                .getNativeMetadataFormatName();

        Node root =
            metadata.getAsTree(
                metadataFormatName);

        Node descriptor =
            findNode(
                root,
                "ImageDescriptor");

        Node control =
            findNode(
                root,
                "GraphicControlExtension");

        GifFrameInfo frame =
            new GifFrameInfo();

        frame.x =
            intAttribute(
                descriptor,
                "imageLeftPosition",
                0);

        frame.y =
            intAttribute(
                descriptor,
                "imageTopPosition",
                0);

        /*
         * Unlike the old eager implementation, there is no decoded
         * BufferedImage available here to provide a fallback size.
         *
         * ImageDescriptor should contain these values for a GIF.
         */
        frame.width =
            intAttribute(
                descriptor,
                "imageWidth",
                0);

        frame.height =
            intAttribute(
                descriptor,
                "imageHeight",
                0);

        if (control != null) {

            String disposalMethod =
                attribute(
                    control,
                    "disposalMethod");

            if (disposalMethod != null) {

                frame.disposalMethod =
                    disposalMethod;
            }

            int delayHundredths =
                intAttribute(
                    control,
                    "delayTime",
                    0);

            frame.delayMillis =
                delayHundredths * 10;
        }

        return frame;
    }


    private static Dimension
            readLogicalScreenSize(
                ImageReader reader)
            throws IOException {

        IIOMetadata metadata =
            reader.getStreamMetadata();

        if (metadata == null) {

            throw new IOException(
                "GIF has no stream metadata");
        }

        String metadataFormatName =
            metadata
                .getNativeMetadataFormatName();

        Node root =
            metadata.getAsTree(
                metadataFormatName);

        Node descriptor =
            findNode(
                root,
                "LogicalScreenDescriptor");

        if (descriptor == null) {

            throw new IOException(
                "GIF has no "
                + "LogicalScreenDescriptor");
        }

        int width =
            intAttribute(
                descriptor,
                "logicalScreenWidth",
                -1);

        int height =
            intAttribute(
                descriptor,
                "logicalScreenHeight",
                -1);

        if (width <= 0 ||
                height <= 0) {

            throw new IOException(
                "Invalid GIF logical screen size: "
                + width
                + "x"
                + height);
        }

        return new Dimension(
            width,
            height);
    }


    private BufferedImage createEmptyCanvas() {

        return new BufferedImage(
            canvasWidth,
            canvasHeight,
            BufferedImage.TYPE_INT_ARGB);
    }


    private static void clearRectangle(
            BufferedImage image,
            int x,
            int y,
            int width,
            int height) {

        Graphics2D g =
            image.createGraphics();

        try {

            g.setComposite(
                AlphaComposite.Clear);

            g.fillRect(
                x,
                y,
                width,
                height);

        } finally {

            g.dispose();
        }
    }


    private static void clearImage(
            BufferedImage image) {

        Graphics2D g =
            image.createGraphics();

        try {

            g.setComposite(
                AlphaComposite.Clear);

            g.fillRect(
                0,
                0,
                image.getWidth(),
                image.getHeight());

        } finally {

            g.dispose();
        }
    }


    /*
     * Restore pixels into the EXISTING canvas rather than replacing
     * the canvas object.
     *
     * This also means callers holding the current canvas reference
     * continue to refer to the same BufferedImage.
     */
    private void replaceCanvasWith(
            BufferedImage source) {

        clearImage(canvas);

        Graphics2D g =
            canvas.createGraphics();

        try {

            g.drawImage(
                source,
                0,
                0,
                null);

        } finally {

            g.dispose();
        }
    }


    private static BufferedImage copyImage(
            BufferedImage source) {

        BufferedImage copy =
            new BufferedImage(
                source.getWidth(),
                source.getHeight(),
                BufferedImage.TYPE_INT_ARGB);

        Graphics2D g =
            copy.createGraphics();

        try {

            g.drawImage(
                source,
                0,
                0,
                null);

        } finally {

            g.dispose();
        }

        return copy;
    }


    private static Node findNode(
            Node node,
            String name) {

        if (node == null) {
            return null;
        }

        if (name.equals(
                node.getNodeName())) {

            return node;
        }

        Node child =
            node.getFirstChild();

        while (child != null) {

            Node found =
                findNode(
                    child,
                    name);

            if (found != null) {
                return found;
            }

            child =
                child.getNextSibling();
        }

        return null;
    }


    private static String attribute(
            Node node,
            String name) {

        if (node == null) {
            return null;
        }

        NamedNodeMap attributes =
            node.getAttributes();

        if (attributes == null) {
            return null;
        }

        Node attribute =
            attributes.getNamedItem(
                name);

        if (attribute == null) {
            return null;
        }

        return attribute.getNodeValue();
    }


    private static int intAttribute(
            Node node,
            String name,
            int defaultValue) {

        String value =
            attribute(
                node,
                name);

        if (value == null) {
            return defaultValue;
        }

        try {

            return Integer.parseInt(
                value);

        } catch (NumberFormatException e) {

            return defaultValue;
        }
    }


    private static class GifFrameInfo {

        int x;
        int y;

        int width;
        int height;

        int delayMillis;

        String disposalMethod = "none";
    }


    private static class Dimension {

        final int width;
        final int height;

        Dimension(
                int width,
                int height) {

            this.width = width;
            this.height = height;
        }
    }
}