package es.lavanda.filebot.bot.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import es.lavanda.filebot.bot.exception.FilebotParserException;
import es.lavanda.filebot.bot.model.Filebot;
import es.lavanda.filebot.bot.model.FilebotNameSelection;
import es.lavanda.filebot.bot.repository.FilebotFileRepository;
import es.lavanda.filebot.bot.repository.FilebotRepository;
import es.lavanda.filebot.bot.util.FilebotParser;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FilebotService implements CommandLineRunner {

    @Autowired
    private FilebotRepository filebotRepository;

    @Autowired
    private FilebotFileRepository filebotFileRepository;

    @Autowired
    private FilebotParser filebotParser;

    @Value("${filebot.path}")
    private String FILEBOT_PATH;

    private String getHtmlData(String filePath) {
        try {
            return Files.readString(Path.of(filePath));
        } catch (IOException e) {
            log.error("Can not access to path {}", FILEBOT_PATH, e);
            throw new FilebotParserException("Can not access to path", e);
        }
    }

    private List<FilebotNameSelection> getAllFilesFounded(String path) {
        try (Stream<Path> walk = Files.walk(Paths.get(path))) {

            return walk.filter(Files::isRegularFile).map(filePath -> new FilebotNameSelection(filePath.toString()))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Can not access to path {}", FILEBOT_PATH, e);
            throw new FilebotParserException("Can not access to path", e);
        }
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Start schedule parse new files");
        List<FilebotNameSelection> newFiles = getAllFilesFounded(FILEBOT_PATH);
        List<FilebotNameSelection> oldFiles = (List<FilebotNameSelection>) filebotFileRepository.findAll();
        newFiles.removeAll(oldFiles);
        newFiles.forEach(file -> {
            log.info("Parsing new file {}", file.getFilePath());
            List<Filebot> filebots = filebotParser.parseHtml(getHtmlData(file.getFilePath()));
            filebotRepository.saveAll(filebots);
            filebotFileRepository.save(file);
        });
        log.info("Finish schedule parse new files");
    }
}