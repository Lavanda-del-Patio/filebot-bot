package es.lavanda.filebot.bot.repository;

import org.springframework.stereotype.Repository;

import es.lavanda.filebot.bot.model.TelegramMessage;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

@Repository
public interface TelegramMessageRepository extends MongoRepository<TelegramMessage, String> {

    List<TelegramMessage> findAllByChatId(String chatId);

    Optional<TelegramMessage> findByChatIdOrderByIdDesc(String chatId);

    void deleteAllByChatId(String chatId);

}
