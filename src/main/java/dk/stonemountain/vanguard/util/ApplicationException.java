package dk.stonemountain.vanguard.util;

public class ApplicationException extends RuntimeException {
    public ApplicationException(String message, Throwable e) {
        super(message, e);
    }

    public ApplicationException(String message) {
        super(message);
    }

}
