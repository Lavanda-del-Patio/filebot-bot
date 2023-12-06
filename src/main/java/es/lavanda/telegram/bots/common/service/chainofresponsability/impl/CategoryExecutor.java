package es.lavanda.telegram.bots.common.service.chainofresponsability.impl;

import java.util.Objects;

import org.springframework.stereotype.Service;

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
public class CategoryExecutor implements Handler {

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
                .equals(FilebotExecutionStatus.CATEGORY)) {
            log.info("Category Executor Link");
            if (Objects.nonNull(callbackResponse)) {
                filebotExecution = assignCategory(filebotExecution, callbackResponse);
                callbackResponse = null;
                filebotExecution.setOnCallback(false);
                filebotExecution = updateStatus(filebotExecution);
                sendEditMessageReplyMarkup(filebotConversation, filebotExecution);
            } else {
                filebotExecution.setOnCallback(true);
                filebotExecutionService.save(filebotExecution);
                sendMessageToSelectLabel(filebotExecution,
                        filebotConversation.getChatId());
            }
            next.handleRequest(filebotConversation, filebotExecution, callbackResponse);
        } else if (next != null) {
            next.handleRequest(filebotConversation, filebotExecution, callbackResponse);
        }
    }

    private FilebotExecution updateStatus(FilebotExecution filebotExecution) {
        filebotExecution.setStatus(FilebotExecutionStatus.FORCE_STRICT);
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
        telegramMessage.setText(String.format("Categoria Seleccionada *%s* para la carpeta %s",
                filebotExecution.getCategory().getValue(), filebotExecution.getName()));
        producerService.sendTelegramMessage(telegramMessage);
    }

    private FilebotExecution assignCategory(FilebotExecution filebotExecution, String response) {
        if (FilebotCategory.FILM.getValue().equals(response)) {
            filebotExecution.setCategory(FilebotCategory.FILM);
        } else if (FilebotCategory.TV.getValue().equals(response)) {
            filebotExecution.setCategory(FilebotCategory.TV);
        } else if (FilebotCategory.TV_EN.getValue().equals(response)) {
            filebotExecution.setCategory(FilebotCategory.TV_EN);
        }
        return filebotExecutionService.save(filebotExecution);
    }

    private void sendMessageToSelectLabel(FilebotExecution filebotExecution, String chatId) {
        log.info("Send message to select label to chatid: {}", chatId);
        TelegramMessage telegramMessage = new TelegramMessage();

        telegramMessage.setChatId(chatId);
        telegramMessage.setIdFilebotConversation(filebotExecution.getId());
        StringBuilder sb = new StringBuilder();
        filebotExecution.getFiles().forEach(f -> {
            sb.append("◦ " + f.trim());
            sb.append("\n");
        });
        telegramMessage
                .setText(String.format(
                        "La carpeta es *%s*.\nLos ficheros son:\n*%s*Selecciona el tipo de contenido:",
                        filebotExecution.getName(), TelegramUtils.abbreviate(sb.toString(), 400)));
        telegramMessage
                .setInlineKeyboardMarkup(
                        TelegramUtils.getInlineKeyboard(FilebotCategory.getAllValues(), false));
        telegramMessage.setCallbackData(FilebotCategory.getAllValues());
        telegramMessage.setHandler(MessageHandler.FILEBOT);
        telegramMessage.setType(MessageType.TEXT);
        producerService.sendTelegramMessage(telegramMessage);
    }

}
