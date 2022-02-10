package es.lavanda.filebot.bot.handler;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import es.lavanda.filebot.bot.service.FilebotService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class FilebotHandler extends TelegramLongPollingBot {

    private String botUsername;

    private String botToken;

    private FilebotService filebotServiceImpl;

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
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

    public void sendMessage(SendMessage sendMessage) {
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Telegram exception sendind message with keyboard", e);
        }
    }

    private void handleIncomingMessage(Message message) throws TelegramApiException {
        filebotServiceImpl.handleIncomingResponse(String.valueOf(message.getChatId()), message.getText());
    }

}