package es.lavanda.filebot.bot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BotConfig {

    @Value("${telegram.filebot.token}")
    private String FILEBOT_TOKEN;

    @Value("${telegram.filebot.user}")
    private String FILEBOT_USERNAME;

    @Value("${telegram.filebot.admin}")
    private String FILEBOT_ADMIN;


    public String getFilebotToken() {
        return FILEBOT_TOKEN;
    }

    public String getFilebotUser() {
        return FILEBOT_USERNAME;
    }

    public String getFilebotAdmin() {
        return FILEBOT_ADMIN;
    }

}
