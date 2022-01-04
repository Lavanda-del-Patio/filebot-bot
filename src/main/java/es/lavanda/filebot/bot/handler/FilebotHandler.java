package es.lavanda.filebot.bot.handler;

import org.springframework.beans.factory.annotation.Autowired;
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
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

@Component
@Slf4j
public class FilebotHandler extends TelegramLongPollingBot {

    @Autowired
    private BotConfig botConfig;

    @Override
    public String getBotUsername() {
        return botConfig.FILEBOT_USER;
    }

    @Override
    public String getBotToken() {
        return botConfig.FILEBOT_TOKEN;
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

    private void handleIncomingMessage(Message message) throws TelegramApiException {
        // final int state =
        // DatabaseManager.getInstance().getWeatherState(message.getFrom().getId(),
        // message.getChatId());
        // final String language =
        // DatabaseManager.getInstance().getUserWeatherOptions(message.getFrom().getId())[0];
        // if (!message.isUserMessage() && message.hasText()) {
        // if (isCommandForOther(message.getText())) {
        // return;
        // } else if (message.getText().startsWith(Commands.STOPCOMMAND)){
        // sendHideKeyboard(message.getFrom().getId(), message.getChatId(),
        // message.getMessageId());
        // return;
        // }
        // }
        String response = message.getText();
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(message.getChatId().toString());
        sendMessageRequest.setText("Hola");
        sendMessageRequest.setReplyMarkup(getReplyKeyboardMarkup());

        execute(sendMessageRequest);
    }

    private ReplyKeyboardMarkup getReplyKeyboardMarkup() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(true);
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("1");
        row.add("2");
        keyboard.add(row);
        row = new KeyboardRow();
        // for (iterable_type iterable_element : iterable) {
        // getNewCommand(language)
        // }
        row.add("3");
        row.add("4");
        keyboard.add(row);
        replyKeyboardMarkup.setKeyboard(keyboard);
        return replyKeyboardMarkup;
    }
}