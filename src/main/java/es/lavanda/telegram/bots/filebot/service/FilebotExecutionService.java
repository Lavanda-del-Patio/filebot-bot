package es.lavanda.telegram.bots.filebot.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import es.lavanda.telegram.bots.filebot.model.FilebotExecution;
import es.lavanda.telegram.bots.filebot.model.FilebotExecution.FilebotExecutionStatus;
import es.lavanda.telegram.bots.filebot.repository.FilebotExecutionRepository;

@Service
public class FilebotExecutionService {

    @Autowired
    private FilebotExecutionRepository filebotExecutionRepository;

    public Optional<FilebotExecution> getNextExecution() {
        List<FilebotExecution> executions = filebotExecutionRepository
                .findAll();
        for (FilebotExecution filebotExecution : executions) {
            if (FilebotExecutionStatus.UNPROCESSED.equals(filebotExecution.getStatus())
                    || FilebotExecutionStatus.TEST.equals(filebotExecution.getStatus())
                    || FilebotExecutionStatus.CHOICE.equals(filebotExecution.getStatus())) {
                if (Boolean.FALSE.equals(filebotExecution.isOnCallback() && getOnCallback() != null)) {
                    return Optional.of(filebotExecution);
                }
            }
        }
        return Optional.empty();
    }

    public List<FilebotExecution> getAllWithoutStatus(List<FilebotExecutionStatus> filebotExecutionStatus) {
        return filebotExecutionRepository.findAllByStatusNotIn(filebotExecutionStatus);
    }

    public FilebotExecution save(FilebotExecution filebotExecution) {
        return filebotExecutionRepository.save(filebotExecution);
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
