package es.lavanda.telegram.bots.filebot.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Elected {
    
    private int times;

    private String name;

    private String releaseDate;

    private int tmdbId;
}
