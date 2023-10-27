package es.lavanda.telegram.bots.classify.handler;

import java.util.Objects;

import org.junit.platform.commons.util.StringUtils;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import es.lavanda.telegram.bots.classify.config.ClassifyConfig;
import es.lavanda.telegram.bots.classify.exception.ClassifyException;
import es.lavanda.telegram.bots.classify.service.ClassifyService;
import es.lavanda.telegram.bots.common.model.MessageHandler;
import es.lavanda.telegram.bots.common.model.TelegramMessage;
import es.lavanda.telegram.bots.filebot.config.FilebotConfig;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class ClassifyHandler extends TelegramLongPollingBot implements MessageHandler {

    private ClassifyService classifyService;

    private ClassifyConfig classifyConfig;

    @Override
    public String getBotUsername() {
        return classifyConfig.getUsername();
    }

    @Override
    public String getBotToken() {
        return classifyConfig.getToken();
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
                Message message = update.getCallbackQuery().getMessage();
                if (message.hasText()) {
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
            throw new ClassifyException("Telegram exception sendind message with keyboard", e);
        }
    }

    public String sendPhoto(SendPhoto sendPhoto) {
        try {
            Message message = execute(sendPhoto);
            return message.getMessageId().toString();
        } catch (TelegramApiException e) {
            log.error("Telegram exception sending photo", e);
            throw new ClassifyException("Telegram exception sending photo", e);
        }
    }

    public void sendEditMessage(EditMessageText sendMessage) {
        try {
            sendMessage.enableMarkdown(true);
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Telegram exception sendind message with keyboard", e);
            throw new ClassifyException("Telegram exception sendind edit message", e);
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
                throw new ClassifyException("Telegram exception deleting message", e);
            }
        }
    }

    private void handleIncomingMessage(Message message) throws TelegramApiException {
        if (message.getText().startsWith("/start")) {
            if (message.isGroupMessage())
                classifyService.newConversation(String.valueOf(message.getChatId()), message.getChat().getTitle());
            else {
                classifyService.newConversation(String.valueOf(message.getChatId()),
                        message.getFrom().getUserName());
            }
        } else if (message.getText().startsWith("/stop")) {
            classifyService.stopConversation(String.valueOf(message.getChatId()));
        }
        // else if (message.getText().startsWith("/reset")) {
        // classifyService.resetAllStatus();
        // }
        else {
            classifyService.handleIncomingResponse(String.valueOf(message.getChatId()), message.getText());
        }
    }

    private void handleCallbackMessage(CallbackQuery callbackQuery) throws TelegramApiException {
        classifyService.handleCallbackResponse(String.valueOf(callbackQuery.getMessage().getChatId()),
                String.valueOf(callbackQuery.getMessage().getMessageId()), callbackQuery.getData());
    }

    private boolean isAuthorized(String username) {
        return classifyConfig.isAuthorizedToUseBot(username);
    }

    @Override
    public void handle(TelegramMessage message) {
        switch (message.getType()) {
            case TEXT:
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(message.getChatId());
                sendMessage.setText(message.getText());
                sendMessage(sendMessage);
                break;
            case PHOTO:
                SendPhoto sendPhoto = new SendPhoto();
                sendPhoto.setChatId(message.getChatId());
                sendPhoto.setPhoto(message.getPhoto());
                sendPhoto(sendPhoto);
                break;
            case EDIT_MESSAGE:
                EditMessageText editMessageText = new EditMessageText();
                editMessageText.setChatId(message.getChatId());
                editMessageText.setMessageId(message.getMessageId());
                editMessageText.setText(message.getText());
                sendEditMessage(editMessageText);
                break;
            case DELETE_MESSAGE:
                DeleteMessage deleteMessage = new DeleteMessage();
                deleteMessage.setChatId(message.getChatId());
                deleteMessage.setMessageId(message.getMessageId());
                deleteMessage(deleteMessage);
                break;
            default:
                break;

        }
    }

}