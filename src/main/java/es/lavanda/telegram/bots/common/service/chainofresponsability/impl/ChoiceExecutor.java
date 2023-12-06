package es.lavanda.telegram.bots.common.service.chainofresponsability.impl;

import java.util.Objects;

import org.springframework.stereotype.Service;

import es.lavanda.lib.common.model.filebot.FilebotCategory;
import es.lavanda.lib.common.model.tmdb.search.TMDBResultDTO;
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
public class ChoiceExecutor implements Handler {

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
        if (filebotExecution.getStatus().equals(FilebotExecutionStatus.CHOICE)) {
            log.info("Choice Executor Link");
            if (Objects.nonNull(callbackResponse)) {
                if ("0".equalsIgnoreCase(callbackResponse)) {
                    filebotExecution.setQuery(null);
                    sendEditMessageReplyMarkup(filebotConversation, "No se ha seleccionado ninguna opción");
                    cleanOldMessages(filebotConversation);
                    sendMessage(String.format("Procesado correctamente %s", filebotExecution.getName()),
                            filebotConversation.getChatId());
                    callbackResponse = null;
                    filebotExecution.setOnCallback(false);
                    log.info("Processed telegramFilebotExecutionId: "
                            + filebotExecution.getPath());
                    filebotExecution = updateStatus(filebotExecution);
                } else if ("data".startsWith(callbackResponse)) {
                    String idTMDB = callbackResponse.split("data")[1];
                    TMDBResultDTO resultToMoreData = filebotExecution
                            .getPossibleChoicesTMDB().get(idTMDB);
                    if (Objects.isNull(resultToMoreData.getPosterPath())) {
                        sendMessage(resultToMoreData.getOverview(), filebotConversation.getChatId());

                    } else {
                        sendPhoto(resultToMoreData.getOverview(),
                                "https://image.tmdb.org/t/p/w500" + resultToMoreData.getPosterPath(),
                                filebotConversation.getChatId());
                    }
                } else {
                    filebotExecution.setQuery(callbackResponse);
                    filebotExecution = updateStatus(filebotExecution);
                    callbackResponse = null;
                    filebotExecution.setOnCallback(false);
                    sendEditMessageReplyMarkup(filebotConversation, getEditMessageReply(filebotExecution));
                    sendMessage("Procesado correctamente", filebotConversation.getChatId());
                    cleanOldMessages(filebotConversation);
                    log.info("Processed telegramFilebotExecutionId: "
                            + filebotExecution.getPath());
                }
            } else {
                filebotExecution.setOnCallback(true);
                filebotExecutionService.save(filebotExecution);
                sendMessageToSelectChoice(filebotExecution,
                        filebotConversation.getChatId());
            }
            if (Objects.nonNull(this.next)) {
                next.handleRequest(filebotConversation, filebotExecution, callbackResponse);
            }
        } else if (next != null) {
            next.handleRequest(filebotConversation, filebotExecution, callbackResponse);
        }
    }

    private String getEditMessageReply(FilebotExecution filebotExecution) {
        if (FilebotCategory.FILM.equals(filebotExecution.getCategory())) {
            return String.format("Seleccionado: %s",
                    filebotExecution.getPossibleChoicesTMDB()
                            .get(filebotExecution.getQuery())
                            .getTitle() +
                            " ("
                            +
                            filebotExecution.getPossibleChoicesTMDB()
                                    .get(filebotExecution.getQuery())
                                    .getReleaseDate()
                            + ")");
        } else {
            return String.format("Seleccionado: %s",
                    filebotExecution.getPossibleChoicesTMDB()
                            .get(filebotExecution.getQuery())
                            .getName() +
                            " ("
                            +
                            filebotExecution.getPossibleChoicesTMDB()
                                    .get(filebotExecution.getQuery())
                                    .getFirstAirDate()
                            + ")");
        }

    }

    private void sendMessage(String overview, String chatId) {
        TelegramMessage telegramMessage = new TelegramMessage();
        telegramMessage.setChatId(chatId);
        telegramMessage
                .setInlineKeyboardMarkup(
                        TelegramUtils.getEmptyInlineKeyboard());
        telegramMessage.setText(overview);
        telegramMessage.setHandler(MessageHandler.FILEBOT);
        telegramMessage.setType(MessageType.TEXT);
        producerService.sendTelegramMessage(telegramMessage);
    }

    private FilebotExecution updateStatus(FilebotExecution filebotExecution) {
        filebotExecution.setStatus(FilebotExecutionStatus.PROCESSED);
        return filebotExecutionService.save(filebotExecution);
    }

    private void sendPhoto(String overview, String photo, String chatId) {
        TelegramMessage telegramMessage = new TelegramMessage();
        telegramMessage.setChatId(chatId);
        telegramMessage
                .setInlineKeyboardMarkup(
                        TelegramUtils.getEmptyInlineKeyboard());
        telegramMessage.setText(overview);
        telegramMessage.setPhotoUrl(photo);
        telegramMessage.setHandler(MessageHandler.FILEBOT);
        telegramMessage.setType(MessageType.PHOTO);
        producerService.sendTelegramMessage(telegramMessage);
    }

    private void cleanOldMessages(FilebotConversation filebotConversation) {
        for (String otherMessagesId : filebotConversation.getPhotosMessageIds()) {
            TelegramMessage telegramMessage = new TelegramMessage();
            telegramMessage.setChatId(filebotConversation.getChatId());
            telegramMessage
                    .setInlineKeyboardMarkup(
                            TelegramUtils.getEmptyInlineKeyboard());
            telegramMessage.setText(null);
            telegramMessage.setHandler(MessageHandler.FILEBOT);
            telegramMessage.setType(MessageType.DELETE_MESSAGE);
            telegramMessage.setMessageId(otherMessagesId);
            producerService.sendTelegramMessage(telegramMessage);
        }
    }

    private void sendMessageToSelectChoice(FilebotExecution filebotExecution, String chatId) {
        log.info("Sending message to select choice");
        TelegramMessage telegramMessage = new TelegramMessage();
        telegramMessage.setChatId(chatId);
        telegramMessage.setText(
                "¿Cual crees que es?:");
        telegramMessage.setInlineKeyboardMarkup(
                TelegramUtils.getInlineKeyboardForChoices(filebotExecution.getPossibleChoicesTMDB(),
                        FilebotCategory.FILM.equals(filebotExecution.getCategory())));
        telegramMessage.setIdFilebotConversation(filebotExecution.getId());
        telegramMessage.setCallbackData(FilebotCategory.getAllValues());
        telegramMessage.setHandler(MessageHandler.FILEBOT);
        telegramMessage.setType(MessageType.TEXT);
        producerService.sendTelegramMessage(telegramMessage);
    }

    private void sendEditMessageReplyMarkup(FilebotConversation filebotConversation, String string) {
        TelegramMessage telegramMessage = new TelegramMessage();
        telegramMessage.setChatId(filebotConversation.getChatId());
        telegramMessage
                .setInlineKeyboardMarkup(
                        TelegramUtils.getEmptyInlineKeyboard());
        telegramMessage.setText(string);
        telegramMessage.setHandler(MessageHandler.FILEBOT);
        telegramMessage.setType(MessageType.EDIT_MESSAGE);
        telegramMessage.setMessageId(filebotConversation.getPreviousMessageId());
        producerService.sendTelegramMessage(telegramMessage);
    }

}
