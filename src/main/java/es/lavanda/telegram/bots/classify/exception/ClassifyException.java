package es.lavanda.telegram.bots.classify.exception;

public class ClassifyException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ClassifyException(String message, Exception e) {
        super(message, e);
    }

    public ClassifyException(String message) {
        super(message);
    }

    public ClassifyException(Exception e) {
        super(e);
    }

}
