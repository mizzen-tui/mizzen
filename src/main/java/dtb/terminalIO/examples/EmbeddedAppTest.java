package dtb.terminalIO.examples;

import dtb.terminalIO.app.Embedding;
import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.examples.plot.OrbitsDemo;
import dtb.terminalIO.examples.plot.PlotAreaDemo;
import dtb.terminalIO.examples.plot.TestCube;
import dtb.terminalIO.examples.plot.TestScale;
import dtb.terminalIO.examples.plot.TestSprites;
import dtb.terminalIO.examples.plot.image.FractalsViewer;
import dtb.terminalIO.geom.Dimension;

/**
 * Other examples running inside this one: pick one on the left and it runs on the right - its own
 * main, unchanged, through TUIApp.runEmbedded. Picking another, or Close, takes the running one out,
 * with everything it put on the event bus; the status line shows how many handlers that is.
 * <p/>
 * Things to try: TAB from the buttons into the embedded example and back; Button bar's F1 / F2 / F3
 * (its hot keys work wherever focus is, as they do when it runs alone); Fractals' timer stopping
 * when it is closed; resizing the terminal.
 */
public class EmbeddedAppTest {

    //private static final int LEFT_WIDTH = 18;

    private static Embedding running;

    public static void main(String[] args) {
        new TUIApp(args)
        .setDefaultMainApplicationPanel()
        .setOnReadyStart((host) -> {
            Composite main = (Composite) host.getMainApplicationPanel();

            VerticalPanel vPanel = new VerticalPanel();
            HorizontalPanel hPanel = new HorizontalPanel();
            
            // right: where the examples run - the embedded app gets the inside of its border
            Composite slot = new Composite();
            slot.setBorder(true);
            // vPanel.add(0, LEFT_WIDTH, slot);
            // vPanel.onResizeExpandDownAndRight(slot);
            
            TextComponent status = new TextComponent(new Colors.FGBGColors(Colors.BRed, Colors.On_Yel),
                                                     new Dimension.Simple(1, main.getWidth()));
            
            
            vPanel.add(hPanel);
            vPanel.add(status);            
//            vPanel.onResizeExpandDownAndRight(hPanel);
             vPanel.appendOnResize((size)->{
                  // resize, not setBounds: resize also runs hPanel's own rules (slot)
                  hPanel.resize(new Dimension.Simple(size.getHeight()-1, size.getWidth()));
             });
            vPanel.onResizeExpandRight(status);

            main.add(vPanel);
            main.onResizeExpandDownAndRight(vPanel);


            // left: the examples 
            VerticalPanel picks = new VerticalPanel();
            picks.setTitle(Colors.IGre, "Examples");
            picks.add(pick("Dropdowns", host, slot, status, () -> DropdownListBoxSimpleTest.main(new String[0])));
            picks.add(pick("TabPanel", host, slot, status, () -> TabPanelTest2.main(new String[0])));
            picks.add(pick("Button bar", host, slot, status, () -> ButtonBarTest.main(new String[0])));
            picks.add(pick("Fractals", host, slot, status, () -> FractalsViewer.main(new String[0])));
            picks.add(pick("Sprites", host, slot, status, () -> TestSprites.main(new String[0])));
            picks.add(pick("Shapes Testing", host, slot, status, () -> TestScale.main(new String[0])));
            picks.add(pick("3D Shapes Testing", host, slot, status, () -> TestCube.main(new String[0])));
            picks.add(pick("Window Frame", host, slot, status, () -> WindowFrameTest.main(new String[0])));
            picks.add(pick("Plot-Area-Demo", host, slot, status, () -> PlotAreaDemo.main(new String[0])));
            picks.add(pick("Orbits", host, slot, status, () -> OrbitsDemo.main(new String[0])));
            picks.add(pick("Parts shop", host, slot, status, () -> PartsShopDemo.main(new String[0])));
            picks.add(pick("TreeView", host, slot, status, () -> TreeViewTest.main(new String[0])));
            Button close = new Button("Close");
            close.onChange((te) -> {
                closeRunning(status, "closed");
            });
            picks.add(new BorderedComponent(close));

            hPanel.add(picks);
            hPanel.add(slot);
            hPanel.onResizeExpandDownAndRight(slot);


            status.setText(" Pick an example on the left");

            slot.appendOnResize((size) -> {
                if (running != null) {
                    running.resize();
                }
            });

            // the rules above run on a resize: run them once now, for the starting size
            main.applyResizeRules();
            

        })
        .start();
    }

    /** a button running the example's main inside slot */
    private static BorderedComponent pick(String name, TUIApp host, Composite slot, TextComponent status, Embedding.Launch launch) {
        Button button = new Button(name);
        button.onChange((te) -> {
            closeRunning(status, "closed");
            Embedding embedding = TUIApp.runEmbedded(host, slot, launch);
            running = embedding;
            // its setup code runs next on the bus; count after it
            host.getEventBus().asynch((after) -> {
                status.setText(" Running " + name + " - its handlers on the bus: " + embedding.handlersOnHostBus());
            });
        });
        return new BorderedComponent(button);
    }

    private static void closeRunning(TextComponent status, String what) {
        if (running == null) {
            return;
        }
        int handlers = running.handlersOnHostBus();
        running.close();
        status.setText(" " + what + ": took " + handlers + " handlers off the bus, " + running.handlersOnHostBus() + " left");
        running = null;
    }
}
