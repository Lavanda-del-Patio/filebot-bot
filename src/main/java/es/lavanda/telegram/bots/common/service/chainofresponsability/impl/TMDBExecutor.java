package es.lavanda.telegram.bots.common.service.chainofresponsability.impl;

import org.springframework.stereotype.Service;

import es.lavanda.lib.common.model.TelegramFilebotExecutionIDTO;
import es.lavanda.lib.common.model.TelegramFilebotExecutionIDTO.Type;
import es.lavanda.lib.common.model.filebot.FilebotCategory;
import es.lavanda.telegram.bots.common.model.TelegramMessage;
import es.lavanda.telegram.bots.common.model.TelegramMessage.MessageHandler;
import es.lavanda.telegram.bots.common.model.TelegramMessage.MessageType;
import es.lavanda.telegram.bots.common.service.ProducerService;
import es.lavanda.telegram.bots.common.service.chainofresponsability.Handler;
import es.lavanda.telegram.bots.filebot.model.FilebotConversation;
import es.lavanda.telegram.bots.filebot.model.FilebotExecution;
import es.lavanda.telegram.bots.filebot.model.FilebotExecution.FilebotExecutionStatus;
import es.lavanda.telegram.bots.filebot.service.FilebotExecutionService;
import es.lavanda.telegram.bots.filebot.utils.TelegramUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TMDBExecutor implements Handler {

    private Handler next;

    private final ProducerService producerService;

    private final FilebotExecutionService filebotExecutionService;

    @Override
    public void setNext(Handler handler) {
        this.next = handler;
    }

    @Override
    public void handleRequest(FilebotConversation filebotConversation, FilebotExecution filebotExecution,
            String callbackResponse) {
        if (filebotExecution.getStatus()
                .equals(FilebotExecutionStatus.TMDB)) {
            log.info("TMDB Executor Link");
            sendMessageToGiveFeedback(filebotExecution,
                    filebotConversation.getChatId());
            sendToTMDBMicroservice(filebotExecution);
            filebotExecution.setStatus(FilebotExecutionStatus.CHECKING_ON_TMDB);
            filebotExecutionService.save(filebotExecution);
            next.handleRequest(filebotConversation, filebotExecution, callbackResponse);
        } else if (next != null) {
            next.handleRequest(filebotConversation, filebotExecution, callbackResponse);
        }
    }

    private void sendMessageToGiveFeedback(FilebotExecution filebotExecution, String chatId) {
        log.info("Send message to give feedback to chatid: {}", chatId);
        TelegramMessage telegramMessage = new TelegramMessage();
        telegramMessage.setText(
                "Buscando información en TMDB");
        telegramMessage.setInlineKeyboardMarkup(TelegramUtils
                .getEmptyInlineKeyboard());
        telegramMessage.setChatId(chatId);
        telegramMessage.setIdFilebotConversation(filebotExecution.getId());
        telegramMessage.setHandler(MessageHandler.FILEBOT);
        telegramMessage.setType(MessageType.TEXT);
        producerService.sendTelegramMessage(telegramMessage);
    }

    private void sendToTMDBMicroservice(FilebotExecution filebotExecution) {
        TelegramFilebotExecutionIDTO telegramFilebotExecutionIDTO = new TelegramFilebotExecutionIDTO();
        telegramFilebotExecutionIDTO.setId(filebotExecution.getId());
        telegramFilebotExecutionIDTO.setFile(filebotExecution.getFiles().get(0)); // TODO:
        telegramFilebotExecutionIDTO.setPath(filebotExecution.getPath());
        if (FilebotCategory.TV.equals(filebotExecution.getCategory())
                || FilebotCategory.TV_EN.equals(filebotExecution.getCategory())) {
            telegramFilebotExecutionIDTO.setType(Type.SHOW);
        } else {
            telegramFilebotExecutionIDTO.setType(Type.FILM);
        }
        producerService.sendTelegramExecutionForTMDB(telegramFilebotExecutionIDTO);
    }

}
