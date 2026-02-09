package ke.co.expd.authserver.exceptions;

public class UnsupportedGrantTypeException extends RuntimeException {
    public UnsupportedGrantTypeException(String message) {
        super(message);
    }
}
