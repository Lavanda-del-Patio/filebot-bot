package es.lavanda.telegram.bots.classify.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@Data
public class ClassifyUserConfig {

    @Value("${telegram.classify.authorized.usernames}")
    private List<String> usernames;
    
}
