package es.lavanda.filebot.bot.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;

import es.lavanda.filebot.bot.exception.FilebotBotException;
import es.lavanda.filebot.bot.model.TelegramFilebotExecution;
import es.lavanda.filebot.bot.model.TelegramMessage;
import es.lavanda.lib.common.model.FilebotExecutionODTO;
import es.lavanda.lib.common.model.TelegramFilebotExecutionIDTO;
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
            throw new FilebotBotException("Failed send message to queue", e);
        }
    }

    public void sendTelegramExecutionForTMDB(TelegramFilebotExecutionIDTO telegramFilebotExecutionIDTO) {
        try {
            log.info("Sending message to queue {}", "telegram-query-tmdb");
            rabbitTemplate.convertAndSend("telegram-query-tmdb", telegramFilebotExecutionIDTO);
            log.info("Sended message to queue");
        } catch (Exception e) {
            log.error("Failed send message to queue {}", "telegram-query-tmdb", e);
            throw new FilebotBotException("Failed send message to queue", e);
        }
    }

    public void sendTelegramMessages(TelegramMessage telegramMessage) {
        try {
            log.info("Sending message to queue {} with the data {}", "filebot-telegram-messages", telegramMessage);
            rabbitTemplate.convertAndSend("filebot-telegram-messages", telegramMessage);
            log.info("Sended message to queue");
        } catch (Exception e) {
            log.error("Failed send message to queue {}", "filebot-telegram-messages", e);
            throw new FilebotBotException("Failed send message to queue", e);
        }
    }
}
