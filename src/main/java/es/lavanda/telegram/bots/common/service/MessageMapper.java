package es.lavanda.telegram.bots.common.service;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;

import es.lavanda.lib.common.model.QbittorrentModel;
import es.lavanda.telegram.bots.classify.model.Qbittorrent;
import es.lavanda.telegram.bots.common.model.TelegramMessage;

@Mapper(componentModel = "spring")
@Service
public interface MessageMapper {

    MessageMapper INSTANCE = Mappers.getMapper(MessageMapper.class);

    SendMessage messageToSendMessage(TelegramMessage message);

    SendPhoto messageToSendPhoto(TelegramMessage message);

    EditMessageText messageToEditMessage(TelegramMessage message);

    DeleteMessage messageToDeleteMessage(TelegramMessage message);

    Qbittorrent qbittorrentModelToQbittorrent(QbittorrentModel qbittorrentModel);

    QbittorrentModel qbittorrentToQbittorrentModel(Qbittorrent qbittorrent);

}