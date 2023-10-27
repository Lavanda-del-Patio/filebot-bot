package es.lavanda.telegram.bots.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;

@Configuration
public class RabbitMQConfig {
        public static final String EXCHANGE_MESSAGES = "lavandadelpatio-exchange";

        public static final String QUEUE_MESSAGES = "filebot-telegram";
        public static final String QUEUE_MESSAGES_DLQ = "filebot-telegram-dlq";

        public static final String QUEUE_TELEGRAM_QUERY_TMDB_RESOLUTION = "telegram-query-tmdb-resolution";
        public static final String QUEUE_TELEGRAM_QUERY_TMDB_RESOLUTION_DLQ = "telegram-query-tmdb-resolution-dlq";

        public static final String QUEUE_TELEGRAM_MESSAGES = "telegram-messages";
        public static final String QUEUE_TELEGRAM_MESSAGES_DLQ = "telegram-messages-dlq";

        public static final String FILEBOT_NEW_EXECUTION = "filebot-new-execution";
        public static final String FILEBOT_NEW_EXECUTION_DLQ = "filebot-new-execution-dlq";

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
                return BindingBuilder.bind(messagesQueue()).to(messagesExchange())
                                .with(QUEUE_TELEGRAM_QUERY_TMDB_RESOLUTION);
        }

        @Bean
        Queue messagesQueueTelegramTMDBResolution() {
                return QueueBuilder.durable(QUEUE_TELEGRAM_QUERY_TMDB_RESOLUTION)
                                .withArgument("x-dead-letter-exchange", "")
                                .withArgument("x-dead-letter-routing-key", QUEUE_TELEGRAM_QUERY_TMDB_RESOLUTION_DLQ)
                                .build();
        }

        @Bean
        Queue deadLetterQueueTelegramTMDBResolution() {
                return QueueBuilder.durable(QUEUE_TELEGRAM_QUERY_TMDB_RESOLUTION_DLQ).build();
        }

        @Bean
        Binding bindingMessagesTelegram() {
                return BindingBuilder.bind(messagesQueue()).to(messagesExchange()).with(QUEUE_TELEGRAM_MESSAGES);
        }

        @Bean
        Queue messagesQueueTelegram() {
                return QueueBuilder.durable(QUEUE_TELEGRAM_MESSAGES).withArgument("x-dead-letter-exchange", "")
                                .withArgument("x-dead-letter-routing-key", QUEUE_TELEGRAM_MESSAGES_DLQ).build();
        }

        @Bean
        Queue deadLetterQueueTelegram() {
                return QueueBuilder.durable(QUEUE_TELEGRAM_MESSAGES_DLQ).build();
        }

        @Bean
        Binding bindingMessagesTelegramNewExecution() {
                return BindingBuilder.bind(messagesQueue()).to(messagesExchange()).with(FILEBOT_NEW_EXECUTION);
        }

        @Bean
        Queue messagesQueueTelegramDeadLetter() {
                return QueueBuilder.durable(FILEBOT_NEW_EXECUTION).withArgument("x-dead-letter-exchange", "")
                                .withArgument("x-dead-letter-routing-key", FILEBOT_NEW_EXECUTION_DLQ).build();
        }

        @Bean
        Queue deadLetterQueueTelegramNewExecution() {
                return QueueBuilder.durable(FILEBOT_NEW_EXECUTION_DLQ).build();
        }

}
