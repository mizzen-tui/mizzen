package mizzen.examples;

import java.io.IOException;

import mizzen.app.TUIApp;
import mizzen.components.TextComponent;
import mizzen.components.frameset.HorizontalSplitter;
import mizzen.components.frameset.VerticalSplitter;
import mizzen.controlcodes.color.Colors;
import mizzen.examples.sampledata.ReadSampleFiles;
import mizzen.geom.Dimension;
import mizzen.util.Logging;
import mizzen.util.StringUtils;

public class VerticalSplitterTest2 {


    public static void main(String[] args) throws Exception {

        Logging.setEnabled(true);

        new TUIApp(args)
        .setEmptyChar(' ')
        .setDefaultMainApplicationPanel()
        .confirmOnExit()
        .setOnReadyStart((tApp)-> {

            // no sizes: the outer splitter fills the screen, the inner one (left) gets half its width;
            // the top text keeps its own height and the bottom one takes the rest
            HorizontalSplitter hzp = new HorizontalSplitter(Colors.BCya);

            VerticalSplitter vzp = new VerticalSplitter(Colors.BCya);

            vzp.addComponent(new TextComponent(Colors.IGre,"TOP\n This is the top part\nSo there!"));
            vzp.addComponent(new TextComponent("BOTTOM\n"+readLargeSampleFile(30)));

            hzp.setTitle(Colors.IBla, "Vertical Splitter Test");

            hzp.addComponent(vzp);
            hzp.addComponent(new TextComponent(Colors.IGre,"RIGHT\n"+readLargeSampleFile(30)));

            hzp.resize(new Dimension.Simple(tApp.getDimensions().getHeight() - 2, tApp.getDimensions().getWidth() - 2));
            tApp.getMainApplicationPanel().add(1,1,hzp);
            tApp.appendOnResizeAction((ta, size) -> hzp.resize(new Dimension.Simple(size.getHeight() - 2, size.getWidth() - 2)));

        }).start();
    }

    static String readLargeSampleFile(int wrapLength) {
        String bigFile = "LoremIpsumLarge.txt";
        String largeText = null;
        try {
            largeText = ReadSampleFiles.readSampleFile(bigFile);  
            largeText = StringUtils.wrap(largeText, wrapLength);

            largeText = StringUtils.addLineNumbers(largeText);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return largeText;
    }
    
    
    
}
