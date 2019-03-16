package dtb.terminalIO.events.resize;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import dtb.terminalIO.core.TerminalScreen;
import dtb.terminalIO.events.EventBus;

public class TerminalResizeDetection {
    
    public static void initiate(TerminalScreen screen, EventBus eventBus) {
	
	// Use the idle events to poll for terminal resize
	
	// TerminalResizePoller.startPollingForTerminalResize(screen, eventBus);
	
	// also ..
	
	// Use the Terminal resize Signaling to detect terminal resize 
	
	registerTerminalResizeListener(new TerminalResizeEventGenerator(screen, eventBus), 
	                               ()->TerminalResizePoller.startPollingForTerminalResize(screen, eventBus));
	
	// Have not yet decided if its wise to be doing both.. Because detection would be on 2 differnt threads I think?
	// todo: need to figure that out
    }
    
    static  void registerTerminalResizeListener(final Runnable onResize, Runnable fallback )  {
        try {
            
            // GOAL: 
            
            // Use sun's Signal and SignalHandler classes to hook into  the standard Linux Terminal resize event ( WINCH ).
            // From this WINCH event we run the onResize (Runnable) which usually points to TerminalResizeEventGenerator
            // to generate a window resize event to then subsequently enqueue this event onto the global EventBus for later 
            // processing by the application.
            
            Class<?> signalClass = Class.forName("sun.misc.Signal");
            for(Method m : signalClass.getDeclaredMethods()) {
                if("handle".equals(m.getName())) {
                    Object windowResizeHandler = Proxy.newProxyInstance(TerminalResizeDetection.class.getClassLoader(), new Class[]{Class.forName("sun.misc.SignalHandler")}, new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            if("handle".equals(method.getName())) {
                                onResize.run();
                            }
                            return null;
                        }
                    });
                    m.invoke(null, signalClass.getConstructor(String.class).newInstance("WINCH"), windowResizeHandler);
                }
            }
        }
        catch(Throwable ignore) {
            // We're probably running on a non-Sun JVM and there's no way to catch signals without resorting to native
            // code integration
            
            System.err.println("Using fallback method to register terminal resize listener");
            
            fallback.run();       
        }
    }
}
