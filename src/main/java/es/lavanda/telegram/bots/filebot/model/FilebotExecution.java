package es.lavanda.telegram.bots.filebot.model;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import es.lavanda.lib.common.model.filebot.FilebotAction;
import es.lavanda.lib.common.model.filebot.FilebotCategory;
import es.lavanda.lib.common.model.tmdb.search.TMDBResultDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString
@EqualsAndHashCode(exclude = { "id", "createdAt", "lastModifiedAt" })
@Document("filebot_conversation_execution")
public class FilebotExecution implements Serializable {
    @Id
    private String id;

    private String path;

    private List<String> files;

    private List<String> possibilities;

    private FilebotExecutionStatus status = FilebotExecutionStatus.UNPROCESSED;

    private FilebotCategory category;

    private FilebotAction action;

    private boolean forceStrict;

    private String query;

    private String selectedPossibilities;

    private boolean inProgress;

    private Map<String, TMDBResultDTO> possibleChoicesTMDB = new HashMap<>();

    @CreatedDate
    @Field("created_at")
    private Date createdAt;

    @LastModifiedDate
    @Field("last_modified_at")
    private Date lastModifiedAt;

    public enum FilebotExecutionStatus {
        UNPROCESSED, CATEGORY, FORCE_STRICT, ACTION, TMDB, CHOICE, PROCESSED;
    }

    public FilebotExecutionStatus getPreviousStatus() {
        return FilebotExecutionStatus.values()[status.ordinal() - 1];
    }

}
