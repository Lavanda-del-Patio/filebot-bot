package es.lavanda.telegram.bots.filebot.exception;

public class FilebotException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FilebotException(String message, Exception e) {
        super(message, e);
    }

    public FilebotException(String message) {
        super(message);
    }

    public FilebotException(Exception e) {
        super(e);
    }

}
