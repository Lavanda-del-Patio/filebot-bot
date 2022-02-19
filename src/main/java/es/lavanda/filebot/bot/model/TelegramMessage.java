package es.lavanda.filebot.bot.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import lombok.ToString;

@Data
@Document("telegram_messages")
@ToString
public class TelegramMessage {

    @Id
    private String id;

    private String chatId;

    private String messageId;

    private String text;
}
