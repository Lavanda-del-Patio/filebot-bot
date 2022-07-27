package es.lavanda.filebot.bot.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
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
import es.lavanda.lib.common.model.TelegramFilebotExecutionIDTO;
import es.lavanda.lib.common.model.TelegramFilebotExecutionODTO;
import es.lavanda.lib.common.model.TelegramFilebotExecutionIDTO.Type;
import es.lavanda.lib.common.model.tmdb.search.TMDBResultDTO;
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
        TelegramFilebotExecution telegramFilebotExecution = convertToModel(filebotExecutionIDTO);
        telegramFilebotExecution.setStatus(FilebotNameStatus.UNPROCESSING);
        telegramFilebotExecutionRepository.save(telegramFilebotExecution);
        processNotProcessing(telegramFilebotExecution);
        // producerService.sendTelegramExecution(telegramFilebotExecution);
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
                telegramConversation.setInlineKeyboardMessageId(messageId);
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

    private String sendMessageToForceQueryWithOptions(String chatId, Map<String, TMDBResultDTO> results) {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(chatId);
        // List<String> options = new arrayList<>();
        // options.add("Ninguno de los anteriores");
        // options.addAll();
        InlineKeyboardMarkup inlineKeyboardMarkup = getInlineKeyboardForChoices(results);
        sendMessageRequest.setText(
                "¿Cual crees que es?:");
        sendMessageRequest.setReplyMarkup(inlineKeyboardMarkup);
        return filebotHandler.sendMessage(sendMessageRequest);
    }

    private InlineKeyboardMarkup getInlineKeyboardForChoices(Map<String, TMDBResultDTO> results) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        // List<InlineKeyboardButton> rowInline = new ArrayList<>();
        // List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();
        // List<InlineKeyboardButton> keyboardButtonsRow2 = new ArrayList<>();
        for (Entry<String, TMDBResultDTO> result : results.entrySet()) {
            log.info("Adding film {}", result.getValue().getTitle());
            List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();
            InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
            inlineKeyboardButton
                    .setText(result.getValue().getTitle() + " (" + result.getValue().getReleaseDate() + ")");
            inlineKeyboardButton.setCallbackData(result.getKey());
            InlineKeyboardButton moreData = new InlineKeyboardButton();
            moreData.setText("Mas datos...");
            moreData.setCallbackData("data" + result.getKey());
            keyboardButtonsRow1.add(inlineKeyboardButton);
            keyboardButtonsRow1.add(moreData);
            rowsInline.add(keyboardButtonsRow1);
        }
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
        inlineKeyboardButton
                .setText("Ninguno de los anteriores");
        inlineKeyboardButton.setCallbackData("0");
        inlineKeyboardMarkup.setKeyboard(rowsInline);
        return inlineKeyboardMarkup;
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

    private String sendPhoto(String overview, String photo, String chatId) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setCaption(overview);
        InputFile inputFile = new InputFile();
        inputFile.setMedia(photo);
        sendPhoto.setPhoto(inputFile);
        sendPhoto.setChatId(chatId);
        return filebotHandler.sendPhoto(sendPhoto);
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
        TelegramConversation telegramConversation = telegramConversationRepository.findByChatId(chatId).get();
        if (Boolean.TRUE.equals(response.equalsIgnoreCase("0"))) {
            telegramFilebotExecution.setQuery(null);
            log.info("NINGUNO DE LOS ANTERIORES");
        } else if (Boolean.TRUE.equals(response.startsWith("data"))) {
            log.info("Mas datos de la pelicula");
            String idTMDB = response.split("data")[1];
            TMDBResultDTO resultToMoreData = telegramFilebotExecution.getPossibleChoicesTMDB().get(idTMDB);
            String mesageId = sendPhoto(resultToMoreData.getOverview(),
                    "https://image.tmdb.org/t/p/w500" + resultToMoreData.getPosterPath(), chatId);
            telegramConversation.getOtherMessageIds().add(mesageId);
            telegramConversationRepository.save(telegramConversation);
        } else {
            telegramFilebotExecution.setQuery(response);
            telegramFilebotExecution.setStatus(FilebotNameStatus.PROCESSED);
            telegramFilebotExecutionRepository.save(telegramFilebotExecution);
            producerService.sendFilebotExecution(
                    getFilebotExecutionODTO(telegramFilebotExecution));
            sendMessageToEditedMessage(chatId, messageId,
                    String.format("Seleccionado: %s",
                            telegramFilebotExecution.getPossibleChoicesTMDB().get(response).getTitle() + " ("
                                    + telegramFilebotExecution.getPossibleChoicesTMDB().get(response).getReleaseDate()
                                    + ")"));
            for (String messageToDelete : telegramConversation.getOtherMessageIds()) {
                deleteMessage(chatId, messageToDelete);
            }
            sendMessage("Procesado correctamente", chatId);
            log.info("Processed telegramFilebotExecutionId: " +
                    telegramFilebotExecution.getPath());
            telegramConversation.setStatus(
                    TelegramStatus.IDLE);
            telegramConversationRepository.save(telegramConversation);
            processNotProcessing();
        }
    }

    private void handleProcessingForceStrict(TelegramFilebotExecution telegramFilebotExecution, String response,
            String chatId,
            String messageId) {
        log.info("Handle processing force strict");
        telegramFilebotExecution.setForceStrict(response.equalsIgnoreCase("Strict"));
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
                        telegramFilebotExecution.isForceStrict() ? "Strict" : "Opportunistic"));
        sendToTMDBMicroservice(telegramFilebotExecution);
        // sendMessageToForceQuery(chatId);
        TelegramConversation telegramConversation = telegramConversationRepository.findByChatId(chatId).get();
        telegramConversation.setStatus(TelegramStatus.WAITING_TMDB_RESPONSE);
        telegramConversationRepository.save(telegramConversation);
        telegramFilebotExecution.setStatus(FilebotNameStatus.PROCESSING_TMDB_RESPONSE);
        telegramFilebotExecutionRepository.save(telegramFilebotExecution);
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
                deleteMessage(telegramConversation.getChatId(), telegramConversation.getInlineKeyboardMessageId());
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
        String messageId = telegramConversations.size() == 1 ? telegramConversations.get(0).getInlineKeyboardMessageId()
                : null;
        telegramFilebotExecutionRepository.findByStatusStartsWith("PROCESSING").ifPresent(
                (filebotNameSelection) -> {
                    handleProcessing(filebotNameSelection, response, chatId, messageId);
                });
    }

    private void sendMessageToEditedMessage(String chatId, String messageId, String response) {
        log.info("Send message to chatId {} ,type edited message with id {} and response {}", chatId, messageId,
                response);
        EditMessageText newMessage = new EditMessageText();
        newMessage.setChatId(chatId);
        newMessage.setMessageId(Integer.parseInt(messageId));
        newMessage.setText(response);
        filebotHandler.sendEditMessage(newMessage);
    }

    private void deleteMessage(String chatId, String messageId) {
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(chatId);
        deleteMessage.setMessageId(Integer.parseInt(messageId));
        filebotHandler.deleteMessage(deleteMessage);
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
            if (telegramFilebotExecution
                    .getStatus().toString().startsWith("PROCESSING")) {
                telegramFilebotExecution.setStatus(FilebotNameStatus.UNPROCESSING);
                telegramFilebotExecutionRepository.save(telegramFilebotExecution);
            }
        }
        for (TelegramConversation telegramConversation : telegramConversations) {
            if (Boolean.FALSE.equals(telegramConversation.getStatus().equals(TelegramStatus.STOPPED))) {
                telegramConversation.setStatus(TelegramStatus.IDLE);
                telegramConversationRepository.save(telegramConversation);
                sendMessage("La conversación ha sido reiniciada para todos los usuarios activos",
                        telegramConversation.getChatId());
            }
        }
        processNotProcessing();
    }

    private void sendToTMDBMicroservice(TelegramFilebotExecution telegramFilebotExecution) {
        TelegramFilebotExecutionIDTO telegramFilebotExecutionIDTO = new TelegramFilebotExecutionIDTO();
        telegramFilebotExecutionIDTO.setId(telegramFilebotExecution.getId());
        telegramFilebotExecutionIDTO.setFile(telegramFilebotExecution.getFiles().get(0)); // TODO:
        telegramFilebotExecutionIDTO.setPath(telegramFilebotExecution.getPath());
        if (telegramFilebotExecution.getLabel().equals("TV")) {
            telegramFilebotExecutionIDTO.setType(Type.SHOW);
        } else {
            telegramFilebotExecutionIDTO.setType(Type.FILM);
        }
        producerService.sendTelegramExecutionForTMDB(telegramFilebotExecutionIDTO);
    }

    @Override
    public void recieveTMDBData(TelegramFilebotExecutionODTO telegramFilebotExecutionODTO) {
        log.info("Recieved TMDB data {}", telegramFilebotExecutionODTO);
        Optional<TelegramFilebotExecution> optTelegramFilebotExecution = telegramFilebotExecutionRepository
                .findById(telegramFilebotExecutionODTO.getId());
        List<TelegramConversation> optTelegramConversation = telegramConversationRepository
                .findAllByStatus(TelegramStatus.WAITING_TMDB_RESPONSE.toString());
        if (optTelegramFilebotExecution.isPresent()
                && optTelegramFilebotExecution.get().getStatus().equals(FilebotNameStatus.PROCESSING_TMDB_RESPONSE)
                && optTelegramConversation.size() == 1) {
            log.info("All recieve tmdb data is correct and will go to force query");
            String messageId = sendMessageToForceQueryWithOptions(optTelegramConversation.get(0).getChatId(),
                    telegramFilebotExecutionODTO.getPossibleChoices());
            optTelegramConversation.get(0).setInlineKeyboardMessageId(messageId);
            optTelegramConversation.get(0).setStatus(TelegramStatus.WAITING_USER_RESPONSE);
            telegramConversationRepository.save(optTelegramConversation.get(0));
            optTelegramFilebotExecution.get().setPossibleChoicesTMDB(telegramFilebotExecutionODTO.getPossibleChoices());
            optTelegramFilebotExecution.get().setStatus(FilebotNameStatus.PROCESSING_QUERY);
            telegramFilebotExecutionRepository.save(optTelegramFilebotExecution.get());
            // getChoicesOrdered(telegramFilebotExecutionODTO.getPossibleChoices()));
        } else {
            log.error("Not found telegram filebot execution with id: " + telegramFilebotExecutionODTO.getId());
        }
    }

    // private List<String> getChoicesOrdered(Map<String, TMDBResultDTO> results) {
    // List<String> choices = new ArrayList<>();
    // for (Entry<String, TMDBResultDTO> result : results.entrySet()) {
    // String choice = String.join(" - ", result.getValue().getTitle(),
    // String.valueOf(result.getValue().getReleaseDate()));
    // choices.add(choice);
    // }
    // return choices;
    // }
}
