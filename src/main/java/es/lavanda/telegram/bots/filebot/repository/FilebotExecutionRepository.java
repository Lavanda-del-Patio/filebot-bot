package es.lavanda.telegram.bots.filebot.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import es.lavanda.telegram.bots.filebot.model.FilebotExecution;
import es.lavanda.telegram.bots.filebot.model.FilebotExecution.FilebotExecutionStatus;

@Repository
public interface FilebotExecutionRepository extends MongoRepository<FilebotExecution, String> {

    List<FilebotExecution> findAllByStatusNotInAndOnCallback(List<FilebotExecutionStatus> filebotExecutionStatus,
            boolean onCallback);

    List<FilebotExecution> findAllByStatusNotIn(List<FilebotExecutionStatus> filebotExecutionStatus);

    FilebotExecution findFirstByStatusIn(List<FilebotExecutionStatus> filebotExecutionStatus);

    FilebotExecution findByOnCallback(boolean b);

}
