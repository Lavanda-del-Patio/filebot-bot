package es.lavanda.telegram.bots.common.service.chainofresponsability.impl;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

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
public class TestExecutor implements Handler {

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
        if (filebotExecution.getStatus()
                .equals(FilebotExecutionStatus.TEST)) {
            log.info("Test Executor Link");
            if (Objects.nonNull(callbackResponse)) {
                filebotExecution = approved(filebotExecution, callbackResponse);
                callbackResponse = null;
                filebotExecution.setOnCallback(false);
                filebotExecution = updateStatus(filebotExecution);
                sendEditMessageReplyMarkup(filebotConversation, filebotExecution);
            } else {
                filebotExecution.setOnCallback(true);
                filebotExecutionService.save(filebotExecution);
                sendMessageToApprove(filebotExecution,
                        filebotConversation.getChatId());
            }
            next.handleRequest(filebotConversation, filebotExecution, callbackResponse);
        } else if (next != null) {
            next.handleRequest(filebotConversation, filebotExecution, callbackResponse);
        }
    }

    private FilebotExecution approved(FilebotExecution filebotExecution, String callbackResponse) {
        if (callbackResponse.equals("Si")) {
            filebotExecution.setApproved(true);
            return filebotExecution;
        } else {
            filebotExecution.setApproved(false);
            return filebotExecution;
        }
    }

    private FilebotExecution updateStatus(FilebotExecution filebotExecution) {
        filebotExecution.setStatus(FilebotExecutionStatus.FINISHED);
        return filebotExecutionService.save(filebotExecution);
    }

    private void sendEditMessageReplyMarkup(FilebotConversation filebotConversation,
            FilebotExecution filebotExecution) {
        TelegramMessage telegramMessage = new TelegramMessage();
        telegramMessage.setChatId(filebotConversation.getChatId());
        telegramMessage
                .setInlineKeyboardMarkup(
                        TelegramUtils.getEmptyInlineKeyboard());
        telegramMessage.setText(null);
        telegramMessage.setHandler(MessageHandler.FILEBOT);
        telegramMessage.setType(MessageType.EDIT_MESSAGE);
        telegramMessage.setMessageId(filebotConversation.getPreviousMessageId());
        telegramMessage.setText(String.format("Aprobado!"));
        producerService.sendTelegramMessage(telegramMessage);
    }

    private void sendMessageToApprove(FilebotExecution filebotExecution, String chatId) {
        log.info("Send message to approve to chatid: {}", chatId);
        TelegramMessage telegramMessage = new TelegramMessage();
        telegramMessage.setChatId(chatId);
        telegramMessage.setIdFilebotConversation(filebotExecution.getId());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < filebotExecution.getFiles().size(); i++) {
            sb.append("◦ " + filebotExecution.getFiles().get(i).trim() + " -> "
                    + filebotExecution.getPossibilities().get(i).trim());
            sb.append("\n");
        }
        telegramMessage
                .setText(String.format(
                        "¿Apruebas la modificación para %s?: \n\n%s",
                        filebotExecution.getName(), TelegramUtils.abbreviate(sb.toString(), 400)));
        telegramMessage
                .setInlineKeyboardMarkup(
                        TelegramUtils.getInlineKeyboard(List.of("Si", "No"), false));
        telegramMessage.setCallbackData(List.of("Si", "No"));
        telegramMessage.setHandler(MessageHandler.FILEBOT);
        telegramMessage.setType(MessageType.TEXT);
        producerService.sendTelegramMessage(telegramMessage);
    }

}
