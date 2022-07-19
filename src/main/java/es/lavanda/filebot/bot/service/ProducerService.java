package es.lavanda.filebot.bot.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import es.lavanda.filebot.bot.exception.FilebotBotException;
import es.lavanda.filebot.bot.model.FilebotNameSelection;
import es.lavanda.lib.common.model.FilebotExecutionODTO;
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

    public void sendTelegramExecution(FilebotNameSelection filebot) {
        try {
            log.info("Sending message to queue {}", "telegram-execution");
            rabbitTemplate.convertAndSend("telegram-execution", filebot);
            log.info("Sended message to queue");
        } catch (Exception e) {
            log.error("Failed send message to queue {}", "telegram-execution", e);
            throw new FilebotBotException("Failed send message to queue", e);
        }
    }
}
