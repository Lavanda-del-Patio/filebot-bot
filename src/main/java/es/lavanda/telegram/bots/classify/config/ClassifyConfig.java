package es.lavanda.telegram.bots.classify.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ClassifyConfig {

    @Value("${telegram.classify.token}")
    private String classifyToken;

    @Value("${telegram.classify.username}")
    private String classifyUsername;

    @Autowired
    private ClassifyUserConfig userConfig;

    public String getToken() {
        return classifyToken;
    }

    public String getUsername() {
        return classifyUsername;
    }

    public boolean isAuthorizedToUseBot(String id) {
        return userConfig.getUsernames().contains(id);
    }

}
