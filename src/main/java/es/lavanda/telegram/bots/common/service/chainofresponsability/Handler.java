package es.lavanda.telegram.bots.common.service.chainofresponsability;

import es.lavanda.telegram.bots.filebot.model.FilebotConversation;
import es.lavanda.telegram.bots.filebot.model.FilebotExecution;

public interface Handler {

    void setNext(Handler handler);

    void handleRequest(FilebotConversation conversation, FilebotExecution filebotExecution, String callbackResponse);
}
