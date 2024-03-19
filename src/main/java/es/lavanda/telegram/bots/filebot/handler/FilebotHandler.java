package es.lavanda.telegram.bots.filebot.handler;

import java.util.Objects;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.MaybeInaccessibleMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import es.lavanda.telegram.bots.filebot.config.FilebotConfig;
import es.lavanda.telegram.bots.filebot.exception.FilebotException;
import es.lavanda.telegram.bots.filebot.service.FilebotService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class FilebotHandler extends TelegramLongPollingBot {

    private FilebotService filebotService;

    private FilebotConfig botConfig;

    @Override
    public String getBotUsername() {
        return botConfig.getUsername();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage() && isAuthorized(update.getMessage().getFrom().getUserName())) {
                log.info("Authorized message");
                Message message = update.getMessage();
                if (message.hasText()) {
                    handleIncomingMessage(message);
                }
            } else if (Objects.nonNull(update.getCallbackQuery())) {
                log.info("Authorized getCallbackQuery");
                MaybeInaccessibleMessage maybeInaccessibleMessage = update.getCallbackQuery().getMessage();
                if (maybeInaccessibleMessage instanceof Message && ((Message) maybeInaccessibleMessage).hasText()) {
                    handleCallbackMessage(update.getCallbackQuery());
                }
            } else {
                log.info("Unauthorized user: {}", update.getMessage().getFrom().getUserName());
            }
        } catch (Exception e) {
            log.info("Exception onUpdateReceived", e);
        }
    }

    public String sendMessage(SendMessage sendMessage) {
        try {
            sendMessage.enableMarkdown(true);
            Message message = execute(sendMessage);
            return message.getMessageId().toString();
        } catch (TelegramApiException e) {
            log.error("Telegram exception sendind message with keyboard", e);
            throw new FilebotException("Telegram exception sendind message with keyboard", e);
        }
    }

    public String sendPhoto(SendPhoto sendPhoto) {
        try {
            Message message = execute(sendPhoto);
            return message.getMessageId().toString();
        } catch (TelegramApiException e) {
            log.error("Telegram exception sending photo", e);
            throw new FilebotException("Telegram exception sending photo", e);
        }
    }

    public void sendEditMessage(EditMessageText sendMessage) {
        try {
            sendMessage.enableMarkdown(true);
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Telegram exception sendind message with keyboard", e);
            throw new FilebotException("Telegram exception sendind edit message", e);
        }
    }

    public void deleteMessage(DeleteMessage deleteMessage) {
        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            log.error("Telegram exception deleteMessage message with keyboard", e);
            if (e.getMessage()
                    .contains("Error deleting message: [400] Bad Request: message can't be deleted for everyone")) {
                log.info("Message already deleted");
            } else {
                throw new FilebotException("Telegram exception deleting message", e);
            }
        }
    }

    private void handleIncomingMessage(Message message) throws TelegramApiException {
        if (message.getText().startsWith("/start")) {
            if (message.isGroupMessage())
                filebotService.newConversation(String.valueOf(message.getChatId()),
                        message.getChat().getTitle());
            else {
                filebotService.newConversation(String.valueOf(message.getChatId()),
                        message.getFrom().getUserName());
            }
        } else if (message.getText().startsWith("/stop")) {
            filebotService.stopConversation(String.valueOf(message.getChatId()));
        } else if (message.getText().startsWith("/reset")) {
            filebotService.resetAllStatus();
        } else {
            filebotService.handleIncomingResponse(String.valueOf(message.getChatId()),
                    message.getText());
        }
    }

    private void handleCallbackMessage(CallbackQuery callbackQuery) throws TelegramApiException {
        filebotService.handleCallbackResponse(String.valueOf(callbackQuery.getMessage().getChatId()),
                String.valueOf(callbackQuery.getMessage().getMessageId()),
                callbackQuery.getData());
    }

    private boolean isAuthorized(String username) {
        return botConfig.isAuthorizedToUseBot(username);
    }
}