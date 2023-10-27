package es.lavanda.telegram.bots.common.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import es.lavanda.lib.common.model.FilebotExecutionODTO;
import es.lavanda.lib.common.model.QbittorrentModel;
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

    public void sendToFilebotExecutorResolution(QbittorrentModel qbittorrentModel) {
        try {
            log.info("Sending message to queue {} with the data {}", "filebot-new-execution-resolution",
                    qbittorrentModel);
            rabbitTemplate.convertAndSend("filebot-new-execution-resolution", qbittorrentModel);
            log.info("Sended message to queue");
        } catch (Exception e) {
            log.error("Failed send message to queue {}", "filebot-new-execution-resolution", e);
            throw new ClassifyException("Failed send message to queue", e);
        }
    }
}
