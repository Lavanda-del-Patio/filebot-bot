// package es.lavanda.telegram.bots.classify.service;

// import java.util.ArrayList;
// import java.util.List;
// import java.util.Objects;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.context.annotation.Scope;
// import org.springframework.stereotype.Service;
// import org.springframework.util.StringUtils;
// import org.telegram.telegrambots.meta.api.methods.ParseMode;
// import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
// import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
// import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
// import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
// import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
// import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
// import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
// import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

// import es.lavanda.lib.common.model.QbittorrentModel;
// import es.lavanda.lib.common.model.filebot.FilebotAction;
// import es.lavanda.lib.common.model.filebot.FilebotCategory;
// import es.lavanda.telegram.bots.classify.exception.ClassifyException;
// import es.lavanda.telegram.bots.classify.handler.ClassifyHandler;
// import es.lavanda.telegram.bots.classify.model.ClassifyConversation;
// import es.lavanda.telegram.bots.classify.model.ClassifyConversation.ClassifyConversationStatus;
// import es.lavanda.telegram.bots.classify.model.Qbittorrent;
// import es.lavanda.telegram.bots.classify.repository.ClassifyConversationRepository;
// import es.lavanda.telegram.bots.classify.repository.QbitorrentRepository;
// import es.lavanda.telegram.bots.common.model.TelegramMessage;
// import es.lavanda.telegram.bots.common.model.TelegramMessage.Handler;
// import es.lavanda.telegram.bots.common.model.TelegramMessage.MessageType;
// import es.lavanda.telegram.bots.common.service.MessageMapper;
// import es.lavanda.telegram.bots.common.service.ProducerService;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;

// @Service
// @Slf4j
// @Scope("singleton")
// @RequiredArgsConstructor
// public class ClassifyService {

//     private ClassifyHandler classifyHandler;

//     @Autowired
//     private MessageMapper messageMapper;

//     @Autowired
//     private ClassifyConversationRepository classifyConversationRepository;

//     @Autowired
//     private ProducerService producerService;

//     @Autowired
//     private QbitorrentRepository qbitorrentRepository;

//     public void newExecution(QbittorrentModel qbittorrentModel) {
//         if (Boolean.FALSE.equals(qbitorrentRepository.existsByName(qbittorrentModel.getName()))) {
//             Qbittorrent qbittorrent = messageMapper.qbittorrentModelToQbittorrent(qbittorrentModel);
//             qbitorrentRepository.save(qbittorrent);
//         }
//         processNotProcessing();
//     }

//     public void newConversation(String chatId, String name) {
//         classifyConversationRepository.findByChatId(chatId).ifPresentOrElse((classifyConversation) -> {
//             classifyConversation.setStatus(ClassifyConversationStatus.IDLE);
//             classifyConversationRepository.save(classifyConversation);
//             createSendMessageAndSendToRabbit("Reiniciado", chatId, false);
//         }, () -> {
//             ClassifyConversation classifyConversation = new ClassifyConversation();
//             classifyConversation.setChatId(chatId);
//             classifyConversation.setName(name);
//             classifyConversation.setStatus(ClassifyConversationStatus.IDLE);
//             classifyConversationRepository.save(classifyConversation);
//             processNotProcessing();
//         });
//     }

//     public void processNotProcessing() {
//         log.info("processNotProcessing whith model method...");
//         List<ClassifyConversation> classifyConversations = classifyConversationRepository
//                 .findAllByStatus(ClassifyConversationStatus.IDLE.toString());
//         List<Qbittorrent> qbittorrents = qbitorrentRepository.findAll();
//         if (qbittorrents.size() > 0) {
//             log.info("qBittorrents size: {}", qbittorrents.size());
//             log.info("ClassifyConversations size: {}", classifyConversations.size());
//             Qbittorrent qbittorrent = qbittorrents.get(0);
//             for (ClassifyConversation classifyConversation : classifyConversations) {
//                 log.info("Nombre de la conversación: {}", classifyConversation.getName());
//                 String messageId = null;
//                 messageId = sendMessageToSelectCategory(qbittorrent, classifyConversation.getChatId());
//                 // messageId = sendMessageWithPossibilities(telegramFilebotExecution,
//                 // classifyConversation.getChatId());
//                 classifyConversation.setStatus(ClassifyConversationStatus.WAITING_USER_RESPONSE_CATEGORY);
//                 classifyConversation.setInlineKeyboardMessageId(messageId);
//                 classifyConversation.setQbittorrentId(qbittorrent.getId());
//                 log.info("Saving telegram conversation with status {} and messageId {}",
//                         ClassifyConversationStatus.WAITING_USER_RESPONSE_CATEGORY,
//                         messageId);
//                 classifyConversationRepository.save(classifyConversation);
//             }
//         }
//     }

//     private String sendMessageToSelectCategory(Qbittorrent qbittorrent, String chatId) {
//         SendMessage sendMessageRequest = new SendMessage();
//         sendMessageRequest.setChatId(chatId);
//         sendMessageRequest.setText("Selecciona una categoría para: " + escapeTextForTelegram(qbittorrent.getName()));
//         // sendMessageRequest.setText(abbreviate(text, 3000));
//         sendMessageRequest.setParseMode(ParseMode.MARKDOWNV2);
//         sendMessageRequest.setReplyMarkup(getKeyboardRemove());
//         // sendMessageRequest.setSaveOnDatabase(saveOnDatabase);
//         // sendMessageRequest.setType(MessageType.TEXT);
//         ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
//         keyboardMarkup.setOneTimeKeyboard(true);
//         keyboardMarkup.setResizeKeyboard(true);
//         List<KeyboardRow> keyboard = new ArrayList<>();
//         KeyboardRow row = new KeyboardRow();
//         for (FilebotCategory category : FilebotCategory.values()) {
//             row.add(category.name());
//         }
//         keyboard.add(row);
//         keyboardMarkup.setKeyboard(keyboard);
//         sendMessageRequest.setReplyMarkup(keyboardMarkup);
//         return classifyHandler.sendMessage(sendMessageRequest);
//     }

//     private void createSendMessageAndSendToRabbit(String text, String chatId, boolean saveOnDatabase) {
//         TelegramMessage sendMessageRequest = new TelegramMessage();
//         sendMessageRequest.setChatId(chatId);
//         sendMessageRequest.setText(abbreviate(text, 3000));
//         sendMessageRequest.setReplyMarkup(getKeyboardRemove());
//         sendMessageRequest.setSaveOnDatabase(saveOnDatabase);
//         sendMessageRequest.setType(MessageType.TEXT);
//         sendMessageRequest.setHandler(Handler.CLASSIFY);
//         // sendMessage(sendMessageRequest);
//         producerService.sendTelegramMessage(sendMessageRequest);
//     }

//     private String abbreviate(String str, int size) {
//         if (str.length() <= size)
//             return str;
//         int index = str.lastIndexOf(' ', size);
//         if (index <= -1)
//             return "";
//         return str.substring(0, index);
//     }

//     private ReplyKeyboard getKeyboardRemove() {
//         ReplyKeyboardRemove replyKeyboardRemove = new ReplyKeyboardRemove();
//         replyKeyboardRemove.setSelective(true);
//         replyKeyboardRemove.setRemoveKeyboard(true);
//         return replyKeyboardRemove;
//     }

//     public void setClassifyHandler(ClassifyHandler classifyHandler) {
//         this.classifyHandler = classifyHandler;
//     }

//     public void sendMessage(TelegramMessage message) {
//         switch (message.getType()) {
//             case TEXT:
//                 sendMessageTelegramMessage(message);
//                 break;
//             case PHOTO:
//                 sendPhotoTelegramMessage(message);
//                 break;
//             case EDIT_MESSAGE:
//                 sendEditMessageTelegramMessage(message);
//                 break;
//             case DELETE_MESSAGE:
//                 sendDeleteMessageTelegramMessage(message);
//                 break;
//             default:
//                 break;
//         }
//     }

//     public void stopConversation(String chatId) {
//         ClassifyConversation telegramConversation = classifyConversationRepository.findByChatId(chatId).get();
//         telegramConversation.setStatus(ClassifyConversationStatus.STOPPED);
//         classifyConversationRepository.save(telegramConversation);
//         createSendMessageAndSendToRabbit("La conversación ha sido detenida", chatId, false);
//     }

//     public void handleCallbackResponse(String chatId, String messageId, String response) {
//         log.info("Handle callback message - NOTHING TO DO");
//         // classifyConversationRepository.findByStatusStartsWith("PROCESSING").ifPresent(
//         // (filebotNameSelection) -> {
//         // handleProcessing(filebotNameSelection, response, chatId, messageId);
//         // });
//     }

//     public void handleIncomingResponse(String chatId, String response) {
//         log.info("Handle incomming response with chatId {} ,and  {}", chatId, response);
//         List<ClassifyConversation> classifyConversations = classifyConversationRepository.findAllByStatusIn(
//                 List.of(ClassifyConversationStatus.WAITING_USER_RESPONSE_CATEGORY.toString(),
//                         ClassifyConversationStatus.WAITING_USER_RESPONSE_ACTION.toString()));
//         // for (ClassifyConversation classifyConversation : classifyConversations) {
//         if (classifyConversations.size() == 0) {
//             log.error("No hay conversaciones en proceso");
//             return;
//         } else {
//             ClassifyConversation classifyConversation = classifyConversations.get(0);
//             if (classifyConversation.getStatus().equals(ClassifyConversationStatus.WAITING_USER_RESPONSE_CATEGORY)) {
//                 Qbittorrent qbittorrent = qbitorrentRepository.findById(classifyConversation.getQbittorrentId())
//                         .orElseThrow(() -> new ClassifyException("Qbittorrent deleted already"));
//                 qbittorrent.setCategory(FilebotCategory.valueOf(response));
//                 qbitorrentRepository.save(qbittorrent);
//                 // A SELECCIONAR ACCION
//                 for (ClassifyConversation classifyConversationToAction : classifyConversations) {
//                     classifyConversationToAction.setStatus(ClassifyConversationStatus.IDLE);
//                     classifyConversationRepository.save(classifyConversationToAction);
//                     String messageId = sendMessageToSelectAction(classifyConversationToAction.getChatId());
//                     classifyConversationToAction.setStatus(ClassifyConversationStatus.WAITING_USER_RESPONSE_ACTION);
//                     classifyConversationToAction.setInlineKeyboardMessageId(messageId);
//                     log.info("Saving telegram conversation with status {} and messageId {}",
//                             ClassifyConversationStatus.WAITING_USER_RESPONSE_ACTION,
//                             messageId);
//                     classifyConversationRepository.save(classifyConversationToAction);
//                 }

//             } else {
//                 Qbittorrent qbittorrent = qbitorrentRepository.findById(classifyConversation.getQbittorrentId())
//                         .orElseThrow(() -> new ClassifyException("Qbittorrent deleted already"));
//                 qbittorrent.setAction(FilebotAction.valueOf(response));
//                 producerService
//                         .sendToFilebotExecutorResolution(messageMapper.qbittorrentToQbittorrentModel(qbittorrent));
//                 qbitorrentRepository.delete(qbittorrent);
//                 for (ClassifyConversation classifyConversationToAction : classifyConversations) {
//                     classifyConversationToAction.setStatus(ClassifyConversationStatus.IDLE);
//                     classifyConversationRepository.save(classifyConversationToAction);
//                 }
//                 processNotProcessing();
//             }
//         }
//         // }
//     }

//     private String sendMessageToSelectAction(String chatId) {
//         SendMessage sendMessageRequest = new SendMessage();
//         sendMessageRequest.setChatId(chatId);
//         sendMessageRequest.setText("¿Que accion quieres hacer?:");
//         // sendMessageRequest.setText(abbreviate(text, 3000));
//         sendMessageRequest.setReplyMarkup(getKeyboardRemove());
//         // sendMessageRequest.setSaveOnDatabase(saveOnDatabase);
//         // sendMessageRequest.set(MessageType.TEXT);
//         ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
//         keyboardMarkup.setOneTimeKeyboard(true);
//         keyboardMarkup.setResizeKeyboard(true);
//         List<KeyboardRow> keyboard = new ArrayList<>();
//         KeyboardRow row = new KeyboardRow();
//         for (FilebotAction action : FilebotAction.values()) {
//             row.add(action.name());
//         }
//         keyboard.add(row);
//         keyboardMarkup.setKeyboard(keyboard);
//         sendMessageRequest.setReplyMarkup(keyboardMarkup);
//         return classifyHandler.sendMessage(sendMessageRequest);
//     }

//     private void sendMessageTelegramMessage(TelegramMessage message) {
//         if (StringUtils.hasText(message.getText())) {
//             SendMessage toSend = messageMapper.messageToSendMessage(message);
//             toSend.setParseMode(ParseMode.MARKDOWNV2);
//             if (Boolean.TRUE.equals(message.isSaveOnDatabase())) {
//                 ClassifyConversation telegramConversation = classifyConversationRepository
//                         .findByChatId(message.getChatId())
//                         .get();
//                 if (Objects.nonNull(telegramConversation)) {
//                     String messageId = classifyHandler.sendMessage(toSend);
//                     telegramConversation.getOtherMessageIds().add(messageId);
//                     classifyConversationRepository.save(telegramConversation);
//                 }
//             } else {
//                 classifyHandler.sendMessage(toSend);
//             }
//         }
//     }

//     private void sendPhotoTelegramMessage(TelegramMessage message) {
//         SendPhoto toSend = messageMapper.messageToSendPhoto(message);
//         toSend.setParseMode(ParseMode.MARKDOWNV2);

//         if (Boolean.TRUE.equals(message.isSaveOnDatabase())) {
//             ClassifyConversation telegramConversation = classifyConversationRepository
//                     .findByChatId(message.getChatId())
//                     .get();
//             if (Objects.nonNull(telegramConversation)) {
//                 String messageId = classifyHandler.sendPhoto(toSend);
//                 telegramConversation.getOtherMessageIds().add(messageId);
//                 classifyConversationRepository.save(telegramConversation);
//             }
//         } else {
//             classifyHandler.sendPhoto(toSend);
//         }
//     }

//     private void sendEditMessageTelegramMessage(TelegramMessage message) {
//         EditMessageText toSend = messageMapper.messageToEditMessage(message);
//         toSend.setParseMode(ParseMode.MARKDOWNV2);
//         classifyHandler.sendEditMessage(toSend);
//     }

//     private void sendDeleteMessageTelegramMessage(TelegramMessage message) {
//         DeleteMessage toSend = messageMapper.messageToDeleteMessage(message);
//         classifyHandler.deleteMessage(toSend);
//     }

//     public void reset() {
//         List<ClassifyConversation> classifyConversations = classifyConversationRepository.findAll();
//         for (ClassifyConversation classifyConversation : classifyConversations) {
//             classifyConversation.setStatus(ClassifyConversationStatus.IDLE);
//             classifyConversationRepository.save(classifyConversation);
//             createSendMessageAndSendToRabbit("Reset status", classifyConversation.getChatId(), false);
//         }
//         processNotProcessing();
//     }

//     private String escapeTextForTelegram(String text) {
//         String[] specialChars = new String[] {
//                 "_", "*", "[", "]", "(", ")", "~", "`", ">", "#", "+", "-", "=", "|", "{", "}", ".", "!"
//         };
//         for (String ch : specialChars) {
//             text = text.replace(ch, "\\" + ch);
//         }

//         return text;
//     }

// }
