package dtb.terminalIO.app;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

/**
 * TUIApp does no work itself: every call goes to the implementation it holds (TerminalApp, which
 * owns the terminal). This checks each call of TUIAppInterface arrives there - the same method, the
 * same arguments - and what comes back: a chaining call gives back the TUIApp (so the caller keeps
 * chaining on the TUIApp, never on the implementation), any other call gives back what the
 * implementation answered.
 */
public class TUIAppDelegationTest extends TestCase {

    /** what the recording implementation answers for a method returning this type */
    private static Object answerFor(Class<?> type, Object implementation) {
        if (type == TUIAppInterface.class) {
            return implementation;                  // like TerminalApp: it returns itself
        }
        if (type == char.class) {
            return 'x';
        }
        if (type == boolean.class) {
            return true;
        }
        if (type == int.class) {
            return 42;
        }
        if (type.isInterface()) {
            return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, (p, m, a) -> null);
        }
        return null;                                // a class (Canvas, Dimension ...): nothing to fake
    }

    /** an argument for a parameter of this type */
    private static Object argumentFor(Class<?> type, int index) {
        if (type == char.class) {
            return 'c';
        }
        if (type == boolean.class) {
            return true;
        }
        if (type == int.class) {
            return 7 + index;
        }
        if (type == String.class) {
            return "arg" + index;
        }
        if (type == String[].class) {
            return new String[] { "arg" + index };
        }
        if (type.isInterface()) {
            return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, (p, m, a) -> null);
        }
        return null;
    }

    public void testEveryCallReachesTheImplementation() throws Exception {
        List<Object[]> calls = new ArrayList<>();     // { method, args, answer }
        TUIAppInterface[] implementation = new TUIAppInterface[1];
        implementation[0] = (TUIAppInterface) Proxy.newProxyInstance(TUIAppInterface.class.getClassLoader(),
                new Class<?>[] { TUIAppInterface.class },
                (proxy, method, args) -> {
                    Object answer = answerFor(method.getReturnType(), implementation[0]);
                    calls.add(new Object[] { method, args == null ? new Object[0] : args, answer });
                    return answer;
                });
        TUIApp tuiApp = new TUIApp(implementation[0]);

        for (Method method : TUIAppInterface.class.getMethods()) {
            if (method.isDefault()) {
                continue;                           // ComponentFinder's own helpers: not forwarded, built on the others
            }
            Class<?>[] types = method.getParameterTypes();
            Object[] args = new Object[types.length];
            for (int i = 0; i < types.length; i++) {
                args[i] = argumentFor(types[i], i);
            }
            calls.clear();
            Method forwarder = TUIApp.class.getMethod(method.getName(), types);
            Object result = forwarder.invoke(tuiApp, args);

            String what = method.toGenericString();
            assertEquals(what + ": calls reaching the implementation", 1, calls.size());
            assertEquals(what + ": the method reaching it", method, calls.get(0)[0]);
            assertTrue(what + ": the arguments reaching it", Arrays.equals(args, (Object[]) calls.get(0)[1]));
            if (method.getReturnType() == TUIAppInterface.class) {
                assertEquals(what + ": a chaining call returns TUIApp", TUIApp.class, forwarder.getReturnType());
                assertSame(what + ": a chaining call returns the TUIApp itself", tuiApp, result);
            } else if (method.getReturnType().isPrimitive()) {
                assertEquals(what + ": the implementation's answer", calls.get(0)[2], result);
            } else {
                assertSame(what + ": the implementation's answer", calls.get(0)[2], result);
            }
        }
    }

    /** the usual TUIApp works through a TerminalApp that knows it (callbacks and names get the TUIApp) */
    public void testTheUsualTUIAppHoldsATerminalApp() {
        TUIApp tuiApp = new TUIApp();
        assertNotNull(tuiApp.terminal());
        tuiApp.setUpdateCanvas((app) -> true);
        assertSame(tuiApp.getUpdateCanvas(), tuiApp.terminal().getUpdateCanvas());
    }
}
