package es.lavanda.telegram.bots.filebot.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import es.lavanda.telegram.bots.filebot.model.Elected;

@Repository
public interface ElectedRepository extends MongoRepository<Elected, String> {

}
