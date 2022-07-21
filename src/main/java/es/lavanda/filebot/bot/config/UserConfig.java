package es.lavanda.filebot.bot.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.Getter;

@Component
@ConfigurationProperties(prefix = "telegram.filebotbot.authorized")
@Data
public class UserConfig {

    private List<String> usernames;

}
