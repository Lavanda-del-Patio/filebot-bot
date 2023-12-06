package es.lavanda.telegram.bots.common.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import es.lavanda.lib.common.model.FilebotExecutionODTO;
import es.lavanda.lib.common.model.FilebotExecutionTestODTO;
import es.lavanda.lib.common.model.TelegramFilebotExecutionIDTO;
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

    private static final String FILEBOT_TELEGRAM_TEST_RESOLUTION = "filebot-telegram-test-resolution";

    private static final String FILEBOT_TELEGRAM_RESOLUTION = "filebot-telegram-resolution";

    private static final String TELEGRAM_QUERY_TMDB = "telegram-query-tmdb";

    private static final String TELEGRAM_MESSAGES = "telegram-messages";

    public void sendFilebotExecution(FilebotExecutionODTO filebot) {
        try {
            log.info("Sending message to queue {}", FILEBOT_TELEGRAM_RESOLUTION);
            rabbitTemplate.convertAndSend(FILEBOT_TELEGRAM_RESOLUTION, filebot);
            log.info("Sended message to queue");
        } catch (Exception e) {
            log.error("Failed send message to queue {}", FILEBOT_TELEGRAM_RESOLUTION, e);
            throw new FilebotException("Failed send message to queue", e);
        }
    }

    public void sendFilebotExecutionTest(FilebotExecutionTestODTO filebotExecutionTestODTO) {
        try {
            log.info("Sending message to queue {}", FILEBOT_TELEGRAM_TEST_RESOLUTION);
            rabbitTemplate.convertAndSend(FILEBOT_TELEGRAM_TEST_RESOLUTION, filebotExecutionTestODTO);
            log.info("Sended message to queue");
        } catch (Exception e) {
            log.error("Failed send message to queue {}", FILEBOT_TELEGRAM_TEST_RESOLUTION, e);
            throw new FilebotException("Failed send message to queue", e);
        }
    }

    public void sendTelegramExecutionForTMDB(TelegramFilebotExecutionIDTO telegramFilebotExecutionIDTO) {
        try {
            log.info("Sending message to queue {}", TELEGRAM_QUERY_TMDB);
            rabbitTemplate.convertAndSend(TELEGRAM_QUERY_TMDB, telegramFilebotExecutionIDTO);
            log.info("Sended message to queue");
        } catch (Exception e) {
            log.error("Failed send message to queue {}", TELEGRAM_QUERY_TMDB, e);
            throw new FilebotException("Failed send message to queue", e);
        }
    }

    public void sendTelegramMessage(TelegramMessage telegramMessage) {
        try {
            log.info("Sending message to queue {} with the data {}", TELEGRAM_MESSAGES, telegramMessage);
            rabbitTemplate.convertAndSend(TELEGRAM_MESSAGES, telegramMessage);
            log.info("Sended message to queue");
        } catch (Exception e) {
            log.error("Failed send message to queue {}", TELEGRAM_MESSAGES, e);
            throw new RuntimeException("Failed send message to queue", e);
        }
    }

}
