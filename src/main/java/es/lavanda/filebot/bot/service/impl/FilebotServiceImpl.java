package es.lavanda.filebot.bot.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import es.lavanda.filebot.bot.config.BotConfig;
import es.lavanda.filebot.bot.exception.FilebotBotException;
import es.lavanda.filebot.bot.handler.FilebotHandler;
import es.lavanda.filebot.bot.model.FilebotNameSelection;
import es.lavanda.filebot.bot.model.FilebotNameStatus;
import es.lavanda.filebot.bot.model.TelegramMessage;
import es.lavanda.filebot.bot.repository.FilebotNameRepository;
import es.lavanda.filebot.bot.repository.TelegramMessageRepository;
import es.lavanda.filebot.bot.service.FilebotService;
import es.lavanda.filebot.bot.service.ProducerService;
import es.lavanda.lib.common.model.FilebotExecutionIDTO;
import es.lavanda.lib.common.model.FilebotExecutionODTO;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Scope("singleton")
public class FilebotServiceImpl implements FilebotService {

    private FilebotHandler filebotHandler;

    @Autowired
    private FilebotNameRepository filebotNameRepository;

    @Autowired
    private TelegramMessageRepository telegramMessageRepository;

    @Autowired
    private ProducerService producerService;

    @Autowired
    private BotConfig botConfig;

    @Override
    public void run(FilebotExecutionIDTO filebotExecutionIDTO) {
        FilebotNameSelection filebotNameSelection = convertToModel(filebotExecutionIDTO);
        filebotNameSelection.setStatus(FilebotNameStatus.UNPROCESSING);
        filebotNameRepository.save(filebotNameSelection);
    }

    private FilebotNameSelection convertToModel(FilebotExecutionIDTO filebotExecutionIDTO) {
        FilebotNameSelection filebotNameSelection = new FilebotNameSelection();
        filebotNameSelection.setId(filebotExecutionIDTO.getId());
        filebotNameSelection.setFiles(filebotExecutionIDTO.getFiles());
        filebotNameSelection.setPath(filebotExecutionIDTO.getPath());
        filebotNameSelection.setPossibilities(filebotExecutionIDTO.getPossibilities());
        return filebotNameSelection;
    }

    @Scheduled(fixedRate = 60000)
    private void hourlyCheck() {
        log.info("Hourly check...");
        Optional<FilebotNameSelection> optFilebot = filebotNameRepository
                .findByStatusStartsWith("PROCESSING");
        if (Boolean.FALSE.equals(optFilebot.isPresent())) {
            List<FilebotNameSelection> filebots = filebotNameRepository
                    .findAllByStatus(FilebotNameStatus.UNPROCESSING.name());
            if (Boolean.FALSE.equals(filebots.isEmpty())) {
                FilebotNameSelection filebotNameSelection = filebots.get(0);
                if (filebotNameSelection.getPossibilities().isEmpty()) {
                    handleFilebotWithoutPosibilities(filebotNameSelection);
                } else {
                    handleFilebotWithPossibilities(filebotNameSelection);
                }
            }
        }
    }

    private void handleFilebotWithPossibilities(FilebotNameSelection filebotNameSelection) {
        log.info("Filebot with possibilities: {}", filebotNameSelection);
        sendMessageWithPossibilities(filebotNameSelection);
        filebotNameSelection.setStatus(FilebotNameStatus.PROCESSING_WITH_POSSIBILITIES);
        filebotNameRepository.save(filebotNameSelection);
    }

    private void sendMessageWithPossibilities(FilebotNameSelection filebotNameSelection) {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(botConfig.getFilebotAdmin());
        sendMessageRequest
                .setText(String.format("La carpeta es %s.\nLos ficheros son: %s\nSelecciona el posible resultado:",
                        filebotNameSelection.getPath(), filebotNameSelection.getFiles()));
        sendMessageRequest.setReplyMarkup(getReplyKeyboardMarkup(filebotNameSelection.getFiles()));
        // sendMessageRequest.setReplyMarkup(getInlineKeyboard(filebotNameSelection.getFiles()));
        filebotHandler.sendMessage(sendMessageRequest, false);
    }

    private void handleFilebotWithoutPosibilities(FilebotNameSelection filebotNameSelection) {
        log.info("Filebot without possibilities: {}", filebotNameSelection);
        sendMessageToSelectLabel(filebotNameSelection);
        filebotNameSelection.setStatus(FilebotNameStatus.PROCESSING_LABEL);
        filebotNameRepository.save(filebotNameSelection);
    }

    private void sendMessageToSelectLabel(FilebotNameSelection filebotNameSelection) {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(botConfig.getFilebotAdmin());
        StringBuilder sb = new StringBuilder();
        filebotNameSelection.getFiles().forEach(f -> {
            sb.append("◦ " + f.trim());
            sb.append("\n");
        });
        sendMessageRequest
                .setText(String.format(
                        "La carpeta es *%s*.\nLos ficheros son:\n*%s*Selecciona el tipo de contenido:",
                        filebotNameSelection.getPath(), sb.toString()));
        sendMessageRequest.setReplyMarkup(getInlineKeyboard(List.of("Serie", "Pelicula"), List.of("TV", "MOVIE")));
        filebotHandler.sendMessage(sendMessageRequest, false);
    }

    private void sendMessageToForceStrict() {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(botConfig.getFilebotAdmin());
        sendMessageRequest.setText(
                "¿Modo de matcheo del filebot?");
        sendMessageRequest.setReplyMarkup(getInlineKeyboard(List.of("Strict", "Opportunistic")));
        filebotHandler.sendMessage(sendMessageRequest, false);
    }

    private void sendMessageToForceQuery() {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(botConfig.getFilebotAdmin());
        sendMessageRequest.setText(
                "¿Texto extra para potenciar el matcheo? Ex: 'Vengadores: Endgame'");
        sendMessageRequest.setReplyMarkup(getInlineKeyboard(List.of("No")));
        filebotHandler.sendMessage(sendMessageRequest, true);
    }

    private void sendMessage(String text, boolean forceSave) {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(botConfig.getFilebotAdmin());
        sendMessageRequest.setText(text);
        sendMessageRequest.setReplyMarkup(getKeyboardRemove());
        filebotHandler.sendMessage(sendMessageRequest, forceSave);

    }

    private ReplyKeyboard getKeyboardRemove() {
        ReplyKeyboardRemove replyKeyboardRemove = new ReplyKeyboardRemove();
        replyKeyboardRemove.setSelective(true);
        replyKeyboardRemove.setRemoveKeyboard(true);
        return replyKeyboardRemove;
    }

    private FilebotExecutionODTO getFilebotExecutionODTO(FilebotNameSelection filebotNameSelection) {
        FilebotExecutionODTO filebotExecutionODTO = new FilebotExecutionODTO();
        filebotExecutionODTO.setForceStrict(filebotNameSelection.isForceStrict());
        filebotExecutionODTO.setQuery(filebotNameSelection.getQuery());
        filebotExecutionODTO.setLabel(filebotNameSelection.getLabel());
        filebotExecutionODTO.setSelectedPossibilitie(filebotNameSelection.getSelectedPossibilitie());
        filebotExecutionODTO.setId(filebotNameSelection.getId());
        return filebotExecutionODTO;
    }

    private ReplyKeyboardMarkup getReplyKeyboardMarkup(List<String> list) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(true);
        List<KeyboardRow> keyboard = new ArrayList<>();
        for (String languageName : list) {
            KeyboardRow row = new KeyboardRow();
            row.add(languageName);
            keyboard.add(row);
        }
        replyKeyboardMarkup.setKeyboard(keyboard);
        return replyKeyboardMarkup;
    }

    private InlineKeyboardMarkup getInlineKeyboard(List<String> list) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        for (String object : list) {
            InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
            inlineKeyboardButton.setText(object);
            inlineKeyboardButton.setCallbackData(object);
            rowInline.add(inlineKeyboardButton);
        }
        rowsInline.add(rowInline);
        inlineKeyboardMarkup.setKeyboard(rowsInline);
        return inlineKeyboardMarkup;
    }

    private InlineKeyboardMarkup getInlineKeyboard(List<String> data, List<String> callbackData) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
            inlineKeyboardButton.setText(data.get(i));
            inlineKeyboardButton.setCallbackData(callbackData.get(i));
            rowInline.add(inlineKeyboardButton);
        }

        rowsInline.add(rowInline);
        inlineKeyboardMarkup.setKeyboard(rowsInline);
        return inlineKeyboardMarkup;
    }

    private void handleProcessing(FilebotNameSelection filebotNameSelection, String response, String chatId,
            String messageId) {
        switch (filebotNameSelection.getStatus()) {
            case PROCESSING_LABEL:
                handleProcessingLabel(filebotNameSelection, response, chatId, messageId);
                break;
            case PROCESSING_FORCE_STRICT:
                handleProcessingForceStrict(filebotNameSelection, response, chatId, messageId);
                break;
            case PROCESSING_QUERY:
                handleProcessingQuery(filebotNameSelection, response, chatId, messageId);
                break;
            case PROCESSING_WITH_POSSIBILITIES:
                handleProcessingWithPossibilities(filebotNameSelection, response, chatId, messageId);
                break;
            default:
                log.error("It should not be here");
                break;
        }
    }

    private void handleProcessingWithPossibilities(FilebotNameSelection filebotNameSelection, String response,
            String chatId, String messageId) {
        log.info("Handle processing with possibilities");
    }

    private void handleProcessingQuery(FilebotNameSelection filebotNameSelection, String response, String chatId,
            String messageId) {
        log.info("Handle processing query");
        if (Boolean.FALSE.equals(response.equalsIgnoreCase("NO"))) {
            filebotNameSelection.setQuery(response);
        }
        producerService.sendFilebotExecution(
                getFilebotExecutionODTO(filebotNameSelection));
        filebotNameSelection.setStatus(FilebotNameStatus.PROCESSED);
        filebotNameRepository.save(filebotNameSelection);
        // if (Objects.nonNull(messageId)) {

        sendMessageToEditedMessage(chatId, getLastMessageId(chatId).getMessageId(),
                String.format("Texto para forzar la query del contenido: %s", response));
        telegramMessageRepository.deleteAllByChatId(chatId);
        sendMessage("Guardado y procesado", false);
        // } else {
        // cleanInlineKeyboard(chatId);
        // sendMessage("Guardado y procesado", false);
        // }
        log.info("Processed filebotNameSelectionId: " +
                filebotNameSelection.getPath());
        hourlyCheck();
    }

    private TelegramMessage getLastMessageId(String chatId) {
        return telegramMessageRepository.findByChatIdOrderByIdDesc(chatId)
                .orElseThrow(() -> new FilebotBotException(String.format("Not found chat id  %s", chatId)));
    }

    private void cleanInlineKeyboard(String chatId) {
        log.info("Clean inline keyboard");
        List<TelegramMessage> allTelegramInline = telegramMessageRepository.findAllByChatId(chatId);
        for (TelegramMessage telegramInlineKeyboard : allTelegramInline) {
            log.info("Cleaning");
            cleanInlineKeyboard(telegramInlineKeyboard.getChatId(), telegramInlineKeyboard.getMessageId());
            telegramMessageRepository.delete(telegramInlineKeyboard);
        }
    }

    private void handleProcessingForceStrict(FilebotNameSelection filebotNameSelection, String response, String chatId,
            String messageId) {
        log.info("Handle processing force strict");
        filebotNameSelection.setForceStrict(response.equalsIgnoreCase("Strict"));
        sendMessageToEditedMessage(chatId, messageId, String.format(
                "Modo de matcheo del filebot: %s ", filebotNameSelection.isForceStrict() ? "Strict" : "Opportunistic"));
        sendMessageToForceQuery();
        filebotNameSelection.setStatus(FilebotNameStatus.PROCESSING_QUERY);
        filebotNameRepository.save(filebotNameSelection);
    }

    private void handleProcessingLabel(FilebotNameSelection filebotNameSelection, String response, String chatId,
            String messageId) {
        log.info("Handle processing label");
        filebotNameSelection.setLabel(response);
        StringBuilder sb = new StringBuilder();

        filebotNameSelection.getFiles().forEach(f -> {
            sb.append("◦ " + f.trim());
            sb.append("\n");
        });
        sendMessageToEditedMessage(chatId, messageId, String.format(
                "La carpeta es *%s*.\nLos ficheros son:\n*%s*Tipo de contenido seleccionado: *%s*",
                sb.toString(), filebotNameSelection.getPath(), response));
        sendMessageToForceStrict();
        filebotNameSelection.setStatus(FilebotNameStatus.PROCESSING_FORCE_STRICT);
        filebotNameRepository.save(filebotNameSelection);
    }

    @Override
    public void setFilebotHandler(FilebotHandler filebotHandler) {
        this.filebotHandler = filebotHandler;
    }

    @Override
    public void handleCallbackResponse(String chatId, String messageId, String response) {
        log.info("Handle callback message");
        filebotNameRepository.findByStatusStartsWith("PROCESSING").ifPresent(
                (filebotNameSelection) -> {
                    handleProcessing(filebotNameSelection, response, chatId, messageId);
                });
    }

    @Override
    public void handleIncomingResponse(String chatId, String response) {
        log.info("Handle incomming message");
        filebotNameRepository.findByStatusStartsWith("PROCESSING").ifPresent(
                (filebotNameSelection) -> {
                    handleProcessing(filebotNameSelection, response, chatId, null);
                });
    }

    private void sendMessageToEditedMessage(String chatId, String messageId, String response) {
        log.info("Send message to edited message");
        EditMessageText new_message = new EditMessageText();
        new_message.setChatId(chatId);
        new_message.setMessageId(Integer.parseInt(messageId));
        new_message.setText(response);
        filebotHandler.sendMessage(new_message);
    }

    private void cleanInlineKeyboard(String chatId, String messageId) {
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(chatId);
        deleteMessage.setMessageId(Integer.parseInt(messageId));
        filebotHandler.deleteMessage(deleteMessage);
        // EditMessageReplyMarkup edited = new EditMessageReplyMarkup();
        // edited.setChatId(chatId);
        // edited.setMessageId(Integer.parseInt(messageId));
        // edited.setReplyMarkup(getEmptyInlineKeyboard());
        // sendMessageToEditedMessage(chatId, messageId, "Empty");
    }

    private InlineKeyboardMarkup getEmptyInlineKeyboard() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        rowsInline.add(rowInline);
        inlineKeyboardMarkup.setKeyboard(rowsInline);
        return inlineKeyboardMarkup;
    }

    @Override
    public void saveMessageId(String chatId, String messageId) {
        log.info("Save message id");
        TelegramMessage telegramInlineKeyboard = new TelegramMessage();
        telegramInlineKeyboard.setChatId(chatId);
        telegramInlineKeyboard.setMessageId(messageId);
        telegramMessageRepository.save(telegramInlineKeyboard);
    }

}
