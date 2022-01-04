package es.lavanda.filebot.bot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import es.lavanda.filebot.bot.model.FilebotNameSelection;
import es.lavanda.filebot.bot.model.FilebotNameSelection.FilebotNameStatus;
import es.lavanda.filebot.bot.repository.FilebotNameRepository;
import es.lavanda.lib.common.model.FilebotExecutionIDTO;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FilebotService {

    @Autowired
    private FilebotNameRepository filebotNameRepository;

    public void run(FilebotExecutionIDTO filebotExecutionIDTO) {
        FilebotNameSelection filebotNameSelection = convertToModel(filebotExecutionIDTO);
        filebotNameSelection.setStatus(FilebotNameStatus.UNPROCESSING);
        filebotNameRepository.save(filebotNameSelection);
    }

    private FilebotNameSelection convertToModel(FilebotExecutionIDTO filebotExecutionIDTO) {
        FilebotNameSelection filebotNameSelection = new FilebotNameSelection();
        filebotNameSelection.setId(filebotExecutionIDTO.getId());
        filebotNameSelection.setFiles(filebotExecutionIDTO.getFiles());
        filebotNameSelection.setPath(filebotExecutionIDTO.getPath());
        filebotNameSelection.setPossibilities(filebotExecutionIDTO.getPossibilities());

        return filebotNameSelection;
    }

}