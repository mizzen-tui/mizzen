package dtb.terminalIO.examples;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.ButtonBar;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.scroll.ScrollableComponent;
import dtb.terminalIO.controlcodes.Colors;
import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.core.ScreenCanvasBackend;
import dtb.terminalIO.core.ScreenCanvasBackend.UpdateStrategy;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.util.Logging;

public class ReadTextFileToScreenTest {
    public static void main(String[] args) throws Exception {
        
        if (args.length == 0) {
            System.err.println("Filename not specified");
            System.exit(1);
        }
        
        Logging.setEnabled(true);
        
        String fileName = args[0]; // TODO: validation required (file exists etc)
        
        byte[] fileByteData = Files.readAllBytes(Paths.get(fileName));
        
        //String fileData  =  new String(fileByteData,StandardCharsets.UTF_8);
        String fileData = new String(fileByteData,StandardCharsets.UTF_8).replace(ControlCode.ERASE_END_OF_LINE.asString(),"");
        
        new TUIApp(args)
        .setCanvasSize(24, 100)
        .setEmptyChar(' ')
        .setMainApplicationPanel(new Composite().setBounds(30, 120).setEmptyChar(' ').setBorder(false))
        .confirmOnExit()
        .setOnReadyStart((tApp)-> {

            AtomicBoolean stripColorCodes = new AtomicBoolean(false);
            
            TextComponent image = new TextComponent(fileData);
            image.setFillOnClear(false);
            
            ButtonBar bannerPanel = new ButtonBar().setHorizontalSpacer(0);
            bannerPanel.setBorder(false);
            bannerPanel.setNoBorderChars();
            
            bannerPanel.add(new TextComponent("Demonstrate reading color codes from text file"));
            bannerPanel.addButton(Colors.IGre,"CTRL-C",Keys.CTRL_C_KEYSTROKE, (te) -> tApp.getTerminateAppHotKeyEventHandler().handle(te)  );
            bannerPanel.add(new TextComponent(" to quit "));

            bannerPanel.addButton(Colors.IGre,"No colors",Keys.CTRL_D_KEYSTROKE, (te) -> {
                stripColorCodes.getAndSet(!stripColorCodes.get());
                String data = fileData;
                if (stripColorCodes.get()) {
                    data = ControlCode.removeColorCodes(data);
                }
                image.setText(data);
                Logging.info("▀ ▀ ▀ ▀ ▀ ▀ \n\n"+data+"\n\n▀ ▀ ▀ ▀ ▀ ▀");
                if (data.contains("▀")) {
                    Logging.info("YES!!");
                }
                tApp.doRefreshCanvasIfModified();
            }  );
            
            
            ScrollableComponent sc = new ScrollableComponent(Colors.Whi, 
                                                             (Dimension)tApp.getMainApplicationPanel(), 
                                                             image,
                                                             ()-> new DefaultFocusManager(Keys.SHIFT_TAB_KEYSTROKE, Keys.TAB_KEYSTROKE).setNavKeysFirst(false));
            sc.setFillOnClear(false);

            sc.resize(  new Dimension.Simple(sc.getHeight()-4, sc.getWidth()));

            tApp.getMainApplicationPanel().add(1,1, bannerPanel);

            tApp.getMainApplicationPanel().add(4,0, sc);

            FocusManager mainFocusManager = FocusManager.UTIL.buildFocusManagerFromComponent(tApp.getMainApplicationPanel(),
                    tApp.getEventBus(),
                    ()-> new DefaultFocusManager().setNavKeysFirst(false).setCycling(false));

            tApp.getEventBus().appendEventHandler( mainFocusManager );

            BiConsumer<TUIApp, Dimension> baseResizeAction = tApp.getResizeAction();

            tApp.setOnResizeAction( (ptApp, newSize) ->{
                baseResizeAction.accept(ptApp, newSize);
                sc.resize(  new Dimension.Simple(newSize.getHeight()-4, newSize.getWidth()));
            } );
            
            
            ((ScreenCanvasBackend)tApp.getCanvas().getCanvasBackend()).setUpdateStrategy(UpdateStrategy.UPDATE_CHANGED);

            FocusManager.UTIL.verifyPathsToFocusables(mainFocusManager, (Component<?>)tApp.getMainApplicationPanel());

        }).start();

    }

}
