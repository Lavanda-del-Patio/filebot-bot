package es.lavanda.telegram.bots.common.service.chainofresponsability.impl;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import es.lavanda.lib.common.model.filebot.FilebotAction;
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
public class ActionExecutor implements Handler {

    private Handler next;

    private final FilebotExecutionService filebotExecutionService;

    private final ProducerService producerService;

    @Override
    public void setNext(Handler handler) {
        this.next = handler;
    }

    @Override
    public void handleRequest(FilebotConversation filebotConversation, FilebotExecution filebotExecution,
            String callbackResponse) {
        if (filebotExecution.getStatus().equals(FilebotExecutionStatus.ACTION)) {
            log.info("Action Executor Link");
            if (Objects.nonNull(callbackResponse)) {
                filebotExecution = assignAction(filebotExecution, callbackResponse);
                callbackResponse = null;
                filebotExecution.setOnCallback(false);
                filebotExecution = updateStatus(filebotExecution);
                sendEditMessageReplyMarkup(filebotConversation, filebotExecution.getAction());
            } else {
                filebotExecution.setOnCallback(true);
                filebotExecutionService.save(filebotExecution);
                sendMessageToSelectAction(filebotExecution,
                        filebotConversation.getChatId());
            }
            next.handleRequest(filebotConversation, filebotExecution, callbackResponse);
        } else if (next != null) {
            next.handleRequest(filebotConversation, filebotExecution, callbackResponse);
        }
    }

    private FilebotExecution updateStatus(FilebotExecution filebotExecution) {
        filebotExecution.setStatus(FilebotExecutionStatus.TMDB);
        return filebotExecutionService.save(filebotExecution);
    }

    private void sendMessageToSelectAction(FilebotExecution filebotExecution, String chatId) {
        log.info("Send message to select action to chatid: {}", chatId);
        TelegramMessage telegramMessage = new TelegramMessage();

        telegramMessage.setChatId(chatId);
        telegramMessage.setIdFilebotConversation(filebotExecution.getId());
        telegramMessage
                .setText(String.format(
                        "¿Que accion quieres hacer?"));
        List.of(FilebotAction.getAllValues(), TelegramUtils.BACK_BUTTON_VALUE);
        List<String> values = FilebotAction.getAllValues();
        values.add(TelegramUtils.BACK_BUTTON_VALUE);
        List<String> keys = FilebotAction.getAllValues();
        keys.add(TelegramUtils.BACK_BUTTON_KEY);
        telegramMessage
                .setInlineKeyboardMarkup(
                        TelegramUtils.getInlineKeyboard(
                                values,
                                keys,
                                false));
        telegramMessage.setCallbackData(FilebotCategory.getAllValues());
        telegramMessage.setHandler(MessageHandler.FILEBOT);
        telegramMessage.setType(MessageType.TEXT);
        producerService.sendTelegramMessage(telegramMessage);
    }

    private FilebotExecution assignAction(FilebotExecution filebotExecution, String callbackResponse) {
        if (FilebotAction.COPY.getValue().equals(callbackResponse)) {
            filebotExecution.setAction(FilebotAction.COPY);
        } else if (FilebotAction.MOVE.getValue().equals(callbackResponse)) {
            filebotExecution.setAction(FilebotAction.MOVE);
        }
        return filebotExecutionService.save(filebotExecution);
    }

    private void sendEditMessageReplyMarkup(FilebotConversation filebotConversation, FilebotAction action) {
        TelegramMessage telegramMessage = new TelegramMessage();
        telegramMessage.setChatId(filebotConversation.getChatId());
        telegramMessage
                .setInlineKeyboardMarkup(
                        TelegramUtils.getEmptyInlineKeyboard());
        telegramMessage.setText(null);
        telegramMessage.setHandler(MessageHandler.FILEBOT);
        telegramMessage.setType(MessageType.EDIT_MESSAGE);
        telegramMessage.setMessageId(filebotConversation.getPreviousMessageId());
        telegramMessage.setText(String.format("Accion Seleccionada: *%s*",
                action.getValue()));
        producerService.sendTelegramMessage(telegramMessage);
    }
}
