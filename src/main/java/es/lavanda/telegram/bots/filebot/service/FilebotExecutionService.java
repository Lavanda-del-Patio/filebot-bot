package es.lavanda.telegram.bots.filebot.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import es.lavanda.telegram.bots.filebot.model.FilebotExecution;
import es.lavanda.telegram.bots.filebot.model.FilebotExecution.FilebotExecutionStatus;
import es.lavanda.telegram.bots.filebot.repository.FilebotExecutionRepository;

@Service
public class FilebotExecutionService {

    @Autowired
    private FilebotExecutionRepository filebotExecutionRepository;

    public FilebotExecution getNextUnprocessed() {
        return filebotExecutionRepository.findFirstByStatusIn(List
                .of(FilebotExecution.FilebotExecutionStatus.UNPROCESSED, FilebotExecution.FilebotExecutionStatus.TEST));
    }

    public FilebotExecution save(FilebotExecution filebotExecution) {
        return filebotExecutionRepository.save(filebotExecution);
    }

    public List<FilebotExecution> getAllWithoutStatus(List<FilebotExecutionStatus> processed) {
        return filebotExecutionRepository.findAllByStatusNotIn(processed);
    }

    public FilebotExecution getByStatusNotIn(List<FilebotExecutionStatus> filebotExecutionStatus) {
        return filebotExecutionRepository.findByStatusNotIn(filebotExecutionStatus);
    }

    public FilebotExecution findById(String id) {
        return filebotExecutionRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
    }

    public FilebotExecution setPreviousStatus(FilebotExecution filebotExecution) {
        filebotExecution.setStatus(filebotExecution.getPreviousStatus());
        return filebotExecutionRepository.save(filebotExecution);
    }

    public FilebotExecution getOnCallback() {
        return filebotExecutionRepository.findByOnCallback(true);
    }

}
