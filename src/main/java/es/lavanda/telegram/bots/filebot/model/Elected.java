package es.lavanda.telegram.bots.filebot.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import es.lavanda.lib.common.model.FilebotExecutionODTO;
import lombok.Data;

@Data
@Document("filebot_elected")
public class Elected {

    @Id
    private String id;

    private int times;

    private String name;

    private FilebotExecutionODTO filebotExecutionODTO;

    public void setTimes(int times) {
        final int maxTimes = 10;
        this.times = Math.min(times, maxTimes);
    }
}
