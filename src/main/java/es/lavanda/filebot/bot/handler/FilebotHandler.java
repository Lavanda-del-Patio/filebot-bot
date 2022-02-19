package es.lavanda.filebot.bot.handler;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
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
                if (message.hasText()) {
                    handleIncomingMessage(message);
                }
            } else {
                Message message = update.getCallbackQuery().getMessage();
                if (message.hasText()) {
                    handleCallbackMessage(update.getCallbackQuery());
                }
            }
        } catch (Exception e) {
            log.info("Exception onUpdateReceived", e);
        }
    }

    public void sendMessage(SendMessage sendMessage, boolean needSave) {
        try {
            sendMessage.enableMarkdown(true);
            Message message = execute(sendMessage);
            if (needSave) {
                saveMessageId(message.getChatId(), message.getMessageId());
            }
        } catch (TelegramApiException e) {
            log.error("Telegram exception sendind message with keyboard", e);
        }
    }

    public void deleteMessage(DeleteMessage deleteMessage) {
        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            log.error("Telegram exception deleteMessage message with keyboard", e);
        }
    }

    private void saveMessageId(Long chatId, Integer messageId) {
        filebotServiceImpl.saveMessageId(String.valueOf(chatId), String.valueOf(messageId));
    }

    public void sendMessage(EditMessageText sendMessage) {
        try {
            sendMessage.enableMarkdown(true);
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Telegram exception sendind message with keyboard", e);
        }
    }

    private void handleIncomingMessage(Message message) throws TelegramApiException {
        filebotServiceImpl.handleIncomingResponse(String.valueOf(message.getChatId()), message.getText());
    }

    private void handleCallbackMessage(CallbackQuery callbackQuery) throws TelegramApiException {
        filebotServiceImpl.handleCallbackResponse(String.valueOf(callbackQuery.getMessage().getChatId()),
                String.valueOf(callbackQuery.getMessage().getMessageId()), callbackQuery.getData());
    }

}