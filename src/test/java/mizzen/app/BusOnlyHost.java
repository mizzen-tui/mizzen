package mizzen.app;

import java.lang.reflect.Proxy;

import mizzen.events.EventBus;

/** for tests outside mizzen.app: a host TUIApp that is only a bus - what an embedded app needs of its host */
public final class BusOnlyHost {

    private BusOnlyHost() {
    }

    public static TUIApp of(EventBus bus) {
        return new TUIApp((TUIAppInterface) Proxy.newProxyInstance(TUIAppInterface.class.getClassLoader(),
                new Class<?>[] { TUIAppInterface.class },
                (proxy, method, args) -> method.getName().equals("getEventBus") ? bus : null));
    }
}
