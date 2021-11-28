package dtb.terminalIO.util;

@FunctionalInterface
public interface ConsumeResultAction<T> {
    void accept(T result) throws Exception;
}