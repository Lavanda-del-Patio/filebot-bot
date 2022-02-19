package es.lavanda.filebot.bot.service;

import es.lavanda.filebot.bot.handler.FilebotHandler;
import es.lavanda.lib.common.model.FilebotExecutionIDTO;

public interface FilebotService {

    void run(FilebotExecutionIDTO filebotExecutionIDTO);

    void handleIncomingResponse(String chatId, String response);

    void handleCallbackResponse(String chatId, String messageId, String response);

    void saveMessageId(String chatId, String messageId);

    void setFilebotHandler(FilebotHandler filebotHandler);
}