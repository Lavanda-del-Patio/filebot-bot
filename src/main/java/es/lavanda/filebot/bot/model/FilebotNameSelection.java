package es.lavanda.filebot.bot.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@Document("filebot_name_selection")
@ToString
@EqualsAndHashCode(exclude = { "id", "createdAt", "lastModifiedAt" })
public class FilebotNameSelection implements Serializable {
    @Id
    private String id;

    private String path;

    private List<String> files;

    private List<String> possibilities;

    private FilebotNameStatus status;

    private String label;

    private boolean forceStrict;

    private String query;

    private String selectedPossibilitie;

    @CreatedDate
    @Field("created_at")
    private Date createdAt;

    @LastModifiedDate
    @Field("last_modified_at")
    private Date lastModifiedAt;

}
