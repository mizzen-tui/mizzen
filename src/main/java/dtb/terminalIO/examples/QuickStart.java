package dtb.terminalIO.examples;

import java.io.IOException;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.WindowFrame;
import dtb.terminalIO.components.plot.PlotArea;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.geom.Dimension;

/**
 * The README's quick start: a sine wave in a window you can drag and resize, with zoom and pan
 * buttons. Kept short on purpose - the README shows this file as it is.
 */
public class QuickStart {

    public static void main(String[] args) throws IOException {
        new TUIApp(args)
                .setDefaultMainApplicationPanel()
                .setEmptyChar(' ')
                .confirmOnExit()
                .setOnReadyStart((tApp) -> {
                    PlotArea plot = new PlotArea(Colors.BCya, new Dimension.Simple(14, 72))
                            .showing(0, 4 * Math.PI, -1.5, 1.5)                // x from..to, y from..to
                            .axes(true)
                            .axesColor(Colors.BBla)
                            .onDraw((pen) -> pen.function(Math::sin));

                    WindowFrame window = new WindowFrame(Colors.Gre, "Hello, terminal", plot);
                    window.setFooterPanel(plot.navigationControls());          // scaling, zoom, pan
                    tApp.getMainApplicationPanel().add(1, 2, window);
                })
                .start();
    }
}
