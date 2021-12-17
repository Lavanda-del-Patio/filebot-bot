package es.lavanda.filebot.bot.repository;

import org.springframework.stereotype.Repository;

import es.lavanda.filebot.bot.model.Filebot;

import org.springframework.data.repository.PagingAndSortingRepository;


@Repository
public interface FilebotRepository extends PagingAndSortingRepository<Filebot, String> {

    
}
