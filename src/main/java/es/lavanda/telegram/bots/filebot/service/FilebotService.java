package es.lavanda.telegram.bots.filebot.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;

import es.lavanda.lib.common.model.FilebotExecutionIDTO;
import es.lavanda.lib.common.model.FilebotExecutionODTO;
import es.lavanda.lib.common.model.FilebotExecutionTestIDTO;
import es.lavanda.lib.common.model.FilebotExecutionTestODTO;
import es.lavanda.lib.common.model.TelegramFilebotExecutionODTO;
import es.lavanda.telegram.bots.common.model.TelegramMessage;
import es.lavanda.telegram.bots.common.model.TelegramMessage.MessageHandler;
import es.lavanda.telegram.bots.common.model.TelegramMessage.MessageType;
import es.lavanda.telegram.bots.common.service.ProducerService;
import es.lavanda.telegram.bots.common.service.chainofresponsability.impl.ActionExecutor;
import es.lavanda.telegram.bots.common.service.chainofresponsability.impl.CategoryExecutor;
import es.lavanda.telegram.bots.common.service.chainofresponsability.impl.ChoiceExecutor;
import es.lavanda.telegram.bots.common.service.chainofresponsability.impl.ForceStrictExecutor;
import es.lavanda.telegram.bots.common.service.chainofresponsability.impl.TMDBExecutor;
import es.lavanda.telegram.bots.common.service.chainofresponsability.impl.TestExecutor;
import es.lavanda.telegram.bots.filebot.handler.FilebotHandler;
import es.lavanda.telegram.bots.filebot.model.FilebotConversation;
import es.lavanda.telegram.bots.filebot.model.FilebotExecution;
import es.lavanda.telegram.bots.filebot.model.FilebotExecution.FilebotExecutionStatus;
import es.lavanda.telegram.bots.filebot.utils.TelegramUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FilebotService {
    private ModelMapper modelMapper = new ModelMapper();

    private FilebotHandler filebotHandler;

    private final FilebotConversationService filebotConversationService;

    private final FilebotExecutionService filebotExecutionService;

    private final ProducerService producerService;

    private final CategoryExecutor categoryExecutor;

    private final ActionExecutor actionExecutor;

    private final ForceStrictExecutor forceStrictExecutor;

    private final ChoiceExecutor choiceExecutor;

    private final TMDBExecutor tmdbExecutor;

    private final TestExecutor testExecutor;

    @PostConstruct
    public void postConstruct() {
        categoryExecutor.setNext(forceStrictExecutor);
        forceStrictExecutor.setNext(actionExecutor);
        actionExecutor.setNext(tmdbExecutor);
        tmdbExecutor.setNext(choiceExecutor);
        choiceExecutor.setNext(testExecutor);
    }

    public void run(FilebotExecutionIDTO filebotExecutionIDTO) {
        FilebotExecution filebotExecution = convertToModel(filebotExecutionIDTO);
        filebotExecutionService.save(filebotExecution);
        log.info("Saved New");
        processNotProcessing();
    }

    public void runTest(FilebotExecutionTestIDTO filebotExecutionTestIDTO) {
        FilebotExecution oldFilebotExecution = filebotExecutionService.findById(filebotExecutionTestIDTO.getId());
        FilebotExecution filebotExecutionNew = convertToModel(filebotExecutionTestIDTO);
        oldFilebotExecution.setFiles(filebotExecutionNew.getFiles());
        oldFilebotExecution.setPossibilities(filebotExecutionNew.getPossibilities());
        oldFilebotExecution.setStatus(FilebotExecutionStatus.TEST);
        filebotExecutionService.save(oldFilebotExecution);
        log.info("Saved Test {}", oldFilebotExecution.getName());
        processNotProcessing();
    }

    public void newConversation(String chatId, String name) {
        filebotConversationService.findByChatId(chatId).ifPresentOrElse((filebotConversation) -> {
            createSendMessageAndSendToRabbit(
                    "Iniciado. Quedo a la espera de poder mandarte posibles ficheros a procesar.", chatId,
                    null);
        }, () -> {
            FilebotConversation filebotConversation = new FilebotConversation();
            filebotConversation.setChatId(chatId);
            filebotConversation.setName(name);
            filebotConversationService.save(filebotConversation);
            createSendMessageAndSendToRabbit(
                    "Iniciado. Quedo a la espera de poder mandarte posibles ficheros a procesar.", chatId,
                    null);
        });
        processNotProcessing();
    }

    public void setFilebotHandler(FilebotHandler filebotHandler) {
        this.filebotHandler = filebotHandler;
    }

    public void handleCallbackResponse(String chatId, String messageId, String response) {
        log.info("Handle callback message");
        Optional<FilebotConversation> optFilebotConversation = filebotConversationService
                .findByChatId(chatId);
        if (optFilebotConversation.isEmpty()) {
            log.error("No filebotConversation with chatId {}", chatId);
            return;
        } else {
            FilebotConversation filebotConversation = optFilebotConversation.get();
            log.info("Handle callback message with chatId {} ,and response {}", chatId, response);
            FilebotExecution filebotExecution = filebotExecutionService
                    .getOnCallback();
            if (Objects.isNull(filebotExecution)) {
                log.error("No filebotExecution on callback");
                return;
            } else {
                if (TelegramUtils.BACK_BUTTON_KEY.equals(response)) {
                    log.info("Pressed back button");
                    deletePreviousMessage(filebotConversation);
                    filebotExecution = filebotExecutionService.setPreviousStatus(filebotExecution);
                    categoryExecutor.handleRequest(filebotConversation, filebotExecution, null);
                } else {
                    categoryExecutor.handleRequest(filebotConversation, filebotExecution, response);
                }
                log.info("Finish handle callback message with chatId {} ,and response {}", chatId,
                        response);
                if (FilebotExecutionStatus.PROCESSED
                        .equals(filebotExecution.getStatus())) {
                    log.info("STATUS PROCESSED.");
                    producerService.sendFilebotExecution(modelMapper.map(filebotExecution, FilebotExecutionODTO.class));
                    processNotProcessing();
                } else if (FilebotExecutionStatus.FINISHED
                        .equals(filebotExecution.getStatus())) {
                    log.info("STATUS FINISHED.");
                    producerService
                            .sendFilebotExecutionTest(
                                    modelMapper.map(filebotExecution, FilebotExecutionTestODTO.class));
                    processNotProcessing();
                }
            }
        }
    }

    public void handleIncomingResponse(String chatId, String response) {
        log.info("Handle incomming response with chatId {} ,and  {}", chatId, response);
    }

    public void processNotProcessing() {
        log.info("processNotProcessing for all Idles");
        List<FilebotConversation> filebotConversations = filebotConversationService
                .findAll();
        if (filebotConversations.size() > 0) {
            FilebotConversation filebotConversation = filebotConversations.get(0);
            log.info("Processing filebotConversation {}", filebotConversation.getName());
            filebotExecutionService.getNextExecution().ifPresentOrElse((filebotExecution) -> {
                log.info("Processing filebotExecution {} on status {}", filebotExecution.getName(),
                        filebotExecution.getStatus());
                categoryExecutor.handleRequest(filebotConversation, filebotExecution, null);
                if (FilebotExecutionStatus.PROCESSED
                        .equals(filebotExecution.getStatus())) {
                    log.info("STATUS PROCESSED.");
                    producerService
                            .sendFilebotExecution(modelMapper.map(filebotExecution, FilebotExecutionODTO.class));
                    processNotProcessing();
                } else if (FilebotExecutionStatus.FINISHED
                        .equals(filebotExecution.getStatus())) {
                    log.info("STATUS FINISHED.");
                    producerService
                            .sendFilebotExecutionTest(
                                    modelMapper.map(filebotExecution, FilebotExecutionTestODTO.class));
                    processNotProcessing();
                }
            }, () -> log.info("No filebotExecution to process"));
        }
    }

    private void createSendMessageAndSendToRabbit(String text, String chatId, String filebotConversationId) {
        TelegramMessage telegramMessage = new TelegramMessage();
        telegramMessage.setChatId(chatId);
        telegramMessage.setText(TelegramUtils.abbreviate(text, 3000));
        telegramMessage.setReplyKeyboard(getKeyboardRemove());
        telegramMessage.setType(MessageType.TEXT);
        telegramMessage.setHandler(MessageHandler.FILEBOT);
        telegramMessage.setIdFilebotConversation(filebotConversationId);
        producerService.sendTelegramMessage(telegramMessage);
    }

    private ReplyKeyboard getKeyboardRemove() {
        ReplyKeyboardRemove replyKeyboardRemove = new ReplyKeyboardRemove();
        replyKeyboardRemove.setSelective(true);
        replyKeyboardRemove.setRemoveKeyboard(true);
        return replyKeyboardRemove;
    }

    private FilebotExecution convertToModel(FilebotExecutionIDTO filebotExecutionIDTO) {
        FilebotExecution filebotNameSelection = new FilebotExecution();
        filebotNameSelection.setId(filebotExecutionIDTO.getId());
        filebotNameSelection.setFiles(filebotExecutionIDTO.getFiles());
        filebotNameSelection.setPath(filebotExecutionIDTO.getPath());
        filebotNameSelection.setName(filebotExecutionIDTO.getName());
        filebotNameSelection.setPossibilities(filebotExecutionIDTO.getPossibilities());
        return filebotNameSelection;
    }

    private FilebotExecution convertToModel(FilebotExecutionTestIDTO filebotExecutionIDTO) {
        FilebotExecution filebotNameSelection = new FilebotExecution();
        filebotNameSelection.setId(filebotExecutionIDTO.getId());
        filebotNameSelection.setFiles(filebotExecutionIDTO.getFiles());
        filebotNameSelection.setPath(filebotExecutionIDTO.getPath());
        filebotNameSelection.setName(filebotExecutionIDTO.getName());
        filebotNameSelection.setPossibilities(filebotExecutionIDTO.getPossibilities());
        return filebotNameSelection;
    }

    public void stopConversation(String chatId) {
        FilebotConversation filebotConversation = filebotConversationService.findByChatId(chatId).get();
        createSendMessageAndSendToRabbit("La conversación ha sido detenida", chatId, filebotConversation.getId());
        filebotConversationService.deleteConversation(filebotConversation);
    }

    public void resetAllStatus() {
        List<FilebotConversation> filebotConversations = filebotConversationService.getFilebotConversations();
        for (FilebotConversation filebotConversation : filebotConversations) {
            for (FilebotExecution filebotExecution : filebotExecutionService
                    .getAllWithoutStatus(List.of(FilebotExecutionStatus.PROCESSED, FilebotExecutionStatus.FINISHED,
                            FilebotExecutionStatus.TEST))) {
                filebotExecution.setStatus(FilebotExecutionStatus.UNPROCESSED);
                filebotExecution.setOnCallback(false);
                filebotExecutionService.save(filebotExecution);
            }
            createSendMessageAndSendToRabbit("La conversación ha sido reiniciada para todos los usuarios activos",
                    filebotConversation.getChatId(), filebotConversation.getId());
            processNotProcessing();
        }

    }

    public void recieveTMDBData(TelegramFilebotExecutionODTO telegramFilebotExecutionODTO) {
        log.info("Recieved TMDB data {}", telegramFilebotExecutionODTO);
        FilebotExecution filebotExecution = filebotExecutionService.findById(telegramFilebotExecutionODTO.getId());
        filebotExecution
                .setPossibleChoicesTMDB(telegramFilebotExecutionODTO.getPossibleChoices());
        filebotExecution.setStatus(FilebotExecutionStatus.CHOICE);
        filebotExecutionService.save(filebotExecution);
        processNotProcessing();
    }

    private void sendMessageTelegramMessage(TelegramMessage message) {
        if (StringUtils.hasText(message.getText())) {
            SendMessage toSend = modelMapper.map(message, SendMessage.class);
            toSend.setParseMode(null);
            if (Objects.nonNull(message.getInlineKeyboardMarkup()))
                toSend.setReplyMarkup(message.getInlineKeyboardMarkup());
            if (Objects.nonNull(message.getReplyKeyboard()))
                toSend.setReplyMarkup(message.getReplyKeyboard());
            FilebotConversation filebotConversation = filebotConversationService
                    .findByChatId(message.getChatId())
                    .get();
            if (Objects.nonNull(filebotConversation)) {
                String messageId = filebotHandler.sendMessage(toSend);
                filebotConversation.setPreviousMessageId(messageId);
                filebotConversation.setCallbackData(message.getCallbackData());
                filebotConversationService.save(filebotConversation);
            }
        }
    }

    private void deletePreviousMessage(FilebotConversation filebotConversation) {
        TelegramMessage telegramMessage = new TelegramMessage();
        telegramMessage.setChatId(filebotConversation.getChatId());
        telegramMessage
                .setInlineKeyboardMarkup(
                        TelegramUtils.getEmptyInlineKeyboard());
        telegramMessage.setText(null);
        telegramMessage.setHandler(MessageHandler.FILEBOT);
        telegramMessage.setType(MessageType.DELETE_MESSAGE);
        telegramMessage.setMessageId(filebotConversation.getPreviousMessageId());
        producerService.sendTelegramMessage(telegramMessage);
    }

    private void sendPhotoTelegramMessage(TelegramMessage message) {
        SendPhoto toSend = modelMapper.map(message, SendPhoto.class);
        InputFile inputFile = new InputFile();
        inputFile.setMedia(message.getPhotoUrl());
        toSend.setPhoto(null);
        FilebotConversation filebotConversation = filebotConversationService
                .findByChatId(message.getIdFilebotConversation())
                .get();
        if (Objects.nonNull(filebotConversation)) {
            String messageId = filebotHandler.sendPhoto(toSend);
            filebotConversation.getPhotosMessageIds().add(messageId);
            filebotConversationService.save(filebotConversation);
        }
    }

    private void sendEditMessageTelegramMessage(TelegramMessage message) {
        EditMessageText toSend = modelMapper.map(message, EditMessageText.class);
        filebotHandler.sendEditMessage(toSend);
    }

    private void sendDeleteMessageTelegramMessage(TelegramMessage message) {
        DeleteMessage toSend = modelMapper.map(message, DeleteMessage.class);
        FilebotConversation filebotConversation = filebotConversationService
                .findByChatId(message.getChatId())
                .get();
        if (Objects.nonNull(filebotConversation)) {
            filebotHandler.deleteMessage(toSend);
            filebotConversationService.save(filebotConversation);
        }
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

}
