package es.lavanda.telegram.bots.common.service;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import es.lavanda.telegram.bots.filebot.service.FilebotService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@EnableScheduling
public class ScheduleService {

    private final FilebotService filebotService;

    // @Scheduled(cron = "0 0/15 * * * *")
    @Scheduled(fixedDelay = 60000)
    public void executeSchedule() {
        filebotService.processNotProcessing();
    }

}