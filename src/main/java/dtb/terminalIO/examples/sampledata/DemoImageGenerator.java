package dtb.terminalIO.examples.sampledata;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

/**
 * Draws the demo images (ours to publish - no downloaded pictures): animated GIFs and a sprite
 * sheet of flat colours with thick outlines, which render well in a terminal.
 * <p/>
 * usage: ./runDBG.sh dtb.terminalIO.examples.sampledata.DemoImageGenerator [output directory]
 * <p/>
 * Writes rocket-launch.gif (see RocketLaunch), shapes.gif, bouncing-ball.gif, solar-system.gif,
 * and two sprite sheets of 4 x 2 frames: rocket-sprites.png (96 x 96 each) and ball-sprites.png
 * (64 x 64). Apart from the launch, each animation loops without a jump.
 */
public class DemoImageGenerator {

    static final Color OUTLINE = new Color(20, 20, 30);         // on light backgrounds
    static final Color LIGHT_OUTLINE = new Color(240, 240, 255); // on dark ones
    static final Color NIGHT = new Color(16, 24, 64);

    public static void main(String[] args) throws IOException {
        File dir = new File(args.length > 0 ? args[0] : "src/main/resources/dtb/terminalIO/examples/sampledata/images");
        dir.mkdirs();
        writeGif(new File(dir, "shapes.gif"), shapes(), 60);
        writeGif(new File(dir, "bouncing-ball.gif"), bouncingBall(), 50);
        writeGif(new File(dir, "solar-system.gif"), solarSystem(), 60);
        ImageIO.write(spriteSheet(), "png", new File(dir, "ball-sprites.png"));
        writeGif(new File(dir, "rocket-launch.gif"), RocketLaunch.gif(), 50);
        ImageIO.write(RocketLaunch.sprites(), "png", new File(dir, "rocket-sprites.png"));
        System.out.println("demo images written to " + dir.getPath());
    }

    // ---- the images

    /** a star spinning in the middle, a triangle, a square and a hexagon circling it */
    static List<BufferedImage> shapes() {
        int frames = 36;
        List<BufferedImage> out = new ArrayList<BufferedImage>();
        for (int f = 0; f < frames; f++) {
            double t = 2 * Math.PI * f / frames;
            BufferedImage img = canvas(160, 160, NIGHT);
            Graphics2D g = graphics(img);
            draw(g, star(80, 80, 34, 15, 5, t), new Color(255, 214, 0));
            Color[] colours = { new Color(255, 70, 70), new Color(60, 200, 90), new Color(70, 150, 255) };
            for (int i = 0; i < 3; i++) {
                double a = -t + i * 2 * Math.PI / 3;      // circle the other way
                int x = (int) (80 + 58 * Math.cos(a));
                int y = (int) (80 + 58 * Math.sin(a));
                draw(g, polygon(x, y, 17, 3 + i, 2 * t), colours[i]);
            }
            g.dispose();
            out.add(img);
        }
        return out;
    }

    /** a beach ball bouncing on a checkered floor, squashing as it lands, with a shadow */
    static List<BufferedImage> bouncingBall() {
        int frames = 30;
        List<BufferedImage> out = new ArrayList<BufferedImage>();
        for (int f = 0; f < frames; f++) {
            out.add(ballFrame(200, 150, (double) f / frames));
        }
        return out;
    }

    /** a sun with turning rays, three planets (one ringed) orbiting at 1, 2 and 3 turns per loop */
    static List<BufferedImage> solarSystem() {
        int frames = 60;
        Random random = new Random(7);
        int[][] stars = new int[40][2];
        for (int[] s : stars) {
            s[0] = random.nextInt(200);
            s[1] = random.nextInt(200);
        }
        List<BufferedImage> out = new ArrayList<BufferedImage>();
        for (int f = 0; f < frames; f++) {
            double t = 2 * Math.PI * f / frames;
            BufferedImage img = canvas(200, 200, new Color(8, 8, 24));
            Graphics2D g = graphics(img);
            g.setColor(Color.WHITE);
            for (int[] s : stars) {
                g.fillRect(s[0], s[1], 2, 2);
            }
            g.setStroke(new BasicStroke(1));
            g.setColor(new Color(60, 60, 90));
            for (int r : new int[] { 42, 66, 90 }) {
                g.drawOval(100 - r, 100 - r, 2 * r, 2 * r);
            }
            draw(g, star(100, 100, 26, 18, 12, t / 2), new Color(255, 140, 0));
            draw(g, new Ellipse2D.Double(83, 83, 34, 34), new Color(255, 220, 40));
            planet(g, 42, 3 * t, 7, new Color(200, 120, 255), false);
            planet(g, 66, 2 * t, 10, new Color(60, 170, 255), false);
            planet(g, 90, t, 9, new Color(240, 200, 120), true);
            g.dispose();
            out.add(img);
        }
        return out;
    }

    /** 8 frames of the bouncing ball on one sheet: 4 across, 2 down, 64 x 64 each */
    static BufferedImage spriteSheet() {
        BufferedImage sheet = canvas(4 * 64, 2 * 64, Color.BLACK);
        Graphics2D g = sheet.createGraphics();
        for (int i = 0; i < 8; i++) {
            g.drawImage(ballFrame(64, 64, i / 8.0), (i % 4) * 64, (i / 4) * 64, null);
        }
        g.dispose();
        return sheet;
    }

    // ---- pieces

    /** one frame of the bouncing ball; phase 0..1 - the ball lands at 0 (and 1) */
    static BufferedImage ballFrame(int w, int h, double phase) {
        BufferedImage img = canvas(w, h, new Color(130, 200, 255));
        Graphics2D g = graphics(img);
        int floor = h * 4 / 5;
        int tile = Math.max(4, w / 10);
        for (int x = 0; x < w; x += tile) {
            for (int y = floor; y < h; y += tile / 2) {
                g.setColor(((x / tile + (y - floor) / (tile / 2)) % 2 == 0) ? new Color(60, 160, 60) : new Color(40, 120, 40));
                g.fillRect(x, y, tile, tile / 2);
            }
        }
        double r = h / 7.0;
        double height = 4 * phase * (1 - phase);           // 0 on the floor, 1 at the top
        double squash = phase < 0.08 || phase > 0.92 ? 0.75 : 1.0;
        double ry = r * squash;
        double rx = r / squash;
        double cy = floor - ry - height * (floor - 2 * r - 4);
        double cx = w / 2.0;
        g.setColor(new Color(0, 0, 0, 80));
        double shadow = r * (1.2 - 0.6 * height);
        g.fill(new Ellipse2D.Double(cx - shadow, floor - 3, 2 * shadow, 6));
        Shape ball = new Ellipse2D.Double(cx - rx, cy - ry, 2 * rx, 2 * ry);
        g.setColor(Color.WHITE);
        g.fill(ball);
        g.setClip(ball);
        Color[] stripes = { new Color(230, 40, 40), new Color(255, 210, 0), new Color(40, 110, 230) };
        double spin = phase * 2 * Math.PI;
        for (int i = 0; i < 3; i++) {
            double a = spin + i * 2 * Math.PI / 3;
            g.setColor(stripes[i]);
            g.fill(AffineTransform.getRotateInstance(a, cx, cy).createTransformedShape(
                    new Ellipse2D.Double(cx - rx * 0.35, cy - ry * 1.2, rx * 0.7, ry * 1.2)));
        }
        g.setClip(null);
        g.setColor(OUTLINE);
        g.setStroke(new BasicStroke(Math.max(2f, w / 80f)));
        g.draw(ball);
        g.dispose();
        return img;
    }

    static void planet(Graphics2D g, int orbit, double angle, int r, Color colour, boolean ringed) {
        int x = (int) (100 + orbit * Math.cos(angle));
        int y = (int) (100 + orbit * Math.sin(angle));
        draw(g, new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r), colour);
        if (ringed) {
            g.setColor(new Color(255, 240, 200));
            g.setStroke(new BasicStroke(2));
            g.drawOval(x - 2 * r, y - r / 3, 4 * r, 2 * r / 3);
        }
    }

    static Shape star(int cx, int cy, int outer, int inner, int points, double angle) {
        Polygon p = new Polygon();
        for (int i = 0; i < 2 * points; i++) {
            double a = angle + Math.PI * i / points;
            int r = i % 2 == 0 ? outer : inner;
            p.addPoint((int) Math.round(cx + r * Math.cos(a)), (int) Math.round(cy + r * Math.sin(a)));
        }
        return p;
    }

    static Shape polygon(int cx, int cy, int r, int sides, double angle) {
        Polygon p = new Polygon();
        for (int i = 0; i < sides; i++) {
            double a = angle + 2 * Math.PI * i / sides;
            p.addPoint((int) Math.round(cx + r * Math.cos(a)), (int) Math.round(cy + r * Math.sin(a)));
        }
        return p;
    }

    /** filled in colour, with a thick light outline (the scenes using it are dark) */
    static void draw(Graphics2D g, Shape shape, Color colour) {
        g.setColor(colour);
        g.fill(shape);
        g.setColor(LIGHT_OUTLINE);
        g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(shape);
    }

    static BufferedImage canvas(int w, int h, Color background) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(background);
        g.fillRect(0, 0, w, h);
        g.dispose();
        return img;
    }

    static Graphics2D graphics(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        return g;
    }

    // ---- writing an animated GIF (javax.imageio, no dependencies)

    static void writeGif(File file, List<BufferedImage> frames, int delayMs) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("gif").next();
        file.delete();
        try (ImageOutputStream out = ImageIO.createImageOutputStream(file)) {
            writer.setOutput(out);
            writer.prepareWriteSequence(null);
            for (BufferedImage frame : frames) {
                IIOMetadata metadata = writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(frame), null);
                String format = metadata.getNativeMetadataFormatName();
                IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(format);

                IIOMetadataNode control = child(root, "GraphicControlExtension");
                control.setAttribute("disposalMethod", "restoreToBackgroundColor");
                control.setAttribute("userInputFlag", "FALSE");
                control.setAttribute("transparentColorFlag", "FALSE");
                control.setAttribute("delayTime", Integer.toString(delayMs / 10));
                control.setAttribute("transparentColorIndex", "0");

                IIOMetadataNode loop = new IIOMetadataNode("ApplicationExtension");
                loop.setAttribute("applicationID", "NETSCAPE");
                loop.setAttribute("authenticationCode", "2.0");
                loop.setUserObject(new byte[] { 1, 0, 0 });          // loop forever
                child(root, "ApplicationExtensions").appendChild(loop);

                metadata.setFromTree(format, root);
                writer.writeToSequence(new IIOImage(frame, null, metadata), null);
            }
            writer.endWriteSequence();
        } finally {
            writer.dispose();
        }
    }

    private static IIOMetadataNode child(IIOMetadataNode root, String name) {
        for (int i = 0; i < root.getLength(); i++) {
            if (root.item(i).getNodeName().equalsIgnoreCase(name)) {
                return (IIOMetadataNode) root.item(i);
            }
        }
        IIOMetadataNode node = new IIOMetadataNode(name);
        root.appendChild(node);
        return node;
    }
}
