package es.lavanda.telegram.bots.classify.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import lombok.ToString;

@Data
@Document("classify_conversation")
@ToString
public class ClassifyConversation {

    @Id
    private String id;

    private String name;

    private String chatId;

    private String inlineKeyboardMessageId;

    private List<String> otherMessageIds = new ArrayList<>();

    private ClassifyConversationStatus status;

    private String qbittorrentId;

    public enum ClassifyConversationStatus {
        IDLE, WAITING_USER_RESPONSE_CATEGORY, WAITING_USER_RESPONSE_ACTION, STOPPED;

    }
}
