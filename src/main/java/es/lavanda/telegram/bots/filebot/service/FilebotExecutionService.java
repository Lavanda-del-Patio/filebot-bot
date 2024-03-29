package es.lavanda.telegram.bots.filebot.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import es.lavanda.telegram.bots.filebot.model.FilebotExecution;
import es.lavanda.telegram.bots.filebot.model.FilebotExecution.FilebotExecutionStatus;
import es.lavanda.telegram.bots.filebot.repository.FilebotExecutionRepository;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
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
                FilebotExecution onCallback = getOnCallback();
                if (Objects.isNull(onCallback)) {
                    log.info(
                            "getNextExecution: " + filebotExecution.getName() + " " + filebotExecution.getStatus() + " "
                                    + filebotExecution.isOnCallback());
                    if (Boolean.FALSE.equals(
                            filebotExecution.isOnCallback())) {
                        log.info("getNextExecution-IN: " + filebotExecution.getName() + " "
                                + filebotExecution.getStatus());
                        return Optional.of(filebotExecution);
                    }
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
