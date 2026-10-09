package dtb.terminalIO.examples.plot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.ButtonBar;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.WindowFrame;
import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.components.input.RadioGroup;
import dtb.terminalIO.components.plot.PlotArea;
import dtb.terminalIO.components.plot.shape.catalog.Star;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.position.Vec2;

/**
 * A PlotArea: sine and cosine, the area under the sine, sample points, a label, and a star you can
 * drag - all in the plot's own numbers (x 0..2 pi, y up); drag empty space to move the view. Click anything: the status line says what
 * and where. The window's footer holds the controls: < > ^ v pan, + / - zoom, and the scaling.
 * Drag the window's corner and the plot redraws at the new size.
 */
public class PlotAreaDemo {

    public static void main(String[] args) throws IOException {

        new TUIApp(args)
                .setEmptyChar(' ')
                .setDefaultMainApplicationPanel()
                .confirmOnExit()
                .setOnReadyStart((tApp) -> {

                    TextComponent status = new TextComponent(" Click the plot - drag the star");

                    List<Vec2<?>> samples = new ArrayList<Vec2<?>>();
                    for (int i = 0; i <= 12; i++) {
                        double x = i * Math.PI / 6;
                        samples.add(new Vec2.BaseVec2((float) x, (float) (Math.sin(x) * 0.8)));
                    }

                    Star star = new Star(4.5f, 0.9f, 0.35f, 0.15f)
                            .setYUp(true)                              // a plot's y goes up
                            .setFilled(true);
                    star.setName("star");

                    PlotArea plot = new PlotArea(Colors.Gre, new Dimension.Simple(22, 90))
                            .showing(0, 2 * Math.PI, -1.5, 1.5)
                            .axes(true)
                            .axesColor(Colors.BBla)
                            .onDraw((pen) -> pen
                                    .color(Colors.Blu).fillUnder(Math::sin, 0, Math.PI)
                                    .color(Colors.BCya).function(Math::sin)
                                    .color(Colors.BYel).function((x) -> Math.cos(x) / 2)
                                    .color(Colors.BRed).points(samples)
                                    .color(Colors.BPur).draw(star)
                                    .color(Colors.BWhi).text(Math.PI / 2 - 0.2, 1.2, "peak"));
                    plot.draggable(star);
                    plot.panByDrag(true);                              // drag empty space: move the view
                    plot.onClick((click) -> {
                        status.setTextAndResize(String.format(" clicked %s at x=%.2f  y=%.2f",
                                click.shape() == null ? "nothing" : click.shape().getName(), click.at().getX(), click.at().getY()));
                    });

                    RadioGroup<String> scaling = RadioGroup.horizontal("stretch", "equal", "none");
                    scaling.onChange((te) -> plot.scaling(PlotArea.Scaling.valueOf(scaling.getValue().toUpperCase())));

                    ButtonBar pan = new ButtonBar();
                    pan.addButton("<", null, (te) -> plot.pan(-stepX(plot), 0));
                    pan.addButton(">", null, (te) -> plot.pan(stepX(plot), 0));
                    pan.addButton("^", null, (te) -> plot.pan(0, stepY(plot)));
                    pan.addButton("v", null, (te) -> plot.pan(0, -stepY(plot)));

                    ButtonBar zoom = new ButtonBar();
                    zoom.addButton("+", null, (te) -> plot.zoom(1.5));
                    zoom.addButton("-", null, (te) -> plot.zoom(1 / 1.5));

                    HorizontalPanel controls = new HorizontalPanel();
                    controls.add(new TextComponent(" "));
                    controls.add(scaling);
                    controls.add(new TextComponent("   zoom "));
                    controls.add(zoom);
                    controls.add(new TextComponent("   pan "));
                    controls.add(pan);

                    // the window's border and title, and its corner to resize it: the plot redraws at the new size
                    WindowFrame window = new WindowFrame(Colors.Gre, "PlotArea: your numbers, y up - drag the corner to resize", plot);
                    window.setFooter(controls);                       // the plot's controls along the window's bottom

                    // straight on the main panel (the terminal's size): a panel around the window would cut it off as it grows
                    tApp.getMainApplicationPanel().add(0, 0, status);
                    tApp.getMainApplicationPanel().add(1, 0, window);
                })
                .start();
    }

    /** a tenth of the window */
    private static double stepX(PlotArea plot) {
        return (plot.getXMax() - plot.getXMin()) / 10;
    }

    private static double stepY(PlotArea plot) {
        return (plot.getYMax() - plot.getYMin()) / 10;
    }
}
