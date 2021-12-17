package es.lavanda.filebot.bot.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

import es.lavanda.lib.common.config.CommonConfigurator;
import org.springframework.cloud.aws.autoconfigure.context.ContextInstanceDataAutoConfiguration;


@Configuration
@EnableMongoAuditing
@Import(CommonConfigurator.class)
@EnableAutoConfiguration(exclude = { ContextInstanceDataAutoConfiguration.class })
public class AppBeans {

}
