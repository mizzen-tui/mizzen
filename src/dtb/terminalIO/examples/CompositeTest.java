package dtb.terminalIO.examples;

import dtb.terminalIO.Canvas;
import dtb.terminalIO.Colors;
import dtb.terminalIO.ControlCode.FGBGColors;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.TextComponent;

public class CompositeTest {
    public static void main(String[] args) {
        // TODO Auto-generated method stub

        Canvas master_canvas = TestsCommon.getMasterCanvasForTesting(TestsCommon.getTerminalScreenForTesting(args));
        master_canvas.setEmptyChar(' ');

        master_canvas.t("HELLO WORLD\nWhat can be\ngoing on here!?");

        Composite textC = new Composite()
                                .setBounds(25, 50 )
                                .setTitle(new FGBGColors(Colors.Red, Colors.On_IYel), " Poem ")
                                .add(new TextComponent(null, "The rain in Spain\n" + 
                                                             "stays mainly on the plain\n" + 
                                                             "and where's that blasted plain?"));
        
        Composite text2 = new Composite()
                .setBounds(25, 50 )
                .setTitle(new FGBGColors(Colors.Red, Colors.On_IYel), " Poem ")
                .add(new TextComponent(null, "In Spain .. In Spain\n" + 
                                             "The Rain in Spain stays \n" + 
                                             "mainly on the plain!")).t(6,4, "Texxxxxt"); //.t("1-one\n2-two\n3-three\n!");
        
       // master_canvas.addComponent(2, 30, textC);
        
        master_canvas.addComponent(7, 35, text2);
        
                                
//        Composite box = new Composite()
//                                 .setPosition(5, 40).add(new BoundedComponent.DONOTHING(10,20)).setBorder(true);
//                                 
//        Composite box2 = new Composite()
//                          .setPosition(10, 45).add(new BoundedComponent.DONOTHING(12,20)).setBorder(true);
//                
//
//        master_canvas.addComponent(0, 0, box);
//        
//        master_canvas.addComponent(5, 5, box2);
                
        master_canvas.refresh();

    }
}
