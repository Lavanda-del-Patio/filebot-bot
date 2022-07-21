package es.lavanda.filebot.bot.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FilebotBotConfig {

    @Value("${telegram.filebotbot.token}")
    private String filebotbotToken;

    @Value("${telegram.filebotbot.username}")
    private String filebotbotUsername;

    @Autowired
    private UserConfig userConfig;

    public String getToken() {
        return filebotbotToken;
    }

    public String getUsername() {
        return filebotbotUsername;
    }

    public boolean isAuthorizedToUseBot(String id) {
        return userConfig.getUsernames().contains(id);
    }

}
