package es.lavanda.telegram.bots.filebot.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@Data
public class FilebotUserConfig {

    @Value("${telegram.filebot.authorized.usernames}")
    private List<String> usernames;

}
