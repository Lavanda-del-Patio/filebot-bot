package es.lavanda.filebot.bot.exception;

public class FilebotBotException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FilebotBotException(String message, Exception e) {
        super(message, e);
    }

    public FilebotBotException(String message) {
        super(message);
    }

    public FilebotBotException(Exception e) {
        super(e);
    }

}
