package es.lavanda.telegram.bots.classify.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import es.lavanda.lib.common.model.QbittorrentModel;
import lombok.ToString;

@Document("telegram_qbitorrent")
@ToString
public class Qbittorrent extends QbittorrentModel {

    @Id
    private String id;

    public String getId() {
        return id;
    }

}
