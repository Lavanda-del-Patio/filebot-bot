package es.lavanda.telegram.bots.common.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import es.lavanda.lib.common.model.FilebotExecutionODTO;
import es.lavanda.lib.common.model.TelegramFilebotExecutionIDTO;
import es.lavanda.telegram.bots.classify.exception.ClassifyException;
import es.lavanda.telegram.bots.common.model.TelegramMessage;
import es.lavanda.telegram.bots.filebot.exception.FilebotException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProducerService {

    @Autowired
    @Qualifier("rabbitTemplateOverrided")
    private final RabbitTemplate rabbitTemplate;

    public void sendFilebotExecution(FilebotExecutionODTO filebot) {
        try {
            log.info("Sending message to queue {}", "filebot-telegram-resolution");
            rabbitTemplate.convertAndSend("filebot-telegram-resolution", filebot);
            log.info("Sended message to queue");
        } catch (Exception e) {
            log.error("Failed send message to queue {}", "filebot-telegram-resolution", e);
            throw new FilebotException("Failed send message to queue", e);
        }
    }

    public void sendTelegramExecutionForTMDB(TelegramFilebotExecutionIDTO telegramFilebotExecutionIDTO) {
        try {
            log.info("Sending message to queue {}", "telegram-query-tmdb");
            rabbitTemplate.convertAndSend("telegram-query-tmdb", telegramFilebotExecutionIDTO);
            log.info("Sended message to queue");
        } catch (Exception e) {
            log.error("Failed send message to queue {}", "telegram-query-tmdb", e);
            throw new FilebotException("Failed send message to queue", e);
        }
    }

    public void sendTelegramMessage(TelegramMessage telegramMessage) {
        try {
            log.info("Sending message to queue {} with the data {}", "telegram-messages", telegramMessage);
            rabbitTemplate.convertAndSend("telegram-messages", telegramMessage);
            log.info("Sended message to queue");
        } catch (Exception e) {
            log.error("Failed send message to queue {}", "telegram-messages", e);
            throw new RuntimeException("Failed send message to queue", e);
        }
    }

}
