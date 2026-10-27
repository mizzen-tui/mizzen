package mizzen.events;

/**
 * Something registered - a listener, a handler: unregister() takes it out again. Handed back by
 * whatever accepted it (e.g. an input's onChange), it knows what to remove; safe to call more than
 * once - after the first, nothing is left to remove.
 * <pre>
 *     Registration watching = nameInput.onChange((te) -&gt; status.setTextAndResize("edited"));
 *     ...
 *     watching.unregister();          // only this listener stops; any others keep running
 * </pre>
 * Most listeners never need it: they live as long as their component.
 */
@FunctionalInterface
public interface Registration {

    /** nothing was registered, so nothing to take out */
    Registration NONE = () -> {
    };

    void unregister();
}
