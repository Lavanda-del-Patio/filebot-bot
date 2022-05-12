package es.lavanda.filebot.bot.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document("filebot_preselection")
public class FilebotPreselection {

    @Id
    private String id;

    private String preselection;

    private int count = 5;

}
