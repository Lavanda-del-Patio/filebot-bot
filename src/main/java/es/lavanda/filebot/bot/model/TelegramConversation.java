package es.lavanda.filebot.bot.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import lombok.ToString;

@Data
@Document("telegram_conversation")
@ToString
public class TelegramConversation {

    @Id
    private String id;

    private String name;

    private String chatId;

    private String inlineKeyboardMessageId;

    private List<String> otherMessageIds = new ArrayList<>();

    private TelegramStatus status;

    public enum TelegramStatus {
        IDLE, WAITING_USER_RESPONSE, WAITING_TMDB_RESPONSE, STOPPED;

    }
}
