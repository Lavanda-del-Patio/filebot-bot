package es.lavanda.filebot.bot.util;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TelegramUtils {

    public Message sendMessage(SendMessage sendMessage) {
        log.info(("Sending message"));
        return null;
    }

}
