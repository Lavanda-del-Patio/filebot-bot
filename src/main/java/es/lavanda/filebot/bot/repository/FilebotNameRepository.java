package es.lavanda.filebot.bot.repository;

import org.springframework.stereotype.Repository;

import es.lavanda.filebot.bot.model.FilebotNameSelection;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

@Repository
public interface FilebotNameRepository extends MongoRepository<FilebotNameSelection, String> {

    Optional<FilebotNameSelection> findByStatusStartsWith(String status);

    List<FilebotNameSelection> findAllByStatus(String Status);

}
