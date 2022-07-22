package es.lavanda.filebot.bot.model;

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

    private String messageId;

    private TelegramStatus status;

    public enum TelegramStatus {
        IDLE, WAITING_USER_RESPONSE, STOPPED;

    }
}
