package es.lavanda.telegram.bots.common.service;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import es.lavanda.lib.common.model.FilebotExecutionIDTO;
import es.lavanda.lib.common.model.FilebotExecutionTestIDTO;
import es.lavanda.lib.common.model.TelegramFilebotExecutionODTO;
import es.lavanda.telegram.bots.common.model.TelegramMessage;
import es.lavanda.telegram.bots.filebot.service.FilebotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConsumerService {

    private final FilebotService filebotService;

    private static final String FILEBOT_TELEGRAM = "filebot-telegram";

    private static final String FILEBOT_TELEGRAM_TEST = "filebot-telegram-test";

    private static final String TELEGRAM_QUERY_TMDB_RESOLUTION = "telegram-query-tmdb-resolution";

    private static final String TELEGRAM_MESSAGES = "telegram-messages";

    // ***** FILEBOT SERVICE */

    @RabbitListener(queues = FILEBOT_TELEGRAM)
    public void consumeMessageFeedFilms(FilebotExecutionIDTO filebotExecutionIDTO) {
        log.info("Reading message of the queue {}: {}", FILEBOT_TELEGRAM, filebotExecutionIDTO);
        filebotService.run(filebotExecutionIDTO);
        log.info("Finish message of the queue {}", FILEBOT_TELEGRAM);
    }

    @RabbitListener(queues = FILEBOT_TELEGRAM_TEST)
    public void consumeMessageFeedFilms(FilebotExecutionTestIDTO filebotExecutionTestIDTO) {
        log.info("Reading message of the queue {}: {}", FILEBOT_TELEGRAM_TEST, filebotExecutionTestIDTO);
        filebotService.runTest(filebotExecutionTestIDTO);
        log.info("Finish message of the queue {}", FILEBOT_TELEGRAM_TEST);
    }

    @RabbitListener(queues = TELEGRAM_QUERY_TMDB_RESOLUTION)
    public void consumeMessageFeedFilms(TelegramFilebotExecutionODTO telegramFilebotExecutionODTO)
            throws InterruptedException {
        log.info("Reading message of the queue {}: {}", TELEGRAM_QUERY_TMDB_RESOLUTION, telegramFilebotExecutionODTO);
        // log.info("Sleeping on this queue");
        // Thread.sleep(2000);
        // log.info("Finish sleeping on this queue");
        filebotService.recieveTMDBData(telegramFilebotExecutionODTO);
        log.info("Finish message of the queue {}", TELEGRAM_QUERY_TMDB_RESOLUTION);
    }

    /* ***** CLASSIFY SERVICE ***** */
    /* ***** FIN DE CLASSIFY SERVICE **** */

    @RabbitListener(queues = TELEGRAM_MESSAGES)
    public void consumeMessageForSendMessages(TelegramMessage telegramMessage) {
        log.info("Reading message of the queue {TELEGRAM_MESSAGES}: {}", TELEGRAM_MESSAGES, telegramMessage);
        switch (telegramMessage.getHandler()) {
            // case CLASSIFY:
            // classifyService.sendMessage(telegramMessage);
            // break;
            case FILEBOT:
                filebotService.sendMessage(telegramMessage);
                break;
            default:
                break;
        }
        log.info("Finish message of the queue {}", TELEGRAM_MESSAGES);
    }

}
