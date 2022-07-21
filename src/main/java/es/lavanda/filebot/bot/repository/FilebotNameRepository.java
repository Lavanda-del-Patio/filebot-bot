package es.lavanda.filebot.bot.repository;

import org.springframework.stereotype.Repository;

import es.lavanda.filebot.bot.model.TelegramFilebotExecution;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

@Repository
public interface FilebotNameRepository extends MongoRepository<TelegramFilebotExecution, String> {

    Optional<TelegramFilebotExecution> findByStatusStartsWith(String status);

    List<TelegramFilebotExecution> findAllByStatus(String Status);

}
