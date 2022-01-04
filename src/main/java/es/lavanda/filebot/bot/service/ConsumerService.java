package es.lavanda.filebot.bot.service;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import es.lavanda.filebot.bot.exception.FilebotBotException;
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
        throw new FilebotBotException("error");
        // log.info("Work message finished");
    }
    /**
     * FilebotExecutionIDTO(
     * id=61cef9c3b442581c35b08487,
     * files=[El incidente BD1080.atomixhq.net.mkv],
     * path=src/main/resources/filebot/El incidente [BluRay 1080p][DTS 5.1
     * Castellano DTS-HD 5.1-Ingles+Subs][ES-EN]
     * )
     * 
     * 
     * 
     */
}
