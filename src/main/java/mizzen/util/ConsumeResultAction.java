package mizzen.util;

@FunctionalInterface
public interface ConsumeResultAction<T> {
    void accept(T result) throws Exception;
}