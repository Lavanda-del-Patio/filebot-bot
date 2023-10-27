package es.lavanda.telegram.bots.classify.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import es.lavanda.telegram.bots.classify.model.Qbittorrent;

@Repository
public interface QbitorrentRepository extends MongoRepository<Qbittorrent, String> {

	boolean existsByName(String name);

}
