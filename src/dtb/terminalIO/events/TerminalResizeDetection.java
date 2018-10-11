package dtb.terminalIO.events;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import dtb.terminalIO.TerminalScreen;

public class TerminalResizeDetection {
    
    public static void initiate(TerminalScreen screen, EventBus eventBus) {
	
	TerminalResizePoller.startPollingForTerminalResize(screen, eventBus);
	
	registerTerminalResizeListener(new TerminalResizeEventGenerator(screen, eventBus), 
	                               ()->TerminalResizePoller.startPollingForTerminalResize(screen, eventBus));
	
    }
    
    static  void registerTerminalResizeListener(final Runnable onResize, Runnable fallback )  {
        try {
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
