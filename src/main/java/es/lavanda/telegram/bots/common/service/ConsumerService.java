package es.lavanda.telegram.bots.common.service;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;

import es.lavanda.lib.common.model.FilebotExecutionIDTO;
import es.lavanda.lib.common.model.FilebotTelegramDeleteFolder;
import es.lavanda.lib.common.model.QbittorrentModel;
import es.lavanda.lib.common.model.TelegramFilebotExecutionODTO;
import es.lavanda.telegram.bots.classify.handler.ClassifyHandler;
import es.lavanda.telegram.bots.classify.service.ClassifyService;
import es.lavanda.telegram.bots.common.model.TelegramMessage;
import es.lavanda.telegram.bots.filebot.model.FilebotExecution;
import es.lavanda.telegram.bots.filebot.service.FilebotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConsumerService {

    private final ClassifyService classifyService;

    private final FilebotService filebotService;

    // ***** FILEBOT SERVICE */

    @RabbitListener(queues = "filebot-telegram")
    public void consumeMessageFeedFilms(FilebotExecutionIDTO filebotExecutionIDTO) {
        log.info("Reading message of the queue filebot-telegram: {}", filebotExecutionIDTO);
        filebotService.run(filebotExecutionIDTO);
        log.info("Finish message of the queue filebot-telegram");
    }

    @RabbitListener(queues = "telegram-query-tmdb-resolution")
    public void consumeMessageFeedFilms(TelegramFilebotExecutionODTO telegramFilebotExecutionODTO) {
        log.info("Reading message of the queue telegram-query-tmdb-resolution: {}", telegramFilebotExecutionODTO);
        filebotService.recieveTMDBData(telegramFilebotExecutionODTO);
        log.info("Finish message of the queue telegram-query-tmdb-resolution");
    }

    // ***** CLASSIFY SERVICE */

    @RabbitListener(queues = "filebot-new-execution")
    public void consumeMessageForDeleteFolder(QbittorrentModel qbittorrentModel) {
        log.info("Reading message of the queue filebot-new-execution {}", qbittorrentModel);
        classifyService.newExecution(qbittorrentModel);
        log.info("Finish message of the queue filebot-new-execution");
    }

    @RabbitListener(queues = "telegram-messages")
    public void consumeMessageForSendMessages(TelegramMessage telegramMessage) {
        log.info("Reading message of the queue filebot-telegram-messages: {}", telegramMessage);
        telegramMessage.getHandler().handle(telegramMessage);
        log.info("Finish message of the queue filebot-telegram-messages");
    }

}
