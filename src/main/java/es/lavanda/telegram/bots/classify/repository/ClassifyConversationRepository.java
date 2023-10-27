package es.lavanda.telegram.bots.classify.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import es.lavanda.telegram.bots.classify.model.ClassifyConversation;

@Repository
public interface ClassifyConversationRepository extends MongoRepository<ClassifyConversation, String> {

    List<ClassifyConversation> findAllByStatusIn(List<String> status);

    List<ClassifyConversation> findAllByStatus(String status);

    Optional<ClassifyConversation> findByChatId(String chatId);

    boolean existsByChatId(String chatId);

    Optional<ClassifyConversation> findByStatusStartsWith(String string);
}
