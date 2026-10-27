package mizzen.examples;

import java.io.IOException;
import java.util.function.Consumer;

import mizzen.app.TUIApp;
import mizzen.components.Component;
import mizzen.components.TextComponent;
import mizzen.components.composite.HorizontalPanel;
import mizzen.components.input.Button;
import mizzen.components.input.MultiLineTextInput;
import mizzen.components.modal.MessageBox;
import mizzen.components.scroll.ScrollableComponent;
import mizzen.components.scroll.SearchFunctionForScrollableComponent;
import mizzen.controlcodes.color.Colors;
import mizzen.events.EventBus;
import mizzen.events.focus.FocusManager;
import mizzen.events.keyboard.Keys;
import mizzen.examples.sampledata.ReadSampleFiles;
import mizzen.geom.Dimension;
import mizzen.geom.position.Position;
import mizzen.util.Logging;
import mizzen.util.StringUtils;

public class MultiLineTextInputTest {

    public static void main(String[] args) throws IOException {

        Logging.setEnabled(true);
        
        new TUIApp(args)
        .setDefaultMainApplicationPanel()
        .confirmOnExit()
        .setOnReadyStart((tApp)-> {

            HorizontalPanel hp = new HorizontalPanel().setHorizontalSpacer(0);

            hp.add(new TextComponent(Colors.ICya,  "MultiLineTextInput tester "));

            Button infoButton = new Button(Colors.ICya, "(?)");
            hp.add(infoButton);

            Button quitButton = new Button(Colors.ICya, "Quit");
            hp.add(quitButton);
            quitButton.setHotKey(Keys.CTRL_C_KEYSTROKE);
            quitButton.setDisplayHotKey(true);
            quitButton.onChange(tApp.getTerminateAppHotKeyEventHandler());

            hp.add(new TextComponent(Colors.IBla," ⣿⣿⣿⣿"));

            Button wordWrapButton = new Button(Colors.IGre, "(Word-wrap)", true); // true mean toggle button
            hp.add(wordWrapButton);

            Button loadSmallText = new Button(Colors.IGre, "(Small Text)");
            hp.add(loadSmallText);

            Button loadLoremIpSumSmall = new Button(Colors.IGre, "(Small LoremIpSum)");
            hp.add(loadLoremIpSumSmall);

            Button loadLoremIpSumMedium = new Button(Colors.IGre, "(Medium LoremIpSum)");
            hp.add(loadLoremIpSumMedium);

            Button loadLoremIpSumLarge = new Button(Colors.IGre, "(Large LoremIpSum)");
            hp.add(loadLoremIpSumLarge);

            String smallText = "Silly Sally sold\nSeaShells at the seashore\nWhat a Silly Sally thing to do!";

            MultiLineTextInput mTextBox = new MultiLineTextInput(); 
            mTextBox.setText(smallText);

            mTextBox.setColor(  new Colors.FGBGColors( Colors.BBlu, Colors.On_Bla  )  );

            ScrollableComponent sc = new ScrollableComponent(Colors.Yel, 
                                                               (Dimension)tApp.getMainApplicationPanel(), 
                                                                mTextBox);
            sc.setFillOnClear(true);
            sc.setEmptyChar('.');

            SearchFunctionForScrollableComponent.addFinderToScrollable(sc.asSearchableView());

            tApp.getMainApplicationPanel().add(0,0, hp);
            
            tApp.getMainApplicationPanel().add(2,0, sc);
            
            
            infoButton.onChange((te)->{
                MessageBox.showInfoDialog(te.getEventBus(), 
                                          tApp.getMainApplicationPanel(), 
                                                  "   Part of the Mizzen Library\n"+
                                                  "   --------------------------\n"+
                                                  "\n"+
                                                  "This mini application is used to test\n"+
                                                  "MultiLineTextInput component in conjunction\n"+
                                                  "with the ScrollableComponent and also \n"+
                                                  "with the external findText button.\n"+
                                                  "---------------------------------------------------- \n"+
                                                  "Cursor management (positioning) should be maintained\n"+
                                                  "event when paging/scrolling up down with controls\n"+
                                                  "of the ScrollableComponent"
                                          );
            });
            
            Consumer<EventBus> wrapIfRequest = (evtBus) ->{
                if (!wordWrapButton.getValue()) {
                    return;
                }
                int maxLineLength = sc.getViewPortAsRectangle().getWidth();
                mTextBox.setText(evtBus, StringUtils.wrap(mTextBox.getValue(), maxLineLength));
            };
            
            wordWrapButton.onChange((te)->{
                wrapIfRequest.accept(te.getEventBus());
            });

            loadSmallText.onChange((te)->{
                getTextFromNameAndSetTitle(te.getEventBus(),sc, "Small Text", mTextBox, smallText, wordWrapButton.getValue());
            });
            loadLoremIpSumSmall.onChange((te)->{
                getTextFromNameAndSetTitle(te.getEventBus(),sc, "Small LoremIpSum", mTextBox, getTextFromName("LoremIpsumSmall.txt"), wordWrapButton.getValue());
            });
            loadLoremIpSumMedium.onChange((te)->{
                getTextFromNameAndSetTitle(te.getEventBus(),sc, "Medium LoremIpSum", mTextBox, getTextFromName("LoremIpsumMedium.txt"), wordWrapButton.getValue());
            });
            loadLoremIpSumLarge.onChange((te)->{
                getTextFromNameAndSetTitle(te.getEventBus(),sc, "Large LoremIpSum", mTextBox, getTextFromName("LoremIpsumLarge.txt"), wordWrapButton.getValue());
            });
            
            sc.resize(  new Dimension.Simple(sc.getHeight()-2, sc.getWidth()));
            
            
            tApp.appendOnResizeAction((tApp2, size)->{
                sc.resize(  new Dimension.Simple(size.getHeight()-2, size.getWidth()));
                tApp2.getEventBus().asynchAfter(100, (te2)->{
                    wrapIfRequest.accept( tApp2.getEventBus() );
                });
            });
            
            
            FocusManager.UTIL.verifyPathsToFocusables(tApp.getApplicationFocusManager(), (Component<?>)tApp.getMainApplicationPanel());



        }).start();
    }

    static void getTextFromNameAndSetTitle(EventBus eventBus, 
                                           ScrollableComponent hasTitle, 
                                           String title, 
                                           MultiLineTextInput mTextBox , 
                                           String textContent,
                                           boolean wordWrapContent) {
        hasTitle.setTitle(title);
        mTextBox.setCaretPosition(Position.ZERO);
        if (wordWrapContent) {
            int maxLineLength = hasTitle.getViewPortAsRectangle().getWidth();
            textContent = StringUtils.wrap(textContent, maxLineLength);
        }
        mTextBox.setText(eventBus, textContent);
        mTextBox.setCaretPosition(Position.ZERO);
    }

    static String getTextFromName(String textResourceName) {
        String textContent = "not loaded";
        try {
            textContent = ReadSampleFiles.readSampleFile(textResourceName);
        } catch (IOException e) {
            textContent = e.toString();
        }  
        return textContent;
    }

}
