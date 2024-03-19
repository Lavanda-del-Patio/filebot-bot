package es.lavanda.telegram.bots.filebot.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import es.lavanda.telegram.bots.filebot.model.FilebotConversation;

@Repository
public interface FilebotConversationRepository extends MongoRepository<FilebotConversation, String> {

    Optional<FilebotConversation> findByChatId(String chatId);

    boolean existsByChatId(String chatId);

}
