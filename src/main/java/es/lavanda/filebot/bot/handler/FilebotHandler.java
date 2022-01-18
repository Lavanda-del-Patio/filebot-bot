package es.lavanda.filebot.bot.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ForceReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import es.lavanda.filebot.bot.config.BotConfig;
import es.lavanda.filebot.bot.exception.FilebotBotException;
import es.lavanda.filebot.bot.model.FilebotNameSelection;
import es.lavanda.filebot.bot.model.FilebotNameStatus;
import es.lavanda.filebot.bot.repository.FilebotNameRepository;
import es.lavanda.filebot.bot.service.ProducerService;
import es.lavanda.lib.common.model.FilebotExecutionODTO;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

@Component
@Slf4j
public class FilebotHandler extends TelegramLongPollingBot {

    @Autowired
    private BotConfig botConfig;

    @Autowired
    private FilebotNameRepository filebotNameRepository;

    @Autowired
    private ProducerService producerService;

    @Override
    public String getBotUsername() {
        return botConfig.getFilebotUser();
    }

    @Override
    public String getBotToken() {
        return botConfig.getFilebotToken();
    }

    @PostConstruct
    public void postConstruct() {
        // log.info("PostConstruct");
        // Optional<FilebotNameSelection> optFilebot =
        // filebotNameRepository.findByStatusPROCESSING();
        // if (Boolean.FALSE.equals(optFilebot.isPresent())) {
        // List<FilebotNameSelection> filebots =
        // filebotNameRepository.findAllByStatusUNPROCESSING();
        // if (Boolean.FALSE.equals(filebots.isEmpty())) {
        // sendMessageWithKeyboard(filebots.get(0));
        // }
        // }
    }

    @Scheduled(fixedRate = 60000)
    private void hourlyCheck() {
        log.info("Hourly check...");
        Optional<FilebotNameSelection> optFilebot = filebotNameRepository
                .findByStatus(FilebotNameStatus.PROCESSING.name());
        if (Boolean.FALSE.equals(optFilebot.isPresent())) {
            List<FilebotNameSelection> filebots = filebotNameRepository
                    .findAllByStatus(FilebotNameStatus.UNPROCESSING.name());
            if (Boolean.FALSE.equals(filebots.isEmpty())) {
                FilebotNameSelection filebotNameSelection = filebots.get(0);
                if (filebotNameSelection.getPossibilities().isEmpty()) {
                    sendMessage(filebotNameSelection);
                } else {
                    sendMessageWithKeyboard(filebotNameSelection);
                }
            }
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage()) {
                Message message = update.getMessage();
                if (message.hasText() || message.hasLocation()) {
                    handleIncomingMessage(message);
                }
            }
        } catch (Exception e) {
            log.info("Exception onUpdateReceived", e);
        }
    }

    private void sendMessageWithKeyboard(FilebotNameSelection filebotNameSelection) {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(botConfig.getFilebotAdmin());
        sendMessageRequest.setText(
                "La carpeta es " + filebotNameSelection.getPath() + "Escribe un nombre para forzar la respuesta");
        sendMessageRequest.setReplyMarkup(getReplyKeyboardMarkup(filebotNameSelection.getFiles()));
        try {
            execute(sendMessageRequest);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            log.error("Telegram exception sendind message with keyboard", e);
        }
    }

    private void sendMessage(FilebotNameSelection filebotNameSelection) {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(botConfig.getFilebotAdmin());
        sendMessageRequest.setText(
                "La carpeta con error es \n◦ **" + filebotNameSelection.getPath()
                        + "**\nEscribe para forzar el filebot a renombrar");
        sendMessageRequest.setReplyMarkup(getKeyboardRemove());

        try {
            execute(sendMessageRequest);
        } catch (TelegramApiException e) {
            log.error("Telegram exception sendind message with keyboard", e);
        }
        sendMessage(filebotNameSelection.getFiles());
        filebotNameSelection.setStatus(FilebotNameStatus.PROCESSING);
        filebotNameRepository.save(filebotNameSelection);
    }

    private void sendMessage(List<String> files) {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(botConfig.getFilebotAdmin());
        StringBuilder sb = new StringBuilder();
        files.forEach(f -> {
            sb.append("◦ " + f.trim());
            sb.append("\n");
        });
        sendMessageRequest.setText(sb.toString());
        try {
            execute(sendMessageRequest);
        } catch (TelegramApiException e) {
            log.error("Telegram exception sendind message with keyboard", e);
        }
    }

    private ReplyKeyboard getKeyboardRemove() {
        ReplyKeyboardRemove replyKeyboardRemove = new ReplyKeyboardRemove();
        replyKeyboardRemove.setSelective(true);
        replyKeyboardRemove.setRemoveKeyboard(true);
        return replyKeyboardRemove;
    }

    private void handleIncomingMessage(Message message) throws TelegramApiException {
        log.info("Handle incomming message");
        filebotNameRepository.findByStatus("PROCESSING").ifPresent(
                (x) -> {
                    producerService.sendFilebotExecution(
                            getFilebotExecutionODTO(message.getText(), x));
                    x.setStatus(FilebotNameStatus.PROCESSED);
                    filebotNameRepository.save(x);
                    log.info("Processed filebotNameSelectionId: " + x.getPath());
                });
    }

    private FilebotExecutionODTO getFilebotExecutionODTO(String text, FilebotNameSelection filebotNameSelection) {
        FilebotExecutionODTO filebotExecutionODTO = new FilebotExecutionODTO();
        filebotExecutionODTO.setForceQuery(true);
        filebotExecutionODTO.setQuery(text);
        filebotExecutionODTO.setId(filebotNameSelection.getId());
        return filebotExecutionODTO;
    }

    private ReplyKeyboardMarkup getReplyKeyboardMarkup(List<String> list) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(true);
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        list.forEach(row::add);
        keyboard.add(row);
        // row = new KeyboardRow();
        // // for (iterable_type iterable_element : iterable) {
        // // getNewCommand(language)
        // // }
        // row.add("3");
        // row.add("4");
        // keyboard.add(row);
        replyKeyboardMarkup.setKeyboard(keyboard);
        return replyKeyboardMarkup;
    }

}