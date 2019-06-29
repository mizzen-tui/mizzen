package dtb.terminalIO.util;

@FunctionalInterface
public interface ConsumeResult<T> {
    void accept(T result) throws Exception;
}