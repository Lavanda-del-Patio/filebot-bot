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
public class ForceStrictExecutor implements Handler {

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
                .equals(FilebotExecutionStatus.FORCE_STRICT)) {
            log.info("ForceStrict Executor Link");
            if (Objects.nonNull(callbackResponse)) {
                filebotExecution = assignForceStrict(filebotExecution, callbackResponse);
                callbackResponse = null;
                sendEditMessageReplyMarkup(filebotConversation, filebotExecution.isForceStrict());
                filebotExecution = updateStatus(filebotExecution);
            } else {
                sendMessageToForceStrict(filebotExecution,
                        filebotConversation.getChatId());
            }
            next.handleRequest(filebotConversation, filebotExecution, callbackResponse);
        } else if (next != null) {
            next.handleRequest(filebotConversation, filebotExecution, callbackResponse);
        }
    }

    private FilebotExecution updateStatus(FilebotExecution filebotExecution) {
        filebotExecution.setStatus(FilebotExecutionStatus.ACTION);
        return filebotExecutionService.save(filebotExecution);
    }

    private FilebotExecution assignForceStrict(FilebotExecution filebotExecution, String callbackResponse) {
        if (callbackResponse.equals("true")) {
            filebotExecution.setForceStrict(true);
        } else if (callbackResponse.equals("false")) {
            filebotExecution.setForceStrict(false);
        }
        return filebotExecutionService.save(filebotExecution);
    }

    private void sendMessageToForceStrict(FilebotExecution filebotExecution, String chatId) {
        log.info("Send message to select strict to chatid: {}", chatId);
        TelegramMessage telegramMessage = new TelegramMessage();
        telegramMessage.setText(
                "¿Precision de Filebot?");
        telegramMessage.setInlineKeyboardMarkup(TelegramUtils
                .getInlineKeyboard(List.of("Estricto", "Oportunista", TelegramUtils.BACK_BUTTON_VALUE),
                        List.of("true", "false", TelegramUtils.BACK_BUTTON_KEY), false));
        telegramMessage.setChatId(chatId);
        telegramMessage.setIdFilebotConversation(filebotExecution.getId());
        telegramMessage.setCallbackData(List.of("true", "false", "back"));
        telegramMessage.setHandler(MessageHandler.FILEBOT);
        telegramMessage.setType(MessageType.TEXT);
        producerService.sendTelegramMessage(telegramMessage);
    }

    private void sendEditMessageReplyMarkup(FilebotConversation filebotConversation, boolean forceStrict) {
        TelegramMessage telegramMessage = new TelegramMessage();
        telegramMessage.setChatId(filebotConversation.getChatId());
        telegramMessage
                .setInlineKeyboardMarkup(
                        TelegramUtils.getEmptyInlineKeyboard());
        telegramMessage.setText(null);
        telegramMessage.setHandler(MessageHandler.FILEBOT);
        telegramMessage.setType(MessageType.EDIT_MESSAGE);
        telegramMessage.setMessageId(filebotConversation.getPreviousMessageId());
        telegramMessage.setText(String.format("Precision Seleccionada: *%s*",
                forceStrict ? "Estricto" : "Oportunista"));
        producerService.sendTelegramMessage(telegramMessage);
    }
}
