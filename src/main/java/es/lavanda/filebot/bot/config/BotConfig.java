package es.lavanda.filebot.bot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BotConfig {

    @Value("${telegram.filebot.token}")
    public String FILEBOT_TOKEN;

    @Value("${telegram.filebot.user}")
    public String FILEBOT_USER;

}
