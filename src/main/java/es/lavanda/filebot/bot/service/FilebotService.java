package es.lavanda.filebot.bot.service;

import es.lavanda.filebot.bot.handler.FilebotHandler;
import es.lavanda.filebot.bot.model.TelegramFilebotExecution;
import es.lavanda.lib.common.model.FilebotExecutionIDTO;

public interface FilebotService {

    void run(FilebotExecutionIDTO filebotExecutionIDTO);

    void newConversation(String chatId,String name);

    void stopConversation(String chatId);

    void handleIncomingResponse(String chatId, String response);

    void handleCallbackResponse(String chatId, String messageId, String response);

    void saveMessageId(String chatId, String messageId);

    void setFilebotHandler(FilebotHandler filebotHandler);

    void processNotProcessing(TelegramFilebotExecution filebotNameSelection);

    // void processNotProcessing(String chatId);

}