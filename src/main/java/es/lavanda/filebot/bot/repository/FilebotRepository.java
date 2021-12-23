package es.lavanda.filebot.bot.repository;

import org.springframework.stereotype.Repository;

import es.lavanda.filebot.bot.model.Filebot;

import org.springframework.data.mongodb.repository.MongoRepository;


@Repository
public interface FilebotRepository extends MongoRepository<Filebot, String> {

    
}
