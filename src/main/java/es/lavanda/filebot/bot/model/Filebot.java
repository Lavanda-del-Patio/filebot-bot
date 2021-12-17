package es.lavanda.filebot.bot.model;

import java.util.Date;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.Data;
import lombok.ToString;

@Data
@Document("filebot")
@ToString
public class Filebot {
    @Id
    private String id;

    @Field("new_location")
    private String newLocation;

    @Field("new_name")
    private String newName;

    @Field("original_name")
    private String originalName;

    private boolean unsorted;

    @CreatedBy
    @Field("created_by")
    private String createdBy;

    @CreatedDate
    @Field("created_at")
    private Date createdAt;

    @LastModifiedBy
    @Field("last_modified_by")
    private String lastModifiedBy;

    @LastModifiedDate
    @Field("last_modified_at")
    private Date lastModifiedAt;
}
