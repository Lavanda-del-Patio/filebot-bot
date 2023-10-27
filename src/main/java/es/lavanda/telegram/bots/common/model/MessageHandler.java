package es.lavanda.telegram.bots.common.model;

public interface MessageHandler {
    void handle(TelegramMessage message);
    // Otros métodos comunes
}