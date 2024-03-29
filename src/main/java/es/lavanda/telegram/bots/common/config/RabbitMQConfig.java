package es.lavanda.telegram.bots.common.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import es.lavanda.lib.common.model.FilebotExecutionIDTO;
import es.lavanda.lib.common.model.FilebotExecutionTestIDTO;
import es.lavanda.lib.common.model.TelegramFilebotExecutionODTO;
import es.lavanda.telegram.bots.common.model.TelegramMessage;

@Configuration
public class RabbitMQConfig {
        public static final String EXCHANGE_MESSAGES = "lavandadelpatio-exchange";

        public static final String QUEUE_MESSAGES = "filebot-telegram";
        public static final String QUEUE_MESSAGES_DLQ = "filebot-telegram-dlq";

        public static final String QUEUE_MESSAGES_TEST = "filebot-telegram-test";
        public static final String QUEUE_MESSAGES_TEST_DLQ = "filebot-telegram-test-dlq";

        public static final String QUEUE_TELEGRAM_QUERY_TMDB_RESOLUTION = "telegram-query-tmdb-resolution";
        public static final String QUEUE_TELEGRAM_QUERY_TMDB_RESOLUTION_DLQ = "telegram-query-tmdb-resolution-dlq";

        public static final String QUEUE_TELEGRAM_MESSAGES = "telegram-messages";
        public static final String QUEUE_TELEGRAM_MESSAGES_DLQ = "telegram-messages-dlq";

        @Bean
        public DefaultClassMapper classMapper() {
                DefaultClassMapper classMapper = new DefaultClassMapper();
                Map<String, Class<?>> idClassMapping = new HashMap<>();
                idClassMapping.put("es.lavanda.telegram.bots.common.model.TelegramMessage",
                                TelegramMessage.class);
                idClassMapping.put("es.lavanda.lib.common.model.TelegramFilebotExecutionODTO",
                                TelegramFilebotExecutionODTO.class);
                idClassMapping.put("es.lavanda.lib.common.model.FilebotExecutionIDTO",
                                FilebotExecutionIDTO.class);
                idClassMapping.put("es.lavanda.lib.common.model.FilebotExecutionTestIDTO",
                                FilebotExecutionTestIDTO.class);
                classMapper.setIdClassMapping(idClassMapping);
                return classMapper;
        }

        @Bean
        public Jackson2JsonMessageConverter jsonMessageConverter() {
                Jackson2JsonMessageConverter jsonConverter = new Jackson2JsonMessageConverter();
                jsonConverter.setClassMapper(classMapper());
                return jsonConverter;
        }

        @Bean("rabbitTemplateOverrided")
        @Primary
        public RabbitTemplate rabbitTemplate(final ConnectionFactory connectionFactory) {
                final var rabbitTemplate = new RabbitTemplate(connectionFactory);
                rabbitTemplate.setMessageConverter(jsonMessageConverter());
                return rabbitTemplate;
        }

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
        Binding bindingMessagesTelegramTest() {
                return BindingBuilder.bind(messagesQueue()).to(messagesExchange()).with(QUEUE_MESSAGES_TEST);
        }

        @Bean
        Queue messagesQueueTelegramTest() {
                return QueueBuilder.durable(QUEUE_MESSAGES_TEST).withArgument("x-dead-letter-exchange", "")
                                .withArgument("x-dead-letter-routing-key", QUEUE_MESSAGES_TEST_DLQ).build();
        }

        @Bean
        Queue deadLetterQueueTelegramTest() {
                return QueueBuilder.durable(QUEUE_MESSAGES_TEST_DLQ).build();
        }

}
