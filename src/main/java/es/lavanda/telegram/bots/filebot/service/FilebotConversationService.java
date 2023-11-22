package es.lavanda.telegram.bots.filebot.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import es.lavanda.telegram.bots.filebot.exception.FilebotException;
import es.lavanda.telegram.bots.filebot.model.FilebotConversation;
import es.lavanda.telegram.bots.filebot.model.FilebotConversation.FilebotConversationStatus;
import es.lavanda.telegram.bots.filebot.repository.FilebotConversationRepository;

@Service
public class FilebotConversationService {

    @Autowired
    private FilebotConversationRepository filebotConversationRepository;

    public FilebotConversation getFilebotConversation(String chatId) {
        return filebotConversationRepository.findByChatId(chatId).orElseThrow(() -> new FilebotException("Not found"));
    }

    public List<FilebotConversation> getFilebotConversations() {
        return filebotConversationRepository.findAll();
    }

    public FilebotConversation save(FilebotConversation filebotConversation) {
        return filebotConversationRepository.save(filebotConversation);
    }

    public Optional<FilebotConversation> findByChatId(String chatId) {
        return filebotConversationRepository.findByChatId(chatId);
    }

    public List<FilebotConversation> findAllByConversationStatus(
            FilebotConversationStatus filebotConversationStatus) {
        return filebotConversationRepository.findAllByConversationStatus(filebotConversationStatus);
    }

    public FilebotConversation findByConversationStatus(FilebotConversationStatus filebotConversationStatus) {
        return filebotConversationRepository.findByConversationStatus(filebotConversationStatus);
    }

}
