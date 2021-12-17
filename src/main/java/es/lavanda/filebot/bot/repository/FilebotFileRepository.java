package es.lavanda.filebot.bot.repository;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import es.lavanda.filebot.bot.model.FilebotFile;


@Repository
public interface FilebotFileRepository extends PagingAndSortingRepository<FilebotFile, String> {
    
}
