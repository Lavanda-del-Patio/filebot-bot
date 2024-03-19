package es.lavanda.telegram.bots.filebot.service;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import es.lavanda.lib.common.model.FilebotExecutionODTO;
import es.lavanda.telegram.bots.filebot.model.Elected;
import es.lavanda.telegram.bots.filebot.model.FilebotExecution;
import es.lavanda.telegram.bots.filebot.repository.ElectedRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ElectedService {

    private final ElectedRepository electedRepository;

    private ModelMapper modelMapper = new ModelMapper();

    public List<Elected> getAll() {
        return electedRepository.findAll();
    }

    public void remove(Elected elected) {
        electedRepository.delete(elected);
    }

    public List<Elected> save(List<Elected> electeds) {
        return electedRepository.saveAll(electeds);
    }

    public Elected save(Elected elected) {
        return electedRepository.save(elected);
    }

    public Elected save(FilebotExecution filebotExecution, String nameOfTheShowOrFilm) {
        List<Elected> electeds = getAll();
        if (electeds.size() < 3) {
            restartOneTimeElecteds(electeds);
            return electedRepository.save(createNewElected(filebotExecution, nameOfTheShowOrFilm));
        } else {
            log.error("Can't save a new elected because there are three already.");
            restartOneTimeElecteds(electeds);
            return null;
        }
    }

    private void restartOneTimeElecteds(List<Elected> electeds) {
        for (Elected elected : electeds) {
            elected.setTimes(elected.getTimes() - 1);
            if (elected.getTimes() == 0) {
                remove(elected);
            } else {
                save(elected);
            }
        }
    }

    private Elected createNewElected(FilebotExecution filebotExecution, String nameOfTheShowOrFilm) {
        Elected elected = new Elected();
        elected.setFilebotExecutionODTO(modelMapper.map(filebotExecution, FilebotExecutionODTO.class));
        elected.setName(nameOfTheShowOrFilm);
        elected.setTimes(1);
        return elected;
    }
}
