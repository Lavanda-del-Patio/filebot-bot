package es.lavanda.filebot.bot.service;

import java.util.List;

public interface FilebotPreselectionService {

    List<String> getPreselection();

    void setPreselection(String preselection);
    
}
