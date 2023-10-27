package es.lavanda.telegram.bots.filebot.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FilebotConfig {

    @Value("${telegram.filebot.token}")
    private String filebotToken;

    @Value("${telegram.filebot.username}")
    private String filebotUsername;

    @Autowired
    private FilebotUserConfig userConfig;

    public String getToken() {
        return filebotToken;
    }

    public String getUsername() {
        return filebotUsername;
    }

    public boolean isAuthorizedToUseBot(String id) {
        return userConfig.getUsernames().contains(id);
    }

}
