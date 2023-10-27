package es.lavanda.telegram.bots.filebot.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import es.lavanda.telegram.bots.filebot.model.FilebotExecution;

@Repository
public interface FilebotExecutionRepository extends MongoRepository<FilebotExecution, String> {

    Optional<FilebotExecution> findByStatusStartsWith(String status);

    List<FilebotExecution> findAllByStatus(String Status);
}
