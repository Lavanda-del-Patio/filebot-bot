package es.lavanda.telegram.bots.common.model;

import java.io.Serializable;

import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

import lombok.Data;

@Data
public class TelegramMessage implements Serializable {

    private String chatId;

    private int messageId;

    private String text;

    private boolean saveOnDatabase;

    private String callbackId;

    private MessageType type;

    private String caption;

    private InputFile photo;

    private ReplyKeyboard replyMarkup;

    private MessageHandler handler;

    public enum MessageType {
        TEXT, PHOTO, EDIT_MESSAGE, DELETE_MESSAGE
    }

}
