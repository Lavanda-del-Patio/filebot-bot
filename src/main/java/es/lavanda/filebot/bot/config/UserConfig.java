package es.lavanda.filebot.bot.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.Getter;

@Component
@Data
public class UserConfig {

    @Value("${telegram.filebotbot.authorized.usernames}")
    private List<String> usernames;

}
