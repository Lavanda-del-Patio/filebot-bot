package es.lavanda.filebot.bot.service;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import es.lavanda.lib.common.model.FilebotExecutionIDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConsumerService {

    private final FilebotService filebotService;

    @RabbitListener(queues = "filebot-executor")
    public void consumeMessageFeedFilms(FilebotExecutionIDTO filebotExecutionIDTO) {
        log.info("Reading message of the queue filebot-executor: {}", filebotExecutionIDTO);
        filebotService.run(filebotExecutionIDTO);
        log.info("Finish message of the queue filebot-executor");
    }
}
