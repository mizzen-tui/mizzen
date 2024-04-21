package dtb.terminalIO.examples;

import java.util.function.Function;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.events.OnTerminateEventHandler;
import dtb.terminalIO.util.Logging;

public class TestLotsOfTerminateHandlers {
    
    public static void main(String[] args) {
        
        Logging.setEnabled(true);
        
        new TUIApp(args)

        .setEmptyChar(' ')
        .setDefaultMainApplicationPanel()
        .confirmOnExit()
        .setOnReadyStart((tApp)-> {
            
              TextComponent tc = new TextComponent("Testing terminate handlers\nCTRL-C to exit\n\nCheck logs for terminate actions output");
              
              tApp.getMainApplicationPanel().add(10,10, tc);

              Logging.info("TestLotsOfTerminateHandlers started");
              
              int counterBucket[] = {0};
              
              Function<Integer, Runnable> makeTerminateHandler = (n) -> {
                  return ()->{
                      int bucketAtStart = counterBucket[0];
                      counterBucket[0]++;
                      Logging.info("TestLotsOfTerminateHandlers.OnTerminateEventHandler("+n+")-start (ctr="+bucketAtStart+")");
                      try {
                          Thread.sleep(1000);
                          Logging.info("TestLotsOfTerminateHandlers.OnTerminateEventHandler("+n+")-end (ctr="+bucketAtStart+")\n");
                      } catch (InterruptedException e) {
                          Logging.info("TestLotsOfTerminateHandlers.OnTerminateEventHandler("+n+")-end (ctr="+bucketAtStart+") + exception = "+ e.getMessage()+"\n");
                      }
                  };
              };

              for (int i=0;i<10;i++) {
                  tApp.getEventBus().appendTerminateEventHandler(new OnTerminateEventHandler( makeTerminateHandler.apply(i) ));
              }
              
              tApp.getEventBus().appendTerminateEventHandler(new OnTerminateEventHandler( ()->  {
                  Logging.info("TestLotsOfTerminateHandlers.OnTerminateEventHandler() - DONE!!");
              }));
            
        }).start();        
        
    }

}
