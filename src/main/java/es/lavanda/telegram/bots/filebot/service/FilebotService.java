package es.lavanda.telegram.bots.filebot.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
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

import es.lavanda.lib.common.model.FilebotExecutionIDTO;
import es.lavanda.lib.common.model.FilebotExecutionODTO;
import es.lavanda.lib.common.model.QbittorrentModel;
import es.lavanda.lib.common.model.TelegramFilebotExecutionIDTO;
import es.lavanda.lib.common.model.TelegramFilebotExecutionIDTO.Type;
import es.lavanda.lib.common.model.TelegramFilebotExecutionODTO;
import es.lavanda.lib.common.model.tmdb.search.TMDBResultDTO;

import es.lavanda.telegram.bots.common.model.TelegramMessage;
import es.lavanda.telegram.bots.common.model.TelegramMessage.Handler;
import es.lavanda.telegram.bots.common.model.TelegramMessage.MessageType;
import es.lavanda.telegram.bots.common.service.MessageMapper;
import es.lavanda.telegram.bots.common.service.ProducerService;
import es.lavanda.telegram.bots.filebot.handler.FilebotHandler;
import es.lavanda.telegram.bots.filebot.model.Elected;
import es.lavanda.telegram.bots.filebot.model.FilebotConversation;
import es.lavanda.telegram.bots.filebot.model.FilebotConversation.FilebotConversationStatus;
import es.lavanda.telegram.bots.filebot.model.FilebotExecution;
import es.lavanda.telegram.bots.filebot.model.FilebotExecution.FilebotNameStatus;
import es.lavanda.telegram.bots.filebot.repository.FilebotConversationRepository;
import es.lavanda.telegram.bots.filebot.repository.FilebotExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FilebotService {

    private FilebotHandler filebotHandler;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private FilebotExecutionRepository filebotExecutionRepository;

    @Autowired
    private FilebotConversationRepository filebotConversationRepository;

    // @Autowired
    // private TelegramFilebotexecutionDeleteFolderRepository
    // telegramFilebotexecutionDeleteFolderRepository;

    @Autowired
    private ProducerService producerService;

    private Set<Elected> electeds = new HashSet<>(3);

    public void run(FilebotExecutionIDTO filebotExecutionIDTO) {
        FilebotExecution filebotExecution = convertToModel(filebotExecutionIDTO);
        filebotExecution.setStatus(FilebotNameStatus.UNPROCESSING);
        filebotExecutionRepository.save(filebotExecution);
        processNotProcessing(filebotExecution);
        // producerService.sendTelegramExecution(telegramFilebotExecution);
    }

    public void newConversation(String chatId, String name) {
        filebotConversationRepository.findByChatId(chatId).ifPresentOrElse((filebotConversation) -> {
            filebotConversation.setStatus(FilebotConversationStatus.IDLE);
            filebotConversationRepository.save(filebotConversation);
            createSendMessageAndSendToRabbit("Reiniciado...", chatId, false);
        }, () -> {
            FilebotConversation filebotConversation = new FilebotConversation();
            filebotConversation.setChatId(chatId);
            filebotConversation.setName(name);
            filebotConversation.setStatus(FilebotConversationStatus.IDLE);
            filebotConversationRepository.save(filebotConversation);
            processNotProcessing();
        });
    }

    public void setFilebotHandler(FilebotHandler filebotHandler) {
        this.filebotHandler = filebotHandler;
    }

    public void handleCallbackResponse(String chatId, String messageId, String response) {
        log.info("Handle callback message");
        filebotExecutionRepository.findByStatusStartsWith("PROCESSING").ifPresent(
                (filebotNameSelection) -> {
                    handleProcessing(filebotNameSelection, response, chatId, messageId);
                });
    }

    public void handleIncomingResponse(String chatId, String response) {
        log.info("Handle incomming response with chatId {} ,and  {}", chatId, response);
        List<FilebotConversation> telegramConversations = filebotConversationRepository.findAllByStatus(
                FilebotConversationStatus.WAITING_USER_RESPONSE.toString());
        String messageId = telegramConversations.size() == 1 ? telegramConversations.get(0).getInlineKeyboardMessageId()
                : null;
        filebotExecutionRepository.findByStatusStartsWith("PROCESSING").ifPresent(
                (filebotNameSelection) -> {
                    handleProcessing(filebotNameSelection, response, chatId, messageId);
                });
    }

    public void processNotProcessing(FilebotExecution telegramFilebotExecution) {
        log.info("processNotProcessing whith model method...");
        if (Boolean.FALSE.equals(filebotExecutionRepository
                .findByStatusStartsWith("PROCESSING").isPresent())) {
            List<FilebotConversation> telegramConversations = filebotConversationRepository
                    .findAllByStatus(FilebotConversationStatus.IDLE.toString());
            for (FilebotConversation telegramConversation : telegramConversations) {
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
                filebotExecutionRepository.save(telegramFilebotExecution);
                telegramConversation.setStatus(FilebotConversationStatus.WAITING_USER_RESPONSE);
                telegramConversation.setInlineKeyboardMessageId(messageId);
                log.info("Saving telegram conversation with status {} and messageId {}", "WAITING_USER_RESPONSE",
                        messageId);
                filebotConversationRepository.save(telegramConversation);
            }
        }
    }

    public void processNotProcessing() {
        log.info("processNotProcessing with list of chatIds...");
        if (Boolean.FALSE.equals(filebotExecutionRepository
                .findByStatusStartsWith("PROCESSING").isPresent())) {
            List<FilebotExecution> filebots = filebotExecutionRepository
                    .findAllByStatus(FilebotNameStatus.UNPROCESSING.name());
            if (Boolean.FALSE.equals(filebots.isEmpty())) {
                processNotProcessing(filebots.get(0));
            }
        }
    }

    private String sendMessageWithPossibilities(FilebotExecution telegramFilebotExecution, String chatId) {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(chatId);
        sendMessageRequest
                .setText(String.format("La carpeta es %s.\nLos ficheros son: %s\nSelecciona el posible resultado:",
                        telegramFilebotExecution.getPath(), telegramFilebotExecution.getFiles()));
        sendMessageRequest.setReplyMarkup(getReplyKeyboardMarkup(telegramFilebotExecution.getFiles()));
        // sendMessageRequest.setReplyMarkup(getInlineKeyboard(filebotNameSelection.getFiles()));
        return filebotHandler.sendMessage(sendMessageRequest);
    }

    private String sendMessageToSelectLabel(FilebotExecution filebotNameSelection, String chatId) {
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
                        "La carpeta es: \n*%s*\nLos ficheros son:\n*%s*Selecciona el tipo de contenido o algun anterior:",
                        filebotNameSelection.getPath(), abbreviate(sb.toString(), 400)));
        sendMessageRequest.setReplyMarkup(
                getInlineKeyboard(List.of("Serie", "Pelicula"), List.of("TV", "MOVIE"), getElectedName(),
                        getElectedTmdbId()));
        return filebotHandler.sendMessage(sendMessageRequest);
    }

    private Elected getElected(int tmdbId) {
        return electeds.stream().filter(e -> e.getTmdbId() == tmdbId).findFirst().orElse(null);
    }

    private List<String> getElectedName() {
        return electeds.stream().map(Elected::getName).collect(Collectors.toList());
    }

    private List<String> getElectedTmdbId() {
        return electeds.stream().map(e -> String.valueOf(e.getTmdbId())).collect(Collectors.toList());
    }

    private void addNewElected(String name, int tmdbId, String releaseDate) {
        boolean found = false;
        for (Elected elected : electeds) {
            if (elected.getName().equals(name) && elected.getTmdbId() == tmdbId) {
                // Increment the number of times this candidate has been elected
                elected.setTimes(elected.getTimes() + 1);
                found = true;
            } else {
                // Decrement the number of times the other candidates have been elected
                elected.setTimes(elected.getTimes() - 1);
                if (elected.getTimes() == 0) {
                    // If the number of times for this candidate has reached 0, remove it from the
                    // list
                    electeds.remove(elected);
                }
            }
        }
        if (!found) {
            // If the elected candidate is not in the list, add them with a times value of 1
            electeds.add(new Elected(1, name, releaseDate, tmdbId));
        }
    }

    private String sendMessageToForceStrict(String chatId) {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(chatId);
        sendMessageRequest.setText(
                "¿Modo de matcheo del filebot?");
        sendMessageRequest.setReplyMarkup(getInlineKeyboard(List.of("Strict", "Opportunistic")));
        return filebotHandler.sendMessage(sendMessageRequest);
    }

    private String sendMessageToForceQueryWithOptions(String chatId, Map<String, TMDBResultDTO> results,
            boolean isShow) {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(chatId);
        // List<String> options = new arrayList<>();
        // options.add("Ninguno de los anteriores");
        // options.addAll();
        InlineKeyboardMarkup inlineKeyboardMarkup = getInlineKeyboardForChoices(results, isShow);
        sendMessageRequest.setText(
                "¿Cual crees que es?:");
        sendMessageRequest.setReplyMarkup(inlineKeyboardMarkup);
        return filebotHandler.sendMessage(sendMessageRequest);
    }

    private InlineKeyboardMarkup getInlineKeyboardForChoices(Map<String, TMDBResultDTO> results, boolean isShow) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        for (Entry<String, TMDBResultDTO> result : results.entrySet()) {
            log.info("Adding show {}", result.getValue().getTitle());
            List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();
            InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
            if (isShow) {
                inlineKeyboardButton
                        .setText(result.getValue().getName() + " (" + result.getValue().getFirstAirDate() + ")");
            } else {
                inlineKeyboardButton
                        .setText(result.getValue().getTitle() + " (" + result.getValue().getReleaseDate() + ")");
            }
            inlineKeyboardButton.setCallbackData(result.getKey());
            InlineKeyboardButton moreData = new InlineKeyboardButton();
            moreData.setText("ℹ️");
            moreData.setCallbackData("data" + result.getKey());
            keyboardButtonsRow1.add(inlineKeyboardButton);
            keyboardButtonsRow1.add(moreData);
            rowsInline.add(keyboardButtonsRow1);
        }
        List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
        inlineKeyboardButton.setText("Ninguno de los anteriores");
        inlineKeyboardButton.setCallbackData("0");
        keyboardButtonsRow1.add(inlineKeyboardButton);
        rowsInline.add(keyboardButtonsRow1);
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

    // private String sendMessage(TelegramSendm) {
    // SendMessage sendMessageRequest = new SendMessage();
    // sendMessageRequest.setChatId(chatId);
    // sendMessageRequest.setText(abbreviate(text, 3000));
    // sendMessageRequest.setReplyMarkup(getKeyboardRemove());
    // return filebotHandler.sendMessage(sendMessageRequest);
    // }

    private void createSendMessageAndSendToRabbit(String text, String chatId, boolean saveOnDatabase) {
        TelegramMessage sendMessageRequest = new TelegramMessage();
        sendMessageRequest.setChatId(chatId);
        sendMessageRequest.setText(abbreviate(text, 3000));
        sendMessageRequest.setReplyMarkup(getKeyboardRemove());
        sendMessageRequest.setSaveOnDatabase(saveOnDatabase);
        sendMessageRequest.setType(MessageType.TEXT);
        sendMessageRequest.setHandler(Handler.FILEBOT);
        producerService.sendTelegramMessage(sendMessageRequest);
    }

    private void createSendPhotoAndSendToRabbit(String overview, String photo, String chatId, boolean saveOnDatabase) {
        TelegramMessage sendPhoto = new TelegramMessage();
        sendPhoto.setType(MessageType.PHOTO);
        sendPhoto.setCaption((abbreviate(overview, 3000)));
        sendPhoto.setSaveOnDatabase(saveOnDatabase);
        InputFile inputFile = new InputFile();
        inputFile.setMedia(photo);
        sendPhoto.setPhoto(inputFile);
        sendPhoto.setChatId(chatId);
        sendPhoto.setHandler(Handler.FILEBOT);
        producerService.sendTelegramMessage(sendPhoto);
    }

    private ReplyKeyboard getKeyboardRemove() {
        ReplyKeyboardRemove replyKeyboardRemove = new ReplyKeyboardRemove();
        replyKeyboardRemove.setSelective(true);
        replyKeyboardRemove.setRemoveKeyboard(true);
        return replyKeyboardRemove;
    }

    private FilebotExecutionODTO getFilebotExecutionODTO(FilebotExecution filebotNameSelection) {
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

    private InlineKeyboardMarkup getInlineKeyboard(List<String> data, List<String> callbackData, List<String> data2,
            List<String> callbackData2) {
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
        List<InlineKeyboardButton> rowInline2 = new ArrayList<>();
        for (int i = 0; i < data2.size(); i++) {
            InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
            inlineKeyboardButton.setText(data2.get(i));
            inlineKeyboardButton.setCallbackData(callbackData2.get(i));
            rowInline.add(inlineKeyboardButton);
        }
        rowsInline.add(rowInline2);
        inlineKeyboardMarkup.setKeyboard(rowsInline);
        return inlineKeyboardMarkup;
    }

    private void handleProcessing(FilebotExecution telegramFilebotExecution, String response, String chatId,
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

    private void handleProcessingWithPossibilities(FilebotExecution filebotNameSelection, String response,
            String chatId, String messageId) {
        log.info("Handle processing with possibilities");
    }

    private void handleProcessingQuery(FilebotExecution telegramFilebotExecution, String response,
            String chatId,
            String messageId) {
        log.info("Handle processing query");
        FilebotConversation telegramConversation = filebotConversationRepository.findByChatId(chatId).get();
        if (Boolean.TRUE.equals(response.equalsIgnoreCase("0"))) {
            telegramFilebotExecution.setQuery(null);
            log.info("NINGUNO DE LOS ANTERIORES");
            telegramFilebotExecution.setStatus(FilebotNameStatus.PROCESSED);
            filebotExecutionRepository.save(telegramFilebotExecution);
            producerService.sendFilebotExecution(
                    getFilebotExecutionODTO(telegramFilebotExecution));
            createEditMessageAndSendToRabbit(chatId, messageId,
                    String.format(" No se ha seleccionado ninguna opción"));
            for (String messageToDelete : telegramConversation.getOtherMessageIds()) {
                createDeleteMessageAndSendToRabbit(chatId, messageToDelete);
            }
            createSendMessageAndSendToRabbit("Procesado correctamente", chatId, false);
            log.info("Processed telegramFilebotExecutionId: " +
                    telegramFilebotExecution.getPath());
            telegramConversation.setStatus(
                    FilebotConversationStatus.IDLE);
            telegramConversation.setOtherMessageIds(new ArrayList<>());
            filebotConversationRepository.save(telegramConversation);
            processNotProcessing();
        } else if (Boolean.TRUE.equals(response.startsWith("data"))) {
            log.info("Mas datos de la pelicula");
            String idTMDB = response.split("data")[1];
            TMDBResultDTO resultToMoreData = telegramFilebotExecution.getPossibleChoicesTMDB().get(idTMDB);
            if (Objects.isNull(resultToMoreData.getPosterPath())) {
                createSendMessageAndSendToRabbit(resultToMoreData.getOverview(), chatId, true);
            } else {
                createSendPhotoAndSendToRabbit(resultToMoreData.getOverview(),
                        "https://image.tmdb.org/t/p/w500" + resultToMoreData.getPosterPath(), chatId, true);
            }
        } else {
            telegramFilebotExecution.setQuery(response);
            telegramFilebotExecution.setStatus(FilebotNameStatus.PROCESSED);
            filebotExecutionRepository.save(telegramFilebotExecution);
            producerService.sendFilebotExecution(
                    getFilebotExecutionODTO(telegramFilebotExecution));
            String name = Optional.ofNullable(
                    telegramFilebotExecution.getPossibleChoicesTMDB().get(response).getTitle())
                    .orElse(telegramFilebotExecution.getPossibleChoicesTMDB().get(response).getName());
            String releaseDate = Optional
                    .ofNullable(telegramFilebotExecution.getPossibleChoicesTMDB().get(response)
                            .getReleaseDate())
                    .orElse(telegramFilebotExecution.getPossibleChoicesTMDB().get(response)
                            .getFirstAirDate());
            createEditMessageAndSendToRabbit(chatId, messageId,
                    String.format("Seleccionado: %s",
                            name
                                    + " ("
                                    + releaseDate
                                    + ")"));
            for (String messageToDelete : telegramConversation.getOtherMessageIds()) {
                createDeleteMessageAndSendToRabbit(chatId, messageToDelete);
            }
            // createSendMessageAndSendToRabbit("Procesado correctamente", chatId, false);
            log.info("Processed telegramFilebotExecutionId: " +
                    telegramFilebotExecution.getPath());
            telegramConversation.setStatus(
                    FilebotConversationStatus.IDLE);
            addNewElected(name, Integer.parseInt(response), releaseDate);
            telegramConversation.setOtherMessageIds(new ArrayList<>());
            filebotConversationRepository.save(telegramConversation);
            processNotProcessing();
        }
    }

    private void handleProcessingForceStrict(FilebotExecution telegramFilebotExecution, String response,
            String chatId,
            String messageId) {
        log.info("Handle processing force strict");
        telegramFilebotExecution.setForceStrict(response.equalsIgnoreCase("Strict"));
        // List<TelegramConversation> telegramConversations =
        // filebotConversationRepository.findAllByStatus(
        // FilebotConversationStatus.WAITING_USER_RESPONSE.toString());
        // for (TelegramConversation telegramConversation : telegramConversations) {
        // if (Boolean.FALSE.equals(telegramConversation.getChatId().equals(chatId))) {
        // telegramConversation.setStatus(FilebotConversationStatus.IDLE);
        // filebotConversationRepository.save(telegramConversation);
        // }
        // }
        createEditMessageAndSendToRabbit(chatId, messageId,
                String.format(
                        "Modo de matcheo del filebot: %s ",
                        telegramFilebotExecution.isForceStrict() ? "Strict" : "Opportunistic"));
        sendToTMDBMicroservice(telegramFilebotExecution);
        // sendMessageToForceQuery(chatId);
        FilebotConversation telegramConversation = filebotConversationRepository.findByChatId(chatId).get();
        telegramConversation.setStatus(FilebotConversationStatus.WAITING_TMDB_RESPONSE);
        filebotConversationRepository.save(telegramConversation);
        telegramFilebotExecution.setStatus(FilebotNameStatus.PROCESSING_TMDB_RESPONSE);
        filebotExecutionRepository.save(telegramFilebotExecution);
    }

    private void handleProcessingLabel(FilebotExecution telegramFilebotExecution, String response,
            String chatId,
            String messageId) {
        log.info("Handle processing label: {}", response);
        FilebotConversation telegramConversation = filebotConversationRepository.findByChatId(chatId).get();

        try {
            int tmdbId = Integer.parseInt(response);
            telegramFilebotExecution.setQuery(response);
            telegramFilebotExecution.setStatus(FilebotNameStatus.PROCESSED);
            filebotExecutionRepository.save(telegramFilebotExecution);
            producerService.sendFilebotExecution(
                    getFilebotExecutionODTO(telegramFilebotExecution));
            Elected elected = getElected(tmdbId);
            createEditMessageAndSendToRabbit(chatId, messageId,
                    String.format("Seleccionado de nuevo: %s",
                            elected.getName()
                                    + " ("
                                    + elected.getReleaseDate()
                                    + ")"));
            for (String messageToDelete : telegramConversation.getOtherMessageIds()) {
                createDeleteMessageAndSendToRabbit(chatId, messageToDelete);
            }
            // createSendMessageAndSendToRabbit("Procesado correctamente", chatId, false);
            log.info("Processed telegramFilebotExecutionId: " +
                    telegramFilebotExecution.getPath());
            telegramConversation.setStatus(
                    FilebotConversationStatus.IDLE);
            telegramConversation.setOtherMessageIds(new ArrayList<>());
            filebotConversationRepository.save(telegramConversation);
            addNewElected(null, tmdbId, null);
            processNotProcessing();

        } catch (NumberFormatException e) {
            telegramFilebotExecution.setLabel(response);
            StringBuilder sb = new StringBuilder();

            telegramFilebotExecution.getFiles().forEach(f -> {
                sb.append("◦ " + f.trim());
                sb.append("\n");
            });
            List<FilebotConversation> telegramConversations = filebotConversationRepository.findAllByStatus(
                    FilebotConversationStatus.WAITING_USER_RESPONSE.toString());
            for (FilebotConversation telegramConversation2 : telegramConversations) {
                if (Boolean.FALSE.equals(telegramConversation2.getChatId().equals(chatId))) {
                    createDeleteMessageAndSendToRabbit(telegramConversation2.getChatId(),
                            telegramConversation2.getInlineKeyboardMessageId());
                    telegramConversation2.setStatus(FilebotConversationStatus.IDLE);
                    filebotConversationRepository.save(telegramConversation2);
                }
            }
            createEditMessageAndSendToRabbit(chatId, messageId, String.format(
                    "La carpeta es *%s*.\nLos ficheros son:\n*%s*\nTipo de contenido seleccionado: *%s*",
                    sb.toString(), telegramFilebotExecution.getPath(), response));
            sendMessageToForceStrict(chatId);
            telegramFilebotExecution.setStatus(FilebotNameStatus.PROCESSING_FORCE_STRICT);
            filebotExecutionRepository.save(telegramFilebotExecution);
        }
    }

    private void createEditMessageAndSendToRabbit(String chatId, String messageId, String response) {
        log.info("Send message to chatId {} ,type edited message with id {} and response {}", chatId, messageId,
                response);
        TelegramMessage newMessage = new TelegramMessage();
        newMessage.setChatId(chatId);
        newMessage.setMessageId(Integer.parseInt(messageId));
        newMessage.setText(abbreviate(response, 3000));
        newMessage.setType(MessageType.EDIT_MESSAGE);
        newMessage.setHandler(Handler.FILEBOT);
        producerService.sendTelegramMessage(newMessage);

    }

    private void createDeleteMessageAndSendToRabbit(String chatId, String messageId) {
        TelegramMessage deleteMessage = new TelegramMessage();
        deleteMessage.setChatId(chatId);
        deleteMessage.setMessageId(Integer.parseInt(messageId));
        deleteMessage.setType(MessageType.DELETE_MESSAGE);
        deleteMessage.setHandler(Handler.FILEBOT);
        producerService.sendTelegramMessage(deleteMessage);
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

    private FilebotExecution convertToModel(FilebotExecutionIDTO filebotExecutionIDTO) {
        FilebotExecution filebotNameSelection = new FilebotExecution();
        filebotNameSelection.setId(filebotExecutionIDTO.getId());
        filebotNameSelection.setFiles(filebotExecutionIDTO.getFiles());
        filebotNameSelection.setPath(filebotExecutionIDTO.getPath());
        filebotNameSelection.setPossibilities(filebotExecutionIDTO.getPossibilities());
        return filebotNameSelection;
    }

    // private void processNotProcessing(String chatId) {
    // log.info("processNotProcessing EMPTY method...");
    // TelegramConversation telegramConversation =
    // filebotConversationRepository.findByChatId(chatId);
    // if (Boolean.FALSE.equals(filebotExecutionRepository
    // .findByStatusStartsWith("PROCESSING").isPresent())) {
    // List<TelegramFilebotExecution> filebots = filebotExecutionRepository
    // .findAllByStatus(FilebotNameStatus.UNPROCESSING.name());
    // if (Boolean.FALSE.equals(filebots.isEmpty())) {
    // TelegramFilebotExecution filebotNameSelection = filebots.get(0);
    // if (filebotNameSelection.getPossibilities().isEmpty()) {
    // handleFilebotWithoutPosibilities(filebotNameSelection, chatId);
    // } else {
    // handleFilebotWithPossibilities(filebotNameSelection, chatId);
    // }
    // telegramConversation.setStatus(FilebotConversationStatus.WAITING_USER_RESPONSE);
    // } else {
    // telegramConversation.setStatus(FilebotConversationStatus.IDLE);
    // }
    // } else {
    // log.info("No filebots to process");
    // sendMessage("En estos momentos no hay ninguna acción más que realizar",
    // chatId);
    // telegramConversation.setStatus(FilebotConversationStatus.IDLE);
    // }
    // filebotConversationRepository.save(telegramConversation);
    // }

    public void stopConversation(String chatId) {
        FilebotConversation telegramConversation = filebotConversationRepository.findByChatId(chatId).get();
        telegramConversation.setStatus(FilebotConversationStatus.STOPPED);
        filebotConversationRepository.save(telegramConversation);
        createSendMessageAndSendToRabbit("La conversación ha sido detenida", chatId, false);
    }

    public void resetAllStatus() {
        List<FilebotConversation> telegramConversations = filebotConversationRepository.findAll();
        List<FilebotExecution> telegramFilebotExecutions = filebotExecutionRepository.findAll();
        for (FilebotExecution telegramFilebotExecution : telegramFilebotExecutions) {
            if (telegramFilebotExecution
                    .getStatus().toString().startsWith("PROCESSING")) {
                telegramFilebotExecution.setStatus(FilebotNameStatus.UNPROCESSING);
                filebotExecutionRepository.save(telegramFilebotExecution);
            }
        }
        for (FilebotConversation telegramConversation : telegramConversations) {
            if (Boolean.FALSE.equals(telegramConversation.getStatus().equals(FilebotConversationStatus.STOPPED))) {
                telegramConversation.setStatus(FilebotConversationStatus.IDLE);
                filebotConversationRepository.save(telegramConversation);
                createSendMessageAndSendToRabbit("La conversación ha sido reiniciada para todos los usuarios activos",
                        telegramConversation.getChatId(), false);
            }
        }
        processNotProcessing();
    }

    private void sendToTMDBMicroservice(FilebotExecution telegramFilebotExecution) {
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

    public void recieveTMDBData(TelegramFilebotExecutionODTO telegramFilebotExecutionODTO) {
        log.info("Recieved TMDB data {}", telegramFilebotExecutionODTO);
        Optional<FilebotExecution> optTelegramFilebotExecution = filebotExecutionRepository
                .findById(telegramFilebotExecutionODTO.getId());
        List<FilebotConversation> optTelegramConversation = filebotConversationRepository
                .findAllByStatus(FilebotConversationStatus.WAITING_TMDB_RESPONSE.toString());
        if (optTelegramFilebotExecution.isPresent()
                && optTelegramFilebotExecution.get().getStatus().equals(FilebotNameStatus.PROCESSING_TMDB_RESPONSE)
                && optTelegramConversation.size() == 1) {
            log.info("All recieve tmdb data is correct and will go to force query");
            String messageId = sendMessageToForceQueryWithOptions(optTelegramConversation.get(0).getChatId(),
                    telegramFilebotExecutionODTO.getPossibleChoices(),
                    optTelegramFilebotExecution.get().getLabel().equals("TV") ? true : false);
            optTelegramConversation.get(0).setInlineKeyboardMessageId(messageId);
            optTelegramConversation.get(0).setStatus(FilebotConversationStatus.WAITING_USER_RESPONSE);
            filebotConversationRepository.save(optTelegramConversation.get(0));
            optTelegramFilebotExecution.get().setPossibleChoicesTMDB(telegramFilebotExecutionODTO.getPossibleChoices());
            optTelegramFilebotExecution.get().setStatus(FilebotNameStatus.PROCESSING_QUERY);
            filebotExecutionRepository.save(optTelegramFilebotExecution.get());
            // getChoicesOrdered(telegramFilebotExecutionODTO.getPossibleChoices()));
        } else {
            log.error("Not found telegram filebot execution with id: " + telegramFilebotExecutionODTO.getId());
        }
    }

    private void sendMessageTelegramMessage(TelegramMessage message) {
        if (StringUtils.hasText(message.getText())) {
            SendMessage toSend = messageMapper.messageToSendMessage(message);
            if (Boolean.TRUE.equals(message.isSaveOnDatabase())) {
                FilebotConversation telegramConversation = filebotConversationRepository
                        .findByChatId(message.getChatId())
                        .get();
                if (Objects.nonNull(telegramConversation)) {
                    String messageId = filebotHandler.sendMessage(toSend);
                    telegramConversation.getOtherMessageIds().add(messageId);
                    filebotConversationRepository.save(telegramConversation);
                }
            } else {
                filebotHandler.sendMessage(toSend);
            }
        }
    }

    private void sendPhotoTelegramMessage(TelegramMessage message) {
        SendPhoto toSend = messageMapper.messageToSendPhoto(message);
        if (Boolean.TRUE.equals(message.isSaveOnDatabase())) {
            FilebotConversation telegramConversation = filebotConversationRepository.findByChatId(message.getChatId())
                    .get();
            if (Objects.nonNull(telegramConversation)) {
                String messageId = filebotHandler.sendPhoto(toSend);
                telegramConversation.getOtherMessageIds().add(messageId);
                filebotConversationRepository.save(telegramConversation);
            }
        } else {
            filebotHandler.sendPhoto(toSend);
        }
    }

    private void sendEditMessageTelegramMessage(TelegramMessage message) {
        EditMessageText toSend = messageMapper.messageToEditMessage(message);

        filebotHandler.sendEditMessage(toSend);
    }

    private void sendDeleteMessageTelegramMessage(TelegramMessage message) {
        DeleteMessage toSend = messageMapper.messageToDeleteMessage(message);
        filebotHandler.deleteMessage(toSend);
    }

    public void sendMessage(TelegramMessage message) {
        switch (message.getType()) {
            case TEXT:
                sendMessageTelegramMessage(message);
                break;
            case PHOTO:
                sendPhotoTelegramMessage(message);
                break;
            case EDIT_MESSAGE:
                sendEditMessageTelegramMessage(message);
                break;
            case DELETE_MESSAGE:
                sendDeleteMessageTelegramMessage(message);
                break;
            default:
                break;
        }
    }

    // @Override
    // public void askToDelete(FilebotTelegramDeleteFolder
    // filebotTelegramDeleteFolder) {
    // List<TelegramConversation> telegramConversations =
    // filebotConversationRepository
    // .findAllByStatus(FilebotConversationStatus.IDLE.toString());
    // TelegramFilebotExecutionDeleteFolder telegramFilebotExecutionDeleteFolder =
    // new TelegramFilebotExecutionDeleteFolder();
    // telegramFilebotExecutionDeleteFolder.setFiles(filebotTelegramDeleteFolder.getFiles());
    // telegramFilebotExecutionDeleteFolder.setName(filebotTelegramDeleteFolder.getName());
    // telegramFilebotExecutionDeleteFolder.setStatus(TelegramFilebotExecutionDeleteFolderStatus.UNPROCESSING);
    // telegramFilebotexecutionDeleteFolderRepository.save(telegramFilebotExecutionDeleteFolder);
    // for (TelegramConversation telegramConversation : telegramConversations) {
    // String messageId = null;
    // telegramFilebotExecution.setStatus(FilebotNameStatus.PROCESSING_LABEL);
    // }
    // filebotExecutionRepository.save(telegramFilebotExecution);
    // telegramConversation.setStatus(FilebotConversationStatus.WAITING_USER_RESPONSE);
    // telegramConversation.setInlineKeyboardMessageId(messageId);
    // log.info("Saving telegram conversation with status {} and messageId {}",
    // "WAITING_USER_RESPONSE",
    // messageId);
    // filebotConversationRepository.save(telegramConversation);

    // sendMessageToDeleteFolder(filebotTelegramDeleteFolder.getName(),
    // filebotTelegramDeleteFolder.getFiles(),
    // filebotTelegramDeleteFolder.getChatId());

    // }

    // private String sendMessageToDeleteFolder(String name, List<String> files,
    // String chatId) {
    // log.info("Send message to select label to chatid: {}", chatId);
    // SendMessage sendMessageRequest = new SendMessage();
    // sendMessageRequest.setChatId(chatId);
    // StringBuilder sb = new StringBuilder();
    // files.forEach(f -> {
    // sb.append("◦ " + f.trim());
    // sb.append("\n");
    // });
    // sendMessageRequest
    // .setText(String.format(
    // "La carpeta es: \n*%s*\nLos ficheros son:\n*%s*¿Eliminar?:",
    // name, abbreviate(sb.toString(), 400)));
    // sendMessageRequest.setReplyMarkup(
    // getInlineKeyboard(List.of("SI", "NO")));
    // return filebotHandler.sendMessage(sendMessageRequest);
    // }

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
