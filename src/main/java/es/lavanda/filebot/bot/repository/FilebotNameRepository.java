package es.lavanda.filebot.bot.repository;

import org.springframework.stereotype.Repository;

import es.lavanda.filebot.bot.model.FilebotNameSelection;

import org.springframework.data.mongodb.repository.MongoRepository;

@Repository
public interface FilebotNameRepository extends MongoRepository<FilebotNameSelection, String> {

}
