package es.lavanda.telegram.bots.filebot.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import lombok.ToString;

@Data
@Document("filebot_conversation")
@ToString
public class FilebotConversation {

    @Id
    private String id;

    private String name;

    private String chatId;

    private String previousMessageId;

    private List<String> inlineKeyboardMessageIds = new ArrayList<>();

    private List<String> photosMessageIds = new ArrayList<>();

    private List<String> callbackData = new ArrayList<>();

}
