package mizzen.examples.sampledata;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * A ketch heeling under full sail - after a Cheoy Lee Midship 40: center cockpit, raised midship
 * deckhouse, the tall mainmast forward, the mizzen aft - drawn for the demos (see
 * DemoImageGenerator): flat colours, thick outlines.
 * <p/>
 * A three-quarter view, the way it is usually painted: the rig leans to leeward and the deck
 * shows. The boat rises and pitches over the swell, sails bellied, bow wave and wake churning,
 * gulls overhead; the waves roll past and the loop has no jump.
 */
class SailingKetch {

    static final int W = 240;
    static final int H = 180;
    static final int FRAMES = 48;
    static final int WATER = 122;              // screen row of the calm waterline

    static final Color OUTLINE = new Color(25, 30, 40);
    static final Color HULL = new Color(20, 50, 95);
    static final Color BOOT = new Color(250, 250, 245);
    static final Color BOTTOM = new Color(160, 40, 40);
    static final Color DECK = new Color(200, 160, 110);           // teak
    static final Color HOUSE = new Color(245, 245, 240);
    static final Color SAIL = new Color(252, 250, 240);
    static final Color SAIL_SHADE = new Color(222, 220, 210);
    static final Color SPAR = new Color(140, 95, 55);
    static final Color SEA_FAR = new Color(40, 110, 170);
    static final Color SEA_NEAR = new Color(25, 85, 145);
    static final Color FOAM = new Color(240, 250, 255);

    private static final double LEAN = Math.toRadians(8);           // the rig's lean to leeward

    static List<BufferedImage> gif() {
        List<BufferedImage> out = new ArrayList<BufferedImage>();
        for (int f = 0; f < FRAMES; f++) {
            double t = 2 * Math.PI * f / FRAMES;               // one swell per loop
            BufferedImage img = DemoImageGenerator.canvas(W, H, Color.WHITE);
            Graphics2D g = DemoImageGenerator.graphics(img);

            g.setPaint(new GradientPaint(0, 0, new Color(95, 160, 230), 0, WATER, new Color(200, 228, 250)));
            g.fillRect(0, 0, W, WATER + 10);
            sun(g, 200, 28);
            RocketLaunch.cloud(g, 20 - 6 * Math.sin(t), 18, 0.9);
            RocketLaunch.cloud(g, 120 + 4 * Math.sin(t), 40, 0.6);
            gulls(g, t);

            sea(g, t, SEA_FAR, 0, 2, 3.0, 1.0);                         // far swell, behind the boat

            double heave = 3.5 * Math.sin(t);
            double pitch = Math.toRadians(2.5) * Math.cos(t);               // bow up as the swell lifts it
            AffineTransform saved = g.getTransform();
            g.translate(118, WATER + heave);
            g.rotate(-pitch);
            g.scale(0.82, 0.82);                                            // masthead to keel fits the frame
            wake(g, t);
            boat(g, t);
            bowWave(g, t);
            g.setTransform(saved);

            sea(g, t, SEA_NEAR, 14, 1, 4.0, 1.4);                          // near swell, over the hull bottom
            whitecaps(g, t);
            g.dispose();
            out.add(img);
        }
        return out;
    }

    // ---- the boat, in its own coordinates: x towards the bow, y down, 0 = waterline amidships

    static void boat(Graphics2D g, double t) {
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        Path2D hull = new Path2D.Double();
        hull.moveTo(-70, -11);                                   // transom top
        hull.curveTo(-30, -9, 30, -10, 80, -17);                 // sheer, rising to the bow
        hull.curveTo(70, -4, 58, 4, 40, 8);                      // stem and forefoot
        hull.curveTo(20, 22, -20, 22, -32, 12);                  // full keel
        hull.curveTo(-48, 8, -62, 4, -66, 0);                    // run aft
        hull.lineTo(-70, -11);
        hull.closePath();

        // deck, seen from windward because of the heel
        Path2D deck = new Path2D.Double();
        deck.moveTo(-70, -11);
        deck.curveTo(-30, -9, 30, -10, 80, -17);
        deck.curveTo(60, -19, 0, -18, -66, -15);
        deck.closePath();
        RocketLaunch.fillAndOutline(g, deck, DECK);

        g.setColor(HULL);
        g.fill(hull);
        Shape clip = g.getClip();
        g.clip(hull);
        g.setColor(BOTTOM);
        g.fill(new Rectangle2D.Double(-80, 1, 170, 30));         // antifouling below the waterline
        g.setColor(BOOT);
        g.fill(new Rectangle2D.Double(-80, -3, 170, 4));         // boot stripe
        g.setColor(new Color(235, 200, 90));
        g.fill(sheerStripe());
        g.setClip(clip);
        g.setColor(OUTLINE);
        g.draw(hull);

        // raised midship deckhouse, center cockpit, portlights
        Path2D house = new Path2D.Double();
        house.moveTo(-28, -14);
        house.lineTo(-24, -23);
        house.lineTo(24, -24);
        house.lineTo(30, -15);
        house.closePath();
        RocketLaunch.fillAndOutline(g, house, HOUSE);
        for (int x = -16; x <= 16; x += 11) {
            RocketLaunch.fillAndOutline(g, new Ellipse2D.Double(x - 2.5, -21, 5, 4), new Color(80, 140, 200));
        }
        RocketLaunch.fillAndOutline(g, RocketLaunch.polygon(-46, -13, -44, -19, -30, -19, -30, -13), new Color(120, 120, 130));  // dodger aft

        // the rig, leaning to leeward (sheared about the deck line)
        AffineTransform beforeRig = g.getTransform();
        g.shear(Math.tan(LEAN), 0);
        double belly = 3 * Math.sin(2 * t);          // the sails breathe a little

        // bowsprit and forestay
        g.setColor(SPAR);
        g.setStroke(new BasicStroke(2.5f));
        g.draw(new Line2D.Double(78, -17, 96, -19));

        // genoa: head near the masthead, tack at the bowsprit, clew aft
        Path2D genoa = new Path2D.Double();
        genoa.moveTo(16, -120);
        genoa.quadTo(64 + belly, -66, 94, -20);
        genoa.quadTo(66, -22, 40, -24);
        genoa.quadTo(30 + belly, -70, 16, -120);
        genoa.closePath();
        sail(g, genoa);

        // mainsail on the mainmast and boom
        Path2D main = new Path2D.Double();
        main.moveTo(13, -124);
        main.lineTo(13, -27);
        main.lineTo(-32, -29);
        main.quadTo(-12 - belly, -84, 13, -124);
        main.closePath();
        sail(g, main);

        // mizzen, smaller, aft
        Path2D mizzen = new Path2D.Double();
        mizzen.moveTo(-48, -86);
        mizzen.lineTo(-48, -24);
        mizzen.lineTo(-76, -25);
        mizzen.quadTo(-64 - belly / 2, -60, -48, -86);
        mizzen.closePath();
        sail(g, mizzen);

        // masts and booms over the sails
        g.setColor(SPAR);
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(14, -14, 14, -128));            // mainmast
        g.draw(new Line2D.Double(-47, -13, -47, -90));           // mizzen mast
        g.setStroke(new BasicStroke(2.5f));
        g.draw(new Line2D.Double(14, -27, -34, -29));            // main boom
        g.draw(new Line2D.Double(-47, -24, -78, -25));           // mizzen boom
        g.setStroke(new BasicStroke(1f));
        g.setColor(new Color(60, 60, 70));
        g.draw(new Line2D.Double(14, -128, 96, -19));            // forestay
        g.draw(new Line2D.Double(14, -128, -47, -90));           // triatic stay, mast to mast
        g.setColor(new Color(220, 40, 40));
        g.fill(RocketLaunch.polygon(14, -128, 14, -134, 24 + 2 * Math.sin(3 * t), -131));   // masthead pennant
        g.setTransform(beforeRig);
    }

    static Shape sheerStripe() {
        Path2D p = new Path2D.Double();
        p.moveTo(-70, -9);
        p.curveTo(-30, -7, 30, -8, 80, -15);
        p.lineTo(80, -13.5);
        p.curveTo(30, -6.5, -30, -5.5, -70, -7.5);
        p.closePath();
        return p;
    }

    static void sail(Graphics2D g, Shape sail) {
        g.setPaint(new GradientPaint(0, -120, SAIL, -30, -20, SAIL_SHADE));
        g.fill(sail);
        g.setColor(OUTLINE);
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(sail);
    }

    static void bowWave(Graphics2D g, double t) {
        double spray = 4 + 3 * Math.max(0, Math.sin(t + 0.6));    // more as the bow drops into the swell
        g.setColor(FOAM);
        g.fill(RocketLaunch.polygon(52, -1, 84, -3 - spray, 92, 2, 70, 4));
        g.setColor(new Color(200, 225, 245));
        g.setStroke(new BasicStroke(1.2f));
        g.draw(RocketLaunch.polygon(52, -1, 84, -3 - spray, 92, 2, 70, 4));
        for (int k = 0; k < 4; k++) {
            double a = t * 2 + k * 1.7;
            g.setColor(FOAM);
            g.fill(new Ellipse2D.Double(80 + 6 * Math.cos(a) + k * 3, -8 - spray - 3 * Math.abs(Math.sin(a)), 2.5, 2.5));
        }
    }

    static void wake(Graphics2D g, double t) {
        g.setColor(FOAM);
        for (int k = 0; k < 6; k++) {
            double x = -72 - k * 16 - 8 * ((t / (2 * Math.PI)) % 1);
            double w = 14 - k * 1.5;
            g.fill(new Ellipse2D.Double(x - w / 2, -2 + k * 0.6, w, 3.5));
        }
    }

    // ---- sea and sky

    /** a band of swell from depth below the waterline down; crests move left (the boat sails right) */
    static void sea(Graphics2D g, double t, Color colour, int depth, int cycles, double amplitude, double speed) {
        Path2D p = new Path2D.Double();
        p.moveTo(0, H);
        for (int x = 0; x <= W; x += 4) {
            double phase = 2 * Math.PI * cycles * x / W + speed * t;
            double crest = amplitude * Math.sin(phase) + 0.4 * amplitude * Math.sin(2 * phase + 1);
            p.lineTo(x, WATER + depth + crest);
        }
        p.lineTo(W, H);
        p.closePath();
        g.setColor(colour);
        g.fill(p);
        g.setColor(colour.darker());
        g.setStroke(new BasicStroke(1.5f));
        g.draw(p);
    }

    static void whitecaps(Graphics2D g, double t) {
        g.setColor(FOAM);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double[][] at = { { 30, 150 }, { 110, 162 }, { 190, 146 }, { 70, 172 }, { 220, 170 } };
        for (int i = 0; i < at.length; i++) {
            double x = ((at[i][0] - 240 * (t / (2 * Math.PI)) * (i % 2 == 0 ? 1 : 2)) % 240 + 240) % 240;
            g.draw(new Line2D.Double(x, at[i][1], x + 8, at[i][1] - 2));
            g.draw(new Line2D.Double(x + 8, at[i][1] - 2, x + 14, at[i][1]));
        }
    }

    static void sun(Graphics2D g, int x, int y) {
        g.setColor(new Color(255, 240, 150));
        g.fill(new Ellipse2D.Double(x - 16, y - 16, 32, 32));
        RocketLaunch.fillAndOutline(g, new Ellipse2D.Double(x - 11, y - 11, 22, 22), new Color(255, 215, 60));
    }

    static void gulls(Graphics2D g, double t) {
        g.setColor(new Color(50, 55, 70));
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double[][] at = { { 70, 30 }, { 92, 22 }, { 160, 56 } };
        for (int i = 0; i < at.length; i++) {
            double x = at[i][0] + 6 * Math.sin(t + i);
            double y = at[i][1] + 3 * Math.cos(t + 2 * i);
            double flap = 3 + 2 * Math.sin(2 * t + i);       // wings beat twice a loop
            Path2D bird = new Path2D.Double();
            bird.moveTo(x - 7, y - flap);
            bird.quadTo(x - 3, y - flap, x, y);
            bird.quadTo(x + 3, y - flap, x + 7, y - flap);
            g.draw(bird);
        }
    }
}
