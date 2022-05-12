package es.lavanda.filebot.bot.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import es.lavanda.filebot.bot.model.FilebotPreselection;
import es.lavanda.filebot.bot.repository.FilebotPreselectionRepository;
import es.lavanda.filebot.bot.service.FilebotPreselectionService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FilebotPreselectionServiceImpl implements FilebotPreselectionService {

    @Autowired
    private FilebotPreselectionRepository filebotPreselectionRepository;

    @Override
    public List<String> getPreselection() {
        return filebotPreselectionRepository.findAll().stream()
                .map(filebotPreselection -> filebotPreselection.getPreselection())
                .collect(Collectors.toList());
    }

    @Override
    public void setPreselection(String preselection) {
        Optional<FilebotPreselection> optionalPreselection = filebotPreselectionRepository
                .findByPreselection(preselection);
        optionalPreselection.ifPresentOrElse(arg0 -> {
            log.info("Preselection already exists: {}", arg0);
        });
        List<FilebotPreselection> filebotPreselections = filebotPreselectionRepository.findAll();
        for (FilebotPreselection filebotPreselection : filebotPreselections) {
            if (Boolean.FALSE.equals(filebotPreselection.getPreselection().equalsIgnoreCase(preselection))) {
                int count = filebotPreselection.getCount() - 1;
                if (count <= 0) {
                    filebotPreselectionRepository.delete(filebotPreselection);
                } else {
                    filebotPreselection.setCount(count);
                    filebotPreselectionRepository.save(filebotPreselection);
                }
            }
        }
    }
}
