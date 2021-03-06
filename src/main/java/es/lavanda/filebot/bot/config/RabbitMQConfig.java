package es.lavanda.filebot.bot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_MESSAGES = "filebot-telegram";
    public static final String QUEUE_MESSAGES_DLQ = "filebot-telegram-dlq";

    public static final String QUEUE_TELEGRAM_QUERY_TMDB_RESOLUTION = "telegram-query-tmdb-resolution";
    public static final String QUEUE_TELEGRAM_QUERY_TMDB_RESOLUTION_DLQ = "telegram-query-tmdb-resolution-dlq";

    public static final String EXCHANGE_MESSAGES = "lavandadelpatio-exchange";

    @Bean
    DirectExchange messagesExchange() {
        return new DirectExchange(EXCHANGE_MESSAGES);
    }

    @Bean
    Binding bindingMessages() {
        return BindingBuilder.bind(messagesQueue()).to(messagesExchange()).with(QUEUE_MESSAGES);
    }

    @Bean
    Queue messagesQueue() {
        return QueueBuilder.durable(QUEUE_MESSAGES).withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", QUEUE_MESSAGES_DLQ).build();
    }

    @Bean
    Queue deadLetterQueue() {
        return QueueBuilder.durable(QUEUE_MESSAGES_DLQ).build();
    }

    @Bean
    Binding bindingMessagesTelegramTMDBResolution() {
        return BindingBuilder.bind(messagesQueue()).to(messagesExchange()).with(QUEUE_TELEGRAM_QUERY_TMDB_RESOLUTION);
    }

    @Bean
    Queue messagesQueueTelegramTMDBResolution() {
        return QueueBuilder.durable(QUEUE_TELEGRAM_QUERY_TMDB_RESOLUTION).withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", QUEUE_TELEGRAM_QUERY_TMDB_RESOLUTION_DLQ).build();
    }

    @Bean
    Queue deadLetterQueueTelegramTMDBResolution() {
        return QueueBuilder.durable(QUEUE_TELEGRAM_QUERY_TMDB_RESOLUTION_DLQ).build();
    }
}
