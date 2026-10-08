package dtb.terminalIO.examples.plot;

import java.io.IOException;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.ButtonBar;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.WindowFrame;
import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.components.plot.Pen;
import dtb.terminalIO.components.plot.PlotArea;
import dtb.terminalIO.components.plot.shape.Circle;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.geom.Dimension;

/**
 * A PlotArea that is not a chart: planets going round the sun, redrawn ten times a second. Equal
 * scaling keeps the orbits round at any window size (drag the window's corner). Click a planet, or
 * its name, to read about it; the buttons change the speed or pause.
 * <p/>
 * Everything is drawn in the plot's own units (the sun at 0, 0; orbits up to 5) with ordinary
 * library shapes - a filled Circle per planet, an outline Circle per orbit - and labels that click as
 * their planet (pen.text(x, y, name, planet)).
 */
public class OrbitsDemo {

    /** a planet: its orbit's radius, its year (in Earth years), its size and colour */
    static class Planet {
        final String name;
        final double orbit;
        final double year;
        final float size;
        final Color color;
        final String about;

        Planet(String name, double orbit, double year, float size, Color color, String about) {
            this.name = name;
            this.orbit = orbit;
            this.year = year;
            this.size = size;
            this.color = color;
            this.about = about;
        }
    }

    static final Planet[] PLANETS = {
        new Planet("Mercury", 0.9, 0.24, 0.10f, Colors.BBla, "the smallest, a year of 88 days"),
        new Planet("Venus",   1.5, 0.62, 0.16f, Colors.BYel, "the hottest, under thick clouds"),
        new Planet("Earth",   2.2, 1.00, 0.17f, Colors.BBlu, "home - the moon goes round it every 27 days"),
        new Planet("Mars",    3.0, 1.88, 0.13f, Colors.BRed, "red with iron rust, two small moons"),
        new Planet("Jupiter", 4.3, 11.9, 0.32f, Colors.Yel,  "the biggest - more than all the others together"),
    };

    public static void main(String[] args) throws IOException {
        new TUIApp(args)
                .setEmptyChar(' ')
                .setDefaultMainApplicationPanel()
                .confirmOnExit()
                .setOnReadyStart((tApp) -> {

                    double[] time = { 0 };                       // in Earth years
                    double[] speed = { 0.02 };                   // years per tick
                    boolean[] paused = { false };
                    boolean[] displayName = { true };

                    TextComponent status = new TextComponent(" Click a planet (or its name)");

                    PlotArea plot = new PlotArea(Colors.Gre, new Dimension.Simple(24, 80))
                            .showing(-5, 5, -5, 5)
                            .scaling(PlotArea.Scaling.EQUAL)
                            .onDraw((pen) -> drawSystem(pen, time[0], displayName[0]));
                    plot.onClick((click) -> {
                        for (Planet p : PLANETS) {
                            if (click.shape() != null && p.name.equals(click.shape().getName())) {
                                status.setTextAndResize(" " + p.name + ": " + p.about + String.format("  (year: %.2f Earth years)", p.year));
                            }
                        }
                        if (click.shape() == null) {
                            status.setTextAndResize(String.format(" Empty space at %.1f, %.1f", click.at().getX(), click.at().getY()));
                        }
                        click.eventBus().raiseEvent(new RequestUpdateCanvasEvent(status));
                    });

                    ButtonBar controls = new ButtonBar()
                                               .setLeftMostBorderChar((char)0)
                                               .setRightMostBorderChar((char)0);
                    controls.addButton("slower", null, (te) -> speed[0] /= 2);
                    controls.addButton("faster", null, (te) -> speed[0] *= 2);
                    controls.addButton("pause / go", null, (te) -> paused[0] = !paused[0])
                             .setToggleButton(true);

                    controls.addButton("names", null, (te) -> displayName[0] = !displayName[0])
                             .setToggleButton(true)
                             .setValue(true);

                    tApp.getEventBus().appendEventHandler(new OnIdleEventEventHandler.RepeatingInterval(100, (te) -> {
                        if (paused[0]) {
                            return;
                        }
                        time[0] += speed[0];
                        plot.redraw();
                        te.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(plot));
                    }));

                    HorizontalPanel controlsPanel = new HorizontalPanel();
                    controlsPanel.add(new TextComponent(" Orbits: "));
                    controlsPanel.add(controls);
                    VerticalPanel windowFooter = new VerticalPanel(Colors.IBla);
                    windowFooter.add(controlsPanel);
                    windowFooter.add(status);
                    windowFooter.setTitle("Controls:");

                    WindowFrame window = new WindowFrame(Colors.Gre, "The inner planets and Jupiter (not to scale) - drag the corner to resize", plot);
                    window.setFooter(windowFooter);
                    tApp.getMainApplicationPanel().add(2, 0, window);
                })
                .start();
    }

    /** the sun, each orbit, each planet where it is at this time (and Earth's moon), each labelled */
    static void drawSystem(Pen pen, double time, boolean displayName) {
        pen.color(Colors.BYel).draw(new Circle(0f, 0f, 0.35f).setFilled(true));
        for (Planet p : PLANETS) {
            pen.color(Colors.BBla).draw(new Circle(0f, 0f, (float) p.orbit));
        }
        for (Planet p : PLANETS) {
            double angle = 2 * Math.PI * time / p.year;
            double x = Math.cos(angle) * p.orbit;
            double y = Math.sin(angle) * p.orbit;
            Circle planet = new Circle((float) x, (float) y, p.size).setFilled(true);
            planet.setName(p.name);
            pen.color(p.color).draw(planet);
            if (displayName) {
                pen.text(x + p.size + 0.15, y, p.name, planet);
            }
            if ("Earth".equals(p.name)) {
                double moonAngle = 2 * Math.PI * time / 0.075;
                pen.color(Colors.BWhi).point(x + Math.cos(moonAngle) * 0.35, y + Math.sin(moonAngle) * 0.35);
            }
        }
    }
}
