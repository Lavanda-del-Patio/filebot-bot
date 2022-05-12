package es.lavanda.filebot.bot.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import es.lavanda.filebot.bot.model.FilebotPreselection;

@Repository
public interface FilebotPreselectionRepository extends MongoRepository<FilebotPreselection, String> {

    Optional<FilebotPreselection> findByPreselection(String preselection);
}
