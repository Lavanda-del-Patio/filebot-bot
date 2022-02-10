package es.lavanda.filebot.bot.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.annotation.AccessType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import es.lavanda.filebot.bot.config.BotConfig;
import es.lavanda.filebot.bot.handler.FilebotHandler;
import es.lavanda.filebot.bot.model.FilebotNameSelection;
import es.lavanda.filebot.bot.model.FilebotNameStatus;
import es.lavanda.filebot.bot.repository.FilebotNameRepository;
import es.lavanda.filebot.bot.service.FilebotService;
import es.lavanda.filebot.bot.service.ProducerService;
import es.lavanda.lib.common.model.FilebotExecutionIDTO;
import es.lavanda.lib.common.model.FilebotExecutionODTO;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Scope("singleton")
public class FilebotServiceImpl implements FilebotService {

    private FilebotHandler filebotHandler;

    @Autowired
    private FilebotNameRepository filebotNameRepository;

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
        filebotHandler.sendMessage(sendMessageRequest);
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
                        "La carpeta es **%s**.\nLos ficheros son:\n%s\nSelecciona el posible tipo de resultado:",
                        filebotNameSelection.getPath(), sb.toString()));
        sendMessageRequest.setReplyMarkup(getReplyKeyboardMarkup(List.of("TV", "MOVIE")));
        // sendMessageRequest.setReplyMarkup(getInlineKeyboard(List.of("TV", "MOVIE")));

        filebotHandler.sendMessage(sendMessageRequest);
    }

    private void sendMessageToForceStrict() {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(botConfig.getFilebotAdmin());
        sendMessageRequest.setText(
                "¿Quieres ser stricto a la hora de procesarlo?");
        sendMessageRequest.setReplyMarkup(getReplyKeyboardMarkup(List.of("SI", "NO")));
        // sendMessageRequest.setReplyMarkup(getInlineKeyboard(List.of("SI", "NO")));

        filebotHandler.sendMessage(sendMessageRequest);
    }

    private void sendMessageToForceQuery() {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(botConfig.getFilebotAdmin());
        sendMessageRequest.setText(
                "¿Quieres agregar un texto extra para potenciar el filtrado?");
        sendMessageRequest.setReplyMarkup(getReplyKeyboardMarkup(List.of("NO")));
        // sendMessageRequest.setReplyMarkup(getInlineKeyboard(List.of("NO")));

        filebotHandler.sendMessage(sendMessageRequest);
    }
    // private void sendMessageWithKeyboard(FilebotNameSelection
    // filebotNameSelection) {
    // SendMessage sendMessageRequest = new SendMessage();
    // sendMessageRequest.setChatId(botConfig.getFilebotAdmin());
    // sendMessageRequest.setText(
    // "La carpeta es " + filebotNameSelection.getPath() + "Escribe un nombre para
    // forzar la respuesta");
    // sendMessageRequest.setReplyMarkup(getReplyKeyboardMarkup(filebotNameSelection.getFiles()));
    // filebotHandler.sendMessage(sendMessageRequest);
    // }

    // private void sendMessage(FilebotNameSelection filebotNameSelection) {
    // SendMessage sendMessageRequest = new SendMessage();
    // sendMessageRequest.setChatId(botConfig.getFilebotAdmin());
    // sendMessageRequest.setText(
    // "La carpeta con error es \n◦ **" + filebotNameSelection.getPath()
    // + "**\nEscribe para forzar el filebot a renombrar");
    // sendMessageRequest.setReplyMarkup(getKeyboardRemove());
    // filebotHandler.execute(sendMessageRequest);
    // sendMessage(filebotNameSelection.getFiles());
    // filebotNameSelection.setStatus(FilebotNameStatus.PROCESSING);
    // filebotNameRepository.save(filebotNameSelection);
    // }

    private void sendMessage(List<String> files) {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(botConfig.getFilebotAdmin());
        StringBuilder sb = new StringBuilder();
        files.forEach(f -> {
            sb.append("◦ " + f.trim());
            sb.append("\n");
        });
        sendMessageRequest.setText(sb.toString());
        filebotHandler.sendMessage(sendMessageRequest);
    }

    private void sendMessage(String text) {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(botConfig.getFilebotAdmin());
        sendMessageRequest.setText(text);
        sendMessageRequest.setReplyMarkup(getKeyboardRemove());
        filebotHandler.sendMessage(sendMessageRequest);

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

    // private ReplyKeyboardMarkup getReplyKeyboardMarkup(List<String> list) {
    // ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
    // replyKeyboardMarkup.setSelective(true);
    // replyKeyboardMarkup.setResizeKeyboard(true);
    // replyKeyboardMarkup.setOneTimeKeyboard(true);
    // List<KeyboardRow> keyboard = new ArrayList<>();
    // KeyboardRow row = new KeyboardRow();
    // Keyboard
    // list.forEach((object) -> keyboard.add(row));
    // // keyboard.add(row);

    // replyKeyboardMarkup.setKeyboard(keyboard);
    // return replyKeyboardMarkup;
    // }

    private ReplyKeyboardMarkup getReplyKeyboardMarkup(List<String> list) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(false);
        replyKeyboardMarkup.setOneTimeKeyboard(true);
        List<KeyboardRow> keyboard = new ArrayList<>();
        for (String languageName : list) {
            KeyboardRow row = new KeyboardRow();
            row.add(languageName);
            keyboard.add(row);
        }
        // KeyboardRow row = new KeyboardRow();
        // row.add(getCancelCommand(language));
        // keyboard.add(row);
        replyKeyboardMarkup.setKeyboard(keyboard);
        return replyKeyboardMarkup;
    }

    private InlineKeyboardMarkup getInlineKeyboard(List<String> list) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        for (String object : list) {
            List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();
            InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
            inlineKeyboardButton.setText(object);
            inlineKeyboardButton.setCallbackData(object);
            keyboardButtonsRow1.add(inlineKeyboardButton);
            rowList.add(keyboardButtonsRow1);
        }
        inlineKeyboardMarkup.setKeyboard(rowList);
        return inlineKeyboardMarkup;
    }

    @Override
    public void handleIncomingResponse(String chatId, String response) {
        log.info("Handle incomming message");
        filebotNameRepository.findByStatusStartsWith("PROCESSING").ifPresent(
                (filebotNameSelection) -> {
                    handleProcessing(filebotNameSelection, response);
                    // Aqui va la diferencia entre la ejecución de un mensaje normal, de un segundo
                    // mensaje o de una selección de inputKeyboard
                    // producerService.sendFilebotExecution(
                    // getFilebotExecutionODTO(message.getText(), filebotNameSelection));
                    // filebotNameSelection.setStatus(FilebotNameStatus.PROCESSED);
                    // filebotNameRepository.save(filebotNameSelection);
                    // sendMessage("Guardado y procesado");
                    // log.info("Processed filebotNameSelectionId: " +
                    // filebotNameSelection.getPath());
                    // hourlyCheck();
                });
    }

    private void handleProcessing(FilebotNameSelection filebotNameSelection, String response) {
        switch (filebotNameSelection.getStatus()) {
            case PROCESSING_LABEL:
                handleProcessingLabel(filebotNameSelection, response);
                break;
            case PROCESSING_FORCE_STRICT:
                handleProcessingForceStrict(filebotNameSelection, response);
                break;
            case PROCESSING_QUERY:
                handleProcessingQuery(filebotNameSelection, response);
                break;
            case PROCESSING_WITH_POSSIBILITIES:
                handleProcessingWithPossibilities(filebotNameSelection, response);
                break;
            default:
                log.error("It should not be here");
                break;
        }
    }

    private void handleProcessingWithPossibilities(FilebotNameSelection filebotNameSelection, String response) {
        log.info("Handle processing with possibilities");
    }

    private void handleProcessingQuery(FilebotNameSelection filebotNameSelection, String response) {
        log.info("Handle processing query");
        if (Boolean.FALSE.equals(response.equals("NO"))) {
            filebotNameSelection.setQuery(response);
        }
        // Aqui va la diferencia entre la ejecución de un mensaje normal, de un segundo
        // mensaje o de una selección de inputKeyboard
        producerService.sendFilebotExecution(
                getFilebotExecutionODTO(filebotNameSelection));
        filebotNameSelection.setStatus(FilebotNameStatus.PROCESSED);
        filebotNameRepository.save(filebotNameSelection);
        sendMessage("Guardado y procesado");
        log.info("Processed filebotNameSelectionId: " +
                filebotNameSelection.getPath());
        hourlyCheck();

    }

    private void handleProcessingForceStrict(FilebotNameSelection filebotNameSelection, String response) {
        log.info("Handle processing force strict");
        filebotNameSelection.setForceStrict(response.equals("SI"));
        sendMessageToForceQuery();
        filebotNameSelection.setStatus(FilebotNameStatus.PROCESSING_QUERY);
        filebotNameRepository.save(filebotNameSelection);
    }

    private void handleProcessingLabel(FilebotNameSelection filebotNameSelection, String response) {
        log.info("Handle processing label");
        filebotNameSelection.setLabel(response);
        sendMessageToForceStrict();
        filebotNameSelection.setStatus(FilebotNameStatus.PROCESSING_FORCE_STRICT);
        filebotNameRepository.save(filebotNameSelection);
    }

    @Override
    public void setFilebotHandler(FilebotHandler filebotHandler) {
        this.filebotHandler = filebotHandler;
    }

}
