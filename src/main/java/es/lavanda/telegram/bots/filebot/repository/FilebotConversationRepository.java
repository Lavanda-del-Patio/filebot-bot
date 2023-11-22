package es.lavanda.telegram.bots.filebot.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import es.lavanda.telegram.bots.filebot.model.FilebotConversation;
import es.lavanda.telegram.bots.filebot.model.FilebotConversation.FilebotConversationStatus;

@Repository
public interface FilebotConversationRepository extends MongoRepository<FilebotConversation, String> {

    List<FilebotConversation> findAllByConversationStatus(FilebotConversationStatus status);

    Optional<FilebotConversation> findByChatId(String chatId);

    boolean existsByChatId(String chatId);

    FilebotConversation findByConversationStatus(FilebotConversationStatus filebotConversationStatus);

}
