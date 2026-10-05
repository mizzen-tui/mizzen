package dtb.terminalIO.examples.plot.image;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.geom.Dimension;

/**
 * Fractals in ASCII (or Braille, or colour blocks): a new random fractal every second, full screen -
 * converting Java images into characters. The fractals and their render controls are a component,
 * FractalAnimation (also used by TreeViewTest).
 */
public class FractalsViewer {

    public static void main(String[] args) {
        new TUIApp(args)
        .setCanvasSize(new Dimension.Simple(50, 120))
        .setDefaultMainApplicationPanel()
        .confirmOnExit()
        .enableMouseHandling()
        .setOnReadyStart((tApp) -> {
            FractalAnimation fractals = new FractalAnimation(tApp.getCanvas(), 1000);
            tApp.getMainApplicationPanel().add(fractals.component());
            fractals.start(tApp.getEventBus());
            tApp.appendOnResizeAction((app, size) -> fractals.resize(size));
        })
        .start();
    }
}
