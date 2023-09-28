package dtb.terminalIO.examples;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.components.input.DisplayableValue.StringDisplayValue;
import dtb.terminalIO.components.input.DropdownListBox;
import dtb.terminalIO.components.input.ListBox;
import dtb.terminalIO.components.input.ListBox.TextComponentItemFactory;
import dtb.terminalIO.components.scroll.ScrollableComponent;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.VAlign;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.geom.position.PositionRelativeFactory;
import dtb.terminalIO.util.Logging;

public class ListBoxTest2 {

    public static void main(String args[]) {
        
        Logging.setEnabled(true);
        
        int appWidth = 130;
        int appHeight = 30;
        
        
        new TUIApp(args)
        .setCanvasSize(appHeight, appWidth)
        //.setEmptyChar(' ')
        .setDefaultMainApplicationPanel()
        .confirmOnExit()
        .setOnReadyStart((tApp)-> {
            
            Composite mainPanel = (Composite) tApp.getMainApplicationPanel();
            
            TextComponent status = new TextComponent("         ");
            Position statusPosition = PositionRelativeFactory.fromVAlign(VAlign.BOTTOM, mainPanel, status, 2);
            
            mainPanel.add(statusPosition, status);
            
            mainPanel.setTitle(new Colors.FGBGColors(Colors.BGre,Colors.On_Blu), "ListBox Testing     ");
            
            HorizontalPanel hpMain = new HorizontalPanel().setHorizontalSpacer(4);
            
            ListBox<String> lb1 = ListBox.fromStrings(Colors.IGre,LIST_DATA.LARGE_LIST);
            lb1.setBorder(false);
            ScrollableComponent lb1_sc = new ScrollableComponent(Colors.Blu, new Dimension.Simple(Math.min(15,lb1.getHeight()-5), lb1.getWidth()+20), lb1);
            //SearchFunctionForScrollableComponent.addFinderToScrollable(lb1_sc);  
            lb1.onChange((te)-> status.setTextAndResize( lb1.getValue() ) );
            
            
            ListBox<String> lb2 = new ListBox<String>(Colors.IGre, LIST_DATA.WEIRD, new TextComponentItemFactory<String>());
            lb2.setTitle("Child list");
            lb2.setName("Child list");
            
            DropdownListBox<String> lb3 = DropdownListBox.fromStrings(new String[] {"Test1", "Test2", "No tests", "No more teacher's","dirty looks"}, new TextComponentItemFactory<String>());
            DropdownListBox<Boolean> lb4 = DropdownListBox.yesNoDropDown();
            
            hpMain.add(lb1_sc);
            hpMain.add(lb2);
            
            VerticalPanel droppablesPanel = new VerticalPanel();

            droppablesPanel.add(lb3);
            droppablesPanel.add(lb4);

            hpMain.add(droppablesPanel);

            tApp.getMainApplicationPanel().add(2,2,hpMain);

        }).start();
        
    }
    
    static class LIST_DATA {
        
        public static StringDisplayValue[] WEIRD = StringDisplayValue.fromStringValues(new String[] {
                "I am", 
                "Forever known", 
                "As the man",
                "with one red shoe"
                });
        
        public static StringDisplayValue[] RAINDEER = StringDisplayValue.fromStringValues(new String[] {
                "Comet", 
                "Blitzen", 
                "Donner",
                "Rudolf"
                });
        public static StringDisplayValue[] IFACES = StringDisplayValue.fromStringValues(new String[] {
                "lo", 
                "eth0",
                "virbr0", 
                "virbr0-nic", 
                "docker0"
                });

        public static String[] LARGE_LIST = new String[] {
                "Blue Suede Shoes", 
                "Red Shoes", 
                "Pink Shoes",
                "Pink Flamingos",
                "T'was brilling and the slithy toth\ndid gere and gimble in the wabe.\nAll mimsey were the borogroves\nAnd the momewraths outgrab",
                "Moss filled turtles",
                "Santa Claus\nIs quite fat!",
                "Mrs Claus\nis OK with that!",
                "Let's agree on one thing",
                "We don't talk about Rudolf",
                "I  mean come on..\nHe's got a red nose",
                "He's a red nosed reindeer after all",
                "1\n2\n3\n4\n5\6\n6\n7\n8\n9\n10\n11\n12\n13\n14\n15\n16",
                "What?",
                "You thought we were done?",
                "NO!",
                "WAY!",
                "Let's make this list crazy long!",
                "We'll do that by repeating it",
                "Here is the demarker",
                "-----------------------",
                "What?",
                "You thought we were done?",
                "NO!",
                "WAY!",
                "Let's make this list crazy long!",
                "We'll do that by repeating it",
                "Here is the demarker",
                "-----------------------",
                "What?",
                "You thought we were done?",
                "NO!",
                "WAY!",
                "Let's make this list crazy long!",
                "We'll do that by repeating it",
                "Here is the demarker",
                "-----------------------",
                "What?",
                "You thought we were done?",
                "NO!",
                "WAY!",
                "Let's make this list crazy long!",
                "We'll do that by repeating it",
                "Here is the demarker",
                "-----------------------",
                "What?",
                "You thought we were done?",
                "NO!",
                "WAY!",
                "Let's make this list crazy long!",
                "We'll do that by repeating it",
                "Here is the demarker",
                "-----------------------",
                "What?",
                "You thought we were done?",
                "NO!",
                "WAY!",
                "Let's make this list crazy long!",
                "We'll do that by repeating it",
                "Here is the demarker",
                "-----------------------",
                "What?",
                "You thought we were done?",
                "NO!",
                "WAY!",
                "Let's make this list crazy long!",
                "We'll do that by repeating it",
                "Here is the demarker",
                "-----------------------",
                "What?",
                "You thought we were done?",
                "NO!",
                "WAY!",
                "Let's make this list crazy long!",
                "We'll do that by repeating it",
                "Here is the demarker",
                "-----------------------",
                "What?",
                "You thought we were done?",
                "NO!",
                "WAY!",
                "Let's make this list crazy long!",
                "We'll do that by repeating it",
                "Here is the demarker",
                "-----------------------",
                "What?",
                "You thought we were done?",
                "NO!",
                "WAY!",
                "Let's make this list crazy long!",
                "We'll do that by repeating it",
                "Here is the demarker",
                "-----------------------"
                };
        
    }
    
    
    
}
