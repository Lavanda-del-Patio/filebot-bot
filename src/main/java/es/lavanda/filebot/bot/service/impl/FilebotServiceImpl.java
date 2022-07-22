package es.lavanda.filebot.bot.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import es.lavanda.filebot.bot.handler.FilebotHandler;
import es.lavanda.filebot.bot.model.TelegramFilebotExecution;
import es.lavanda.filebot.bot.model.TelegramConversation;
import es.lavanda.filebot.bot.model.TelegramConversation.TelegramStatus;
import es.lavanda.filebot.bot.model.TelegramFilebotExecution.FilebotNameStatus;
import es.lavanda.filebot.bot.repository.TelegramConversationRepository;
import es.lavanda.filebot.bot.repository.TelegramFilebotExecutionRepository;
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
    private TelegramFilebotExecutionRepository telegramFilebotExecutionRepository;

    @Autowired
    private TelegramConversationRepository telegramConversationRepository;

    @Autowired
    private ProducerService producerService;

    @Override
    public void run(FilebotExecutionIDTO filebotExecutionIDTO) {
        TelegramFilebotExecution filebotNameSelection = convertToModel(filebotExecutionIDTO);
        filebotNameSelection.setStatus(FilebotNameStatus.UNPROCESSING);
        telegramFilebotExecutionRepository.save(filebotNameSelection);
        producerService.sendTelegramExecution(filebotNameSelection);
    }

    @Override
    public void newConversation(String chatId, String name) {
        telegramConversationRepository.findByChatId(chatId).ifPresentOrElse((telegramConversation) -> {
            telegramConversation.setStatus(TelegramStatus.IDLE);
            telegramConversationRepository.save(telegramConversation);
            sendMessage("Reiniciado...", chatId);
        }, () -> {
            TelegramConversation telegramConversation = new TelegramConversation();
            telegramConversation.setChatId(chatId);
            telegramConversation.setName(name);
            telegramConversation.setStatus(TelegramStatus.IDLE);
            telegramConversationRepository.save(telegramConversation);
            processNotProcessing();
        });
    }

    public void processNotProcessing(TelegramFilebotExecution telegramFilebotExecution) {
        log.info("processNotProcessing whith model method...");
        if (Boolean.FALSE.equals(telegramFilebotExecutionRepository
                .findByStatusStartsWith("PROCESSING").isPresent())) {
            List<TelegramConversation> telegramConversations = telegramConversationRepository
                    .findAllByStatus(TelegramStatus.IDLE.toString());
            for (TelegramConversation telegramConversation : telegramConversations) {
                String messageId = null;
                if (telegramFilebotExecution.getPossibilities().isEmpty()) {
                    messageId = sendMessageToSelectLabel(telegramFilebotExecution,
                            telegramConversation.getChatId());
                    telegramFilebotExecution.setStatus(FilebotNameStatus.PROCESSING_LABEL);
                } else {
                    messageId = sendMessageWithPossibilities(telegramFilebotExecution,
                            telegramConversation.getChatId());
                    telegramFilebotExecution.setStatus(FilebotNameStatus.PROCESSING_WITH_POSSIBILITIES);
                }
                telegramFilebotExecutionRepository.save(telegramFilebotExecution);
                telegramConversation.setStatus(TelegramStatus.WAITING_USER_RESPONSE);
                telegramConversation.setMessageId(messageId);
                log.info("Saving telegram conversation with status {} and messageId {}", "WAITING_USER_RESPONSE",
                        messageId);
                telegramConversationRepository.save(telegramConversation);
            }
        }
    }

    public void processNotProcessing() {
        log.info("processNotProcessing with list of chatIds...");
        if (Boolean.FALSE.equals(telegramFilebotExecutionRepository
                .findByStatusStartsWith("PROCESSING").isPresent())) {
            List<TelegramFilebotExecution> filebots = telegramFilebotExecutionRepository
                    .findAllByStatus(FilebotNameStatus.UNPROCESSING.name());
            if (Boolean.FALSE.equals(filebots.isEmpty())) {
                processNotProcessing(filebots.get(0));
            }
        }
    }

    private String sendMessageWithPossibilities(TelegramFilebotExecution telegramFilebotExecution, String chatId) {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(chatId);
        sendMessageRequest
                .setText(String.format("La carpeta es %s.\nLos ficheros son: %s\nSelecciona el posible resultado:",
                        telegramFilebotExecution.getPath(), telegramFilebotExecution.getFiles()));
        sendMessageRequest.setReplyMarkup(getReplyKeyboardMarkup(telegramFilebotExecution.getFiles()));
        // sendMessageRequest.setReplyMarkup(getInlineKeyboard(filebotNameSelection.getFiles()));
        return filebotHandler.sendMessage(sendMessageRequest);
    }

    private String sendMessageToSelectLabel(TelegramFilebotExecution filebotNameSelection, String chatId) {
        log.info("Send message to select label to chatid: {}", chatId);
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(chatId);
        StringBuilder sb = new StringBuilder();
        filebotNameSelection.getFiles().forEach(f -> {
            sb.append("◦ " + f.trim());
            sb.append("\n");
        });
        sendMessageRequest
                .setText(String.format(
                        "La carpeta es *%s*.\nLos ficheros son:\n*%s*Selecciona el tipo de contenido:",
                        filebotNameSelection.getPath(), abbreviate(sb.toString(), 400)));
        sendMessageRequest.setReplyMarkup(getInlineKeyboard(List.of("Serie", "Pelicula"), List.of("TV", "MOVIE")));
        return filebotHandler.sendMessage(sendMessageRequest);
    }

    private String sendMessageToForceStrict(String chatId) {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(chatId);
        sendMessageRequest.setText(
                "¿Modo de matcheo del filebot?");
        sendMessageRequest.setReplyMarkup(getInlineKeyboard(List.of("Strict", "Opportunistic")));
        return filebotHandler.sendMessage(sendMessageRequest);
    }

    private String sendMessageToForceQuery(String chatId) {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(chatId);
        sendMessageRequest.setText(
                "¿Texto extra para potenciar el matcheo? Ex: 'Vengadores: Endgame'");
        sendMessageRequest.setReplyMarkup(getInlineKeyboard(List.of("No")));
        return filebotHandler.sendMessage(sendMessageRequest);
    }

    private String sendMessage(String text, String chatId) {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(chatId);
        sendMessageRequest.setText(abbreviate(text, 3000));
        sendMessageRequest.setReplyMarkup(getKeyboardRemove());
        return filebotHandler.sendMessage(sendMessageRequest);
    }

    private ReplyKeyboard getKeyboardRemove() {
        ReplyKeyboardRemove replyKeyboardRemove = new ReplyKeyboardRemove();
        replyKeyboardRemove.setSelective(true);
        replyKeyboardRemove.setRemoveKeyboard(true);
        return replyKeyboardRemove;
    }

    private FilebotExecutionODTO getFilebotExecutionODTO(TelegramFilebotExecution filebotNameSelection) {
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

    private void handleProcessing(TelegramFilebotExecution telegramFilebotExecution, String response, String chatId,
            String messageId) {
        log.info("Handle processing for telegramFilebotExecution: {}", telegramFilebotExecution.toString());
        switch (telegramFilebotExecution.getStatus()) {
            case PROCESSING_LABEL:
                handleProcessingLabel(telegramFilebotExecution, response, chatId, messageId);
                break;
            case PROCESSING_FORCE_STRICT:
                handleProcessingForceStrict(telegramFilebotExecution, response, chatId, messageId);
                break;
            case PROCESSING_QUERY:
                handleProcessingQuery(telegramFilebotExecution, response, chatId, messageId);
                break;
            case PROCESSING_WITH_POSSIBILITIES:
                handleProcessingWithPossibilities(telegramFilebotExecution, response, chatId, messageId);
                break;
            default:
                log.error("It should not be here");
                break;
        }
    }

    private void handleProcessingWithPossibilities(TelegramFilebotExecution filebotNameSelection, String response,
            String chatId, String messageId) {
        log.info("Handle processing with possibilities");
    }

    private void handleProcessingQuery(TelegramFilebotExecution telegramFilebotExecution, String response,
            String chatId,
            String messageId) {
        log.info("Handle processing query");
        if (Boolean.FALSE.equals(response.equalsIgnoreCase("NO"))) {
            telegramFilebotExecution.setQuery(response);
        }
        producerService.sendFilebotExecution(
                getFilebotExecutionODTO(telegramFilebotExecution));
        telegramFilebotExecution.setStatus(FilebotNameStatus.PROCESSED);
        telegramFilebotExecutionRepository.save(telegramFilebotExecution);
        if (Objects.nonNull(messageId)) {
            sendMessageToEditedMessage(chatId, messageId,
                    String.format("Texto para forzar la query del contenido: %s", response));
            // telegramMessageRepository.deleteAllByChatId(chatId);
            sendMessage("Guardado y procesado", chatId);
        } else {
            cleanInlineKeyboard(chatId);
            sendMessage("Guardado y procesado", chatId);
        }
        log.info("Processed telegramFilebotExecutionId: " +
                telegramFilebotExecution.getPath());
        TelegramConversation telegramConversation = telegramConversationRepository.findByChatId(chatId).get();
        telegramConversation.setStatus(
                TelegramStatus.IDLE);
        telegramConversationRepository.save(telegramConversation);
        processNotProcessing();
    }

    private void cleanInlineKeyboard(String chatId) {
        log.info("Clean inline keyboard");
        log.info("Cleaning");
        cleanInlineKeyboard(chatId, "telegramInlineKeyboard.getMessageId()");
    }

    private void handleProcessingForceStrict(TelegramFilebotExecution filebotNameSelection, String response,
            String chatId,
            String messageId) {
        log.info("Handle processing force strict");
        filebotNameSelection.setForceStrict(response.equalsIgnoreCase("Strict"));
        // List<TelegramConversation> telegramConversations =
        // telegramConversationRepository.findAllByStatus(
        // TelegramStatus.WAITING_USER_RESPONSE.toString());
        // for (TelegramConversation telegramConversation : telegramConversations) {
        // if (Boolean.FALSE.equals(telegramConversation.getChatId().equals(chatId))) {
        // telegramConversation.setStatus(TelegramStatus.IDLE);
        // telegramConversationRepository.save(telegramConversation);
        // }
        // }
        sendMessageToEditedMessage(chatId, messageId,
                String.format(
                        "Modo de matcheo del filebot: %s ",
                        filebotNameSelection.isForceStrict() ? "Strict" : "Opportunistic"));
        sendMessageToForceQuery(chatId);
        filebotNameSelection.setStatus(FilebotNameStatus.PROCESSING_QUERY);
        telegramFilebotExecutionRepository.save(filebotNameSelection);
    }

    private void handleProcessingLabel(TelegramFilebotExecution filebotNameSelection, String response, String chatId,
            String messageId) {
        log.info("Handle processing label");
        filebotNameSelection.setLabel(response);
        StringBuilder sb = new StringBuilder();

        filebotNameSelection.getFiles().forEach(f -> {
            sb.append("◦ " + f.trim());
            sb.append("\n");
        });
        List<TelegramConversation> telegramConversations = telegramConversationRepository.findAllByStatus(
                TelegramStatus.WAITING_USER_RESPONSE.toString());
        for (TelegramConversation telegramConversation : telegramConversations) {
            if (Boolean.FALSE.equals(telegramConversation.getChatId().equals(chatId))) {
                cleanInlineKeyboard(telegramConversation.getChatId(), telegramConversation.getMessageId());
                telegramConversation.setStatus(TelegramStatus.IDLE);
                telegramConversationRepository.save(telegramConversation);
            }
        }
        sendMessageToEditedMessage(chatId, messageId, String.format(
                "La carpeta es *%s*.\nLos ficheros son:\n*%s*Tipo de contenido seleccionado: *%s*",
                sb.toString(), filebotNameSelection.getPath(), response));
        sendMessageToForceStrict(chatId);
        filebotNameSelection.setStatus(FilebotNameStatus.PROCESSING_FORCE_STRICT);
        telegramFilebotExecutionRepository.save(filebotNameSelection);
    }

    @Override
    public void setFilebotHandler(FilebotHandler filebotHandler) {
        this.filebotHandler = filebotHandler;
    }

    @Override
    public void handleCallbackResponse(String chatId, String messageId, String response) {
        log.info("Handle callback message");
        telegramFilebotExecutionRepository.findByStatusStartsWith("PROCESSING").ifPresent(
                (filebotNameSelection) -> {
                    handleProcessing(filebotNameSelection, response, chatId, messageId);
                });
    }

    @Override
    public void handleIncomingResponse(String chatId, String response) {
        log.info("Handle incomming response with chatId {} ,and  {}", chatId, response);
        List<TelegramConversation> telegramConversations = telegramConversationRepository.findAllByStatus(
                TelegramStatus.WAITING_USER_RESPONSE.toString());
        String messageId = telegramConversations.size() == 1 ? telegramConversations.get(0).getMessageId() : null;
        telegramFilebotExecutionRepository.findByStatusStartsWith("PROCESSING").ifPresent(
                (filebotNameSelection) -> {
                    handleProcessing(filebotNameSelection, response, chatId, messageId);
                });
    }

    private void sendMessageToEditedMessage(String chatId, String messageId, String response) {
        log.info("Send message to chatId {} ,type edited message with id {} and response {}", chatId, messageId,
                response);
        EditMessageText new_message = new EditMessageText();
        new_message.setChatId(chatId);
        new_message.setMessageId(Integer.parseInt(messageId));
        new_message.setText(response);
        filebotHandler.sendEditMessage(new_message);
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

    private String abbreviate(String str, int size) {
        if (str.length() <= size)
            return str;
        int index = str.lastIndexOf(' ', size);
        if (index <= -1)
            return "";
        return str.substring(0, index);
    }

    private TelegramFilebotExecution convertToModel(FilebotExecutionIDTO filebotExecutionIDTO) {
        TelegramFilebotExecution filebotNameSelection = new TelegramFilebotExecution();
        filebotNameSelection.setId(filebotExecutionIDTO.getId());
        filebotNameSelection.setFiles(filebotExecutionIDTO.getFiles());
        filebotNameSelection.setPath(filebotExecutionIDTO.getPath());
        filebotNameSelection.setPossibilities(filebotExecutionIDTO.getPossibilities());
        return filebotNameSelection;
    }

    // private void processNotProcessing(String chatId) {
    // log.info("processNotProcessing EMPTY method...");
    // TelegramConversation telegramConversation =
    // telegramConversationRepository.findByChatId(chatId);
    // if (Boolean.FALSE.equals(telegramFilebotExecutionRepository
    // .findByStatusStartsWith("PROCESSING").isPresent())) {
    // List<TelegramFilebotExecution> filebots = telegramFilebotExecutionRepository
    // .findAllByStatus(FilebotNameStatus.UNPROCESSING.name());
    // if (Boolean.FALSE.equals(filebots.isEmpty())) {
    // TelegramFilebotExecution filebotNameSelection = filebots.get(0);
    // if (filebotNameSelection.getPossibilities().isEmpty()) {
    // handleFilebotWithoutPosibilities(filebotNameSelection, chatId);
    // } else {
    // handleFilebotWithPossibilities(filebotNameSelection, chatId);
    // }
    // telegramConversation.setStatus(TelegramStatus.WAITING_USER_RESPONSE);
    // } else {
    // telegramConversation.setStatus(TelegramStatus.IDLE);
    // }
    // } else {
    // log.info("No filebots to process");
    // sendMessage("En estos momentos no hay ninguna acción más que realizar",
    // chatId);
    // telegramConversation.setStatus(TelegramStatus.IDLE);
    // }
    // telegramConversationRepository.save(telegramConversation);
    // }

    @Override
    public void stopConversation(String chatId) {
        TelegramConversation telegramConversation = telegramConversationRepository.findByChatId(chatId).get();
        telegramConversation.setStatus(TelegramStatus.STOPPED);
        telegramConversationRepository.save(telegramConversation);
        sendMessage("La conversación ha sido detenida", chatId);
    }

    @Override
    public void resetAllStatus() {
        List<TelegramConversation> telegramConversations = telegramConversationRepository.findAll();
        List<TelegramFilebotExecution> telegramFilebotExecutions = telegramFilebotExecutionRepository.findAll();
        for (TelegramFilebotExecution telegramFilebotExecution : telegramFilebotExecutions) {
            telegramFilebotExecution.setStatus(FilebotNameStatus.UNPROCESSING);
            telegramFilebotExecutionRepository.save(telegramFilebotExecution);
        }
        for (TelegramConversation telegramConversation : telegramConversations) {
            telegramConversation.setStatus(TelegramStatus.IDLE);
            telegramConversationRepository.save(telegramConversation);
            sendMessage("La conversación ha sido reiniciada para todos los usuarios", telegramConversation.getChatId());
        }
        processNotProcessing();
    }
}
