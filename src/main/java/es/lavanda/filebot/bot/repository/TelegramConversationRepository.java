package es.lavanda.filebot.bot.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import es.lavanda.filebot.bot.model.TelegramConversation;

@Repository
public interface TelegramConversationRepository extends MongoRepository<TelegramConversation, String> {

    List<TelegramConversation> findAllByStatus(String status);

    TelegramConversation findByChatId(String chatId);

    boolean existsByChatId(String chatId);
}
