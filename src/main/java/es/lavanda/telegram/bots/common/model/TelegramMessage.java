package es.lavanda.telegram.bots.common.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethodMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

import es.lavanda.telegram.bots.common.service.chainofresponsability.Handler;
import lombok.Data;

@Data
public class TelegramMessage implements Serializable {

    private String idFilebotConversation;

    private MessageHandler handler;

    private MessageType type;

    private List<String> callbackData = new ArrayList<>();

    private boolean cleanPrevious;

    private String photoUrl;

    // MessageOfTelegram

    private String chatId;

    private String messageId;

    private String text;

    private InlineKeyboardMarkup inlineKeyboardMarkup;

    private ReplyKeyboard replyKeyboard;

    // FINISH MessageOfTelegram

    public enum MessageHandler {
        FILEBOT, CLASSIFY
    }

    public enum MessageType {
        TEXT, PHOTO, EDIT_MESSAGE, DELETE_MESSAGE
    }

}
