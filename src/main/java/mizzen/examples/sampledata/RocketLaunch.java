package mizzen.examples.sampledata;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A rocket launch, drawn for the demos (see DemoImageGenerator): flat colours, thick outlines.
 * <p/>
 * gif(): on the pad beside its tower, ignition and billowing smoke, the tower arm swings away,
 * lift-off; the camera follows it up as the sky darkens, clouds slide past and stars come out;
 * then a gentle gravity turn - the rocket leans a little east, the way the Earth turns - and it
 * leaves the picture trailing smoke.
 * <p/>
 * sprites(): 8 frames of the rocket in flight (flame flickering, clouds and stars streaming past)
 * on one 4 x 2 sheet, looping without a jump.
 */
class RocketLaunch {

    static final int W = 240;
    static final int H = 240;
    static final int GROUND = 205;          // screen row of the ground before the camera moves

    static final Color OUTLINE = new Color(25, 25, 35);
    static final Color HULL = new Color(245, 245, 250);
    static final Color RED = new Color(220, 40, 40);
    static final Color TOWER = new Color(230, 100, 30);
    static final Color FLAME_OUT = new Color(255, 110, 0);
    static final Color FLAME_MID = new Color(255, 200, 40);
    static final Color FLAME_CORE = new Color(255, 255, 220);

    private static final int IGNITION = 10;
    private static final int LIFTOFF = 26;
    private static final int TURN_START = 38;     // flight frames before the gravity turn starts
    private static final int TURN_FRAMES = 30;
    private static final double TURN_DEGREES = 12;
    private static final int CAMERA_STOPS = 62;   // flight frame the camera stops following
    private static final int FRAMES = 104;

    /** a smoke puff, in world coordinates (y up from the ground) */
    private static class Puff {
        double x, y, vx, vy, r, grow;
        int born;

        Puff(double x, double y, double vx, double vy, double r, double grow, int born) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.r = r;
            this.grow = grow;
            this.born = born;
        }
    }

    static List<BufferedImage> gif() {
        Random random = new Random(11);
        List<Puff> puffs = new ArrayList<Puff>();
        int[][] stars = randomStars(random, 70);
        double rx = 0, ry = 0, speed = 0, pitch = 0;
        double lastTailX = 0, lastTailY = -34;
        double camX = 0, camY = 0;
        List<BufferedImage> out = new ArrayList<BufferedImage>();

        for (int f = 0; f < FRAMES; f++) {
            int flight = f - LIFTOFF;
            // the rocket: speeds up, then leans gently east (degrees off vertical)
            if (flight > 0) {
                speed += 0.32;
                double turn = Math.min(1, Math.max(0, (flight - TURN_START) / (double) TURN_FRAMES));
                pitch = Math.toRadians(TURN_DEGREES) * (turn * turn * (3 - 2 * turn));   // smooth start and end
                rx += speed * Math.sin(pitch);
                ry += speed * Math.cos(pitch);
            }
            if (flight < CAMERA_STOPS) {
                camY = Math.max(0, ry - 70);
                camX = rx * 0.75;
            }

            // smoke: billowing sideways from the pad, then a trail behind the rocket
            if (f >= IGNITION && ry < 60) {
                for (int side = -1; side <= 1; side += 2) {
                    puffs.add(new Puff(side * 6, 2, side * (1.6 + random.nextDouble()), 0.25 + 0.3 * random.nextDouble(),
                                       5 + 3 * random.nextDouble(), 0.55, f));
                }
            }
            if (flight > 0 && flight < CAMERA_STOPS + 12) {
                // a puff every few pixels between the last frame's tail and this one: a solid trail, not beads
                double tailX = rx - 34 * Math.sin(pitch);
                double tailY = ry - 34 * Math.cos(pitch);
                int steps = Math.max(1, (int) (Math.hypot(tailX - lastTailX, tailY - lastTailY) / 4));
                for (int k = 1; k <= steps; k++) {
                    double t = (double) k / steps;
                    puffs.add(new Puff(lastTailX + (tailX - lastTailX) * t + random.nextGaussian(),
                                       lastTailY + (tailY - lastTailY) * t, 0.2 * random.nextGaussian(), -0.15,
                                       3.5 + 2 * random.nextDouble(), 0.2, f));
                }
                lastTailX = tailX;
                lastTailY = tailY;
            }

            double altitude = Math.min(1, ry / 520);
            Color sky = mix(new Color(125, 195, 255), new Color(8, 12, 40), altitude);
            BufferedImage img = DemoImageGenerator.canvas(W, H, sky);
            Graphics2D g = DemoImageGenerator.graphics(img);
            g.setPaint(new GradientPaint(0, 0, mix(sky, new Color(0, 0, 20), 0.45), 0, H, mix(sky, Color.WHITE, 0.15)));
            g.fillRect(0, 0, W, H);
            stars(g, stars, (altitude - 0.5) / 0.3);
            clouds(g, camX, camY);
            ground(g, camX, camY, f);
            for (Puff p : puffs) {
                int age = f - p.born;
                if (age < 0) {
                    continue;
                }
                double r = p.r + p.grow * age;
                double sx = W / 2 + p.x + p.vx * age - camX;
                double sy = GROUND - (p.y + p.vy * age) + camY;
                int shade = (int) Math.max(150, 235 - age * 0.6);
                g.setColor(mix(new Color(shade, shade, shade + 5), sky, Math.min(0.8, age / 70.0)));   // old smoke fades
                g.fill(new Ellipse2D.Double(sx - r, sy - r, 2 * r, 2 * r));
            }
            double flame = f < IGNITION ? 0 : Math.min(1, (f - IGNITION) / 10.0);
            rocket(g, W / 2 + rx - camX, GROUND - ry + camY, pitch, flame * (0.85 + 0.3 * random.nextDouble()), 1.0);
            g.dispose();
            out.add(img);
        }
        return out;
    }

    /** 8 frames, 4 across and 2 down, each 96 x 96: the rocket in flight, the sky streaming past */
    static BufferedImage sprites() {
        int size = 96;
        int frames = 8;
        Random random = new Random(5);
        int[][] stars = randomStars(random, 30);
        BufferedImage sheet = DemoImageGenerator.canvas(4 * size, 2 * size, Color.BLACK);
        Graphics2D sg = sheet.createGraphics();
        for (int f = 0; f < frames; f++) {
            BufferedImage img = DemoImageGenerator.canvas(size, size, new Color(14, 22, 60));
            Graphics2D g = DemoImageGenerator.graphics(img);
            double shift = (double) f / frames;           // the sky moves one tile per loop
            g.setColor(Color.WHITE);
            for (int[] s : stars) {
                g.fillRect(s[0] * size / W, Math.floorMod((int) (s[1] * size / H + shift * size), size), 1, 1);
            }
            for (int k = 0; k < 2; k++) {
                double y = Math.floorMod((int) (k * size / 2 + shift * size), size) - 10;
                for (int wrap = -1; wrap <= 1; wrap++) {               // and the part wrapping round an edge
                    cloud(g, k == 0 ? 6 : 58, y + wrap * size, 0.55);
                }
            }
            rocket(g, size / 2 + 2, size / 2 + 12, Math.toRadians(TURN_DEGREES), 0.8 + 0.25 * random.nextDouble(), 0.55);
            g.dispose();
            sg.drawImage(img, (f % 4) * size, (f / 4) * size, null);
        }
        sg.dispose();
        return sheet;
    }

    // ---- pieces

    /**
     * The rocket, its base at (x, y) on screen, leaning by pitch (radians, east = right); flame:
     * 0 (none) .. about 1 (full).
     */
    static void rocket(Graphics2D g, double x, double y, double pitch, double flame, double scale) {
        AffineTransform saved = g.getTransform();
        g.translate(x, y);
        g.rotate(pitch);
        g.scale(scale, scale);

        if (flame > 0.02) {
            double len = 26 + 30 * flame;
            g.setColor(FLAME_OUT);
            g.fill(teardrop(0, 2, 9, len));
            g.setColor(FLAME_MID);
            g.fill(teardrop(0, 2, 6, len * 0.7));
            g.setColor(FLAME_CORE);
            g.fill(teardrop(0, 2, 3, len * 0.4));
        }

        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Shape leftFin = polygon(-7, -4, -15, 4, -15, -2, -7, -18);
        Shape rightFin = polygon(7, -4, 15, 4, 15, -2, 7, -18);
        Shape body = new Rectangle2D.Double(-7, -56, 14, 54);
        Path2D nose = new Path2D.Double();
        nose.moveTo(-7, -56);
        nose.quadTo(-6, -70, 0, -80);
        nose.quadTo(6, -70, 7, -56);
        nose.closePath();
        Shape nozzle = polygon(-5, -2, 5, -2, 7, 3, -7, 3);

        fillAndOutline(g, leftFin, RED);
        fillAndOutline(g, rightFin, RED);
        fillAndOutline(g, nozzle, new Color(90, 90, 100));
        fillAndOutline(g, body, HULL);
        g.setColor(new Color(40, 40, 50));
        g.fill(new Rectangle2D.Double(-7, -30, 14, 4));          // stage band
        g.setColor(RED);
        g.fill(new Rectangle2D.Double(-7, -50, 14, 3));
        g.setColor(OUTLINE);
        g.draw(body);
        fillAndOutline(g, nose, RED);
        fillAndOutline(g, new Ellipse2D.Double(-3.5, -46, 7, 7), new Color(90, 180, 255));   // window
        fillAndOutline(g, polygon(-1.5, -40, 1.5, -40, 1.5, -6, -1.5, -6), new Color(200, 200, 210));
        g.setTransform(saved);
    }

    /** grass, the launch pad and the tower; its arm swings away at ignition */
    static void ground(Graphics2D g, double camX, double camY, int frame) {
        double groundY = GROUND + camY;
        if (groundY > H + 40) {
            return;
        }
        g.setColor(new Color(70, 150, 70));
        g.fill(new Rectangle2D.Double(0, groundY, W, H));
        g.setColor(new Color(50, 120, 55));
        for (int x = -40; x < W + 40; x += 22) {
            double sx = Math.floorMod((int) (x - camX * 0.9), W + 40) - 20;
            g.fill(new Ellipse2D.Double(sx, groundY + 8, 16, 5));
        }
        double cx = W / 2 - camX;
        fillAndOutline(g, polygon(cx - 34, groundY, cx + 34, groundY, cx + 26, groundY - 6, cx - 26, groundY - 6),
                       new Color(170, 170, 175));                         // pad
        double tx = cx - 36;
        g.setStroke(new BasicStroke(3));
        g.setColor(TOWER);
        g.draw(new Line2D.Double(tx - 6, groundY - 6, tx - 6, groundY - 96));
        g.draw(new Line2D.Double(tx + 6, groundY - 6, tx + 6, groundY - 96));
        g.setStroke(new BasicStroke(1.5f));
        for (int k = 0; k < 9; k++) {
            double y0 = groundY - 6 - k * 10;
            g.draw(new Line2D.Double(tx - 6, y0, tx + 6, y0 - 10));
            g.draw(new Line2D.Double(tx + 6, y0, tx - 6, y0 - 10));
        }
        // the arm reaching the rocket swings up and away from ignition on
        double swing = Math.min(1, Math.max(0, (frame - IGNITION) / 10.0));
        double a = -swing * Math.toRadians(70);
        double ax = tx + 6;
        double ay = groundY - 70;
        g.setStroke(new BasicStroke(3));
        g.draw(new Line2D.Double(ax, ay, ax + 22 * Math.cos(a), ay + 22 * Math.sin(a)));
        g.setColor(OUTLINE);
        g.setStroke(new BasicStroke(1));
        g.draw(new Rectangle2D.Double(tx - 8, groundY - 98, 16, 4));
    }

    /** clouds at fixed heights, sliding down past as the camera rises */
    static void clouds(Graphics2D g, double camX, double camY) {
        double[][] at = { { -70, 150 }, { 60, 230 }, { -20, 330 }, { 90, 410 }, { -90, 470 } };   // x, height
        for (double[] c : at) {
            double sx = W / 2 + c[0] - camX * 0.6;
            double sy = GROUND - c[1] + camY;
            if (sy > -30 && sy < H + 30) {
                cloud(g, sx, sy, 1.0);
            }
        }
    }

    static void cloud(Graphics2D g, double x, double y, double scale) {
        Path2D shape = new Path2D.Double();
        double[][] blobs = { { 0, 6, 16 }, { 12, 0, 20 }, { 28, 4, 16 }, { 40, 8, 12 } };     // dx, dy, diameter
        for (double[] b : blobs) {
            shape.append(new Ellipse2D.Double(x + b[0] * scale, y + b[1] * scale, b[2] * scale, b[2] * scale), false);
        }
        shape.append(new Rectangle2D.Double(x + 6 * scale, y + 12 * scale, 40 * scale, 8 * scale), false);
        Area area = new Area(shape);
        g.setColor(Color.WHITE);
        g.fill(area);
        g.setColor(new Color(170, 185, 210));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(area);
    }

    static void stars(Graphics2D g, int[][] stars, double visibility) {
        if (visibility <= 0) {
            return;
        }
        int v = (int) (255 * Math.min(1, visibility));
        g.setColor(new Color(255, 255, 255, v));
        for (int[] s : stars) {
            g.fillRect(s[0], s[1], s[2], s[2]);
        }
    }

    // ---- helpers

    static int[][] randomStars(Random random, int count) {
        int[][] stars = new int[count][3];
        for (int[] s : stars) {
            s[0] = random.nextInt(W);
            s[1] = random.nextInt(H);
            s[2] = random.nextInt(4) == 0 ? 2 : 1;
        }
        return stars;
    }

    /** a flame pointing down from (x, y): width w, length len */
    static Shape teardrop(double x, double y, double w, double len) {
        Path2D p = new Path2D.Double();
        p.moveTo(x - w, y);
        p.quadTo(x - w, y + len * 0.5, x, y + len);
        p.quadTo(x + w, y + len * 0.5, x + w, y);
        p.closePath();
        return p;
    }

    static Shape polygon(double... xy) {
        Path2D p = new Path2D.Double();
        p.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) {
            p.lineTo(xy[i], xy[i + 1]);
        }
        p.closePath();
        return p;
    }

    static void fillAndOutline(Graphics2D g, Shape shape, Color colour) {
        g.setColor(colour);
        g.fill(shape);
        g.setColor(OUTLINE);
        g.draw(shape);
    }

    static Color mix(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                         (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                         (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }
}
