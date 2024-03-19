package es.lavanda.telegram.bots.common.service.chainofresponsability.impl;

import java.util.Objects;

import org.springframework.stereotype.Service;

import es.lavanda.lib.common.model.filebot.FilebotCategory;
import es.lavanda.telegram.bots.common.model.TelegramMessage;
import es.lavanda.telegram.bots.common.model.TelegramMessage.MessageHandler;
import es.lavanda.telegram.bots.common.model.TelegramMessage.MessageType;
import es.lavanda.telegram.bots.common.service.ProducerService;
import es.lavanda.telegram.bots.common.service.chainofresponsability.Handler;
import es.lavanda.telegram.bots.filebot.model.Elected;
import es.lavanda.telegram.bots.filebot.model.FilebotConversation;
import es.lavanda.telegram.bots.filebot.model.FilebotExecution;
import es.lavanda.telegram.bots.filebot.model.FilebotExecution.FilebotExecutionStatus;
import es.lavanda.telegram.bots.filebot.service.ElectedService;
import es.lavanda.telegram.bots.filebot.service.FilebotExecutionService;
import es.lavanda.telegram.bots.filebot.utils.TelegramUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.Iterator;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryExecutor implements Handler {

    private Handler next;

    private final FilebotExecutionService filebotExecutionService;

    private final ProducerService producerService;

    private final ElectedService electedService;

    @Override
    public void setNext(Handler handler) {
        this.next = handler;
    }

    @Override
    public void handleRequest(FilebotConversation filebotConversation, FilebotExecution filebotExecution,
            String callbackResponse) {
        if (filebotExecution.getStatus().equals(FilebotExecutionStatus.UNPROCESSED) ||
                filebotExecution.getStatus().equals(FilebotExecutionStatus.CATEGORY)) {
            log.info("Category Executor Link");
            if (Objects.nonNull(callbackResponse)) {
                filebotExecution = assignCategory(filebotExecution, callbackResponse);
                callbackResponse = null;
                filebotExecution.setOnCallback(false);
                if (Boolean.TRUE.equals(FilebotExecutionStatus.PROCESSED.equals(filebotExecution.getStatus()))) {
                    sendEditMessageReplyMarkupProcessed(filebotConversation, filebotExecution);
                } else {
                    filebotExecution = updateStatus(filebotExecution);
                    sendEditMessageReplyMarkup(filebotConversation, filebotExecution);
                }
            } else {
                filebotExecution.setOnCallback(true);
                filebotExecution.setStatus(FilebotExecutionStatus.CATEGORY);
                filebotExecutionService.save(filebotExecution);
                sendMessageToSelectLabel(filebotExecution,
                        filebotConversation.getChatId());
            }
            if (Objects.nonNull(this.next)) {
                next.handleRequest(filebotConversation, filebotExecution, callbackResponse);
            }
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
        telegramMessage.setText(String.format("Categoria seleccionada *%s* para la carpeta %s",
                filebotExecution.getCategory().getValue(), filebotExecution.getName()));
        producerService.sendTelegramMessage(telegramMessage);
    }

    private void sendEditMessageReplyMarkupProcessed(FilebotConversation filebotConversation,
            FilebotExecution filebotExecution) {
        // List<Elected> electeds = electedService.getAll();

        TelegramMessage telegramMessage = new TelegramMessage();
        telegramMessage.setChatId(filebotConversation.getChatId());
        telegramMessage
                .setInlineKeyboardMarkup(
                        TelegramUtils.getEmptyInlineKeyboard());
        telegramMessage.setText(null);
        telegramMessage.setHandler(MessageHandler.FILEBOT);
        telegramMessage.setType(MessageType.EDIT_MESSAGE);
        telegramMessage.setMessageId(filebotConversation.getPreviousMessageId());
        telegramMessage.setText(String.format("Seleccionado *%s* para *%s*",
                filebotExecution.getQuery(), filebotExecution.getName()));
        producerService.sendTelegramMessage(telegramMessage);
    }

    private FilebotExecution assignCategory(FilebotExecution filebotExecution, String response) {
        List<Elected> electeds = electedService.getAll();
        if (electeds.stream().anyMatch(e -> response.equals(e.getId()))) {
            for (Iterator<Elected> iterator = electeds.iterator(); iterator.hasNext();) {
                Elected elected = iterator.next();
                if (response.equals(elected.getId())) {
                    elected.setTimes(elected.getTimes() + 1);
                    filebotExecution.setAction(elected.getFilebotExecutionODTO().getAction());
                    filebotExecution.setCategory(elected.getFilebotExecutionODTO().getCategory());
                    filebotExecution.setForceStrict(elected.getFilebotExecutionODTO().isForceStrict());
                    filebotExecution.setQuery(elected.getFilebotExecutionODTO().getQuery());
                    filebotExecution
                            .setSelectedPossibilities(elected.getFilebotExecutionODTO().getSelectedPossibilities());
                    filebotExecution.setOnCallback(false);
                    filebotExecution.setStatus(FilebotExecutionStatus.PROCESSED);
                } else {
                    elected.setTimes(elected.getTimes() - 1);
                    if (elected.getTimes() == 0) {
                        iterator.remove();
                    }
                }
            }
            electedService.save(electeds);
            return filebotExecutionService.save(filebotExecution);
        }
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
        List<Elected> elected = electedService.getAll();
        List<String> callBackData = new ArrayList<>();
        List<String> data = new ArrayList<>();
        data.addAll(FilebotCategory.getAllValues());
        callBackData.addAll(FilebotCategory.getAllValues());
        data.addAll(elected.stream().map(e -> TelegramUtils.abbreviate(e.getName(), 64)).collect(Collectors.toList()));
        callBackData.addAll(elected.stream().map(e -> e.getId()).collect(Collectors.toList()));
        telegramMessage
                .setInlineKeyboardMarkup(
                        TelegramUtils.getInlineKeyboard(data, callBackData, false));
        telegramMessage.setCallbackData(callBackData);
        telegramMessage.setHandler(MessageHandler.FILEBOT);
        telegramMessage.setType(MessageType.TEXT);
        producerService.sendTelegramMessage(telegramMessage);
    }

}
