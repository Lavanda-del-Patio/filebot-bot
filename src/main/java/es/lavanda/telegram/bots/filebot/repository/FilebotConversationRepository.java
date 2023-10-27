package es.lavanda.telegram.bots.filebot.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import es.lavanda.telegram.bots.filebot.model.FilebotConversation;

@Repository
public interface FilebotConversationRepository extends MongoRepository<FilebotConversation, String> {
    
    List<FilebotConversation> findAllByStatus(String status);

    Optional<FilebotConversation> findByChatId(String chatId);

    boolean existsByChatId(String chatId);
}
