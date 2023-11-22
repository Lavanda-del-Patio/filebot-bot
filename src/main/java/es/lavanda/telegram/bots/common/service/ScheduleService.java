package es.lavanda.telegram.bots.common.service;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@EnableScheduling
public class ScheduleService {

    // private final ClassifyService classifyService;

    // @Scheduled(cron = "0 0 * * * *")
    // public void executeSchedule() {
    //     classifyService.processNotProcessing();
    // }

}