package es.lavanda.telegram.bots.filebot.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import es.lavanda.lib.common.model.tmdb.search.TMDBResultDTO;

public class TelegramUtils {

    public static String BACK_BUTTON_VALUE = "🔙 Atrás 🔙";
    public static String BACK_BUTTON_KEY = "back";

    public static InlineKeyboardMarkup getInlineKeyboard(List<String> list, boolean columns) {
        return getInlineKeyboard(list, list, columns);
    }

    public static InlineKeyboardMarkup getInlineKeyboard(List<String> data, List<String> callbackData,
            boolean columns) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        if (columns) {
            // Crea una sola fila con todos los botones (X columnas)
            List<InlineKeyboardButton> rowInline = new ArrayList<>();
            for (int i = 0; i < data.size(); i++) {
                InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
                inlineKeyboardButton.setText(data.get(i));
                inlineKeyboardButton.setCallbackData(callbackData.get(i));
                rowInline.add(inlineKeyboardButton);
            }
            rowsInline.add(rowInline);
        } else {
            // Crea múltiples filas, cada una con un solo botón (X filas)
            for (int i = 0; i < data.size(); i++) {
                List<InlineKeyboardButton> rowInline = new ArrayList<>();
                InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
                inlineKeyboardButton.setText(data.get(i));
                inlineKeyboardButton.setCallbackData(callbackData.get(i));
                rowInline.add(inlineKeyboardButton);
                rowsInline.add(rowInline);
            }
        }
        inlineKeyboardMarkup.setKeyboard(rowsInline);
        return inlineKeyboardMarkup;
    }

    public static InlineKeyboardMarkup getEmptyInlineKeyboard() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        rowsInline.add(rowInline);
        inlineKeyboardMarkup.setKeyboard(rowsInline);
        return inlineKeyboardMarkup;
    }

    public static InlineKeyboardMarkup getInlineKeyboardForChoices(Map<String, TMDBResultDTO> results, boolean isFilm) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        for (Entry<String, TMDBResultDTO> result : results.entrySet()) {
            List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();
            InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
            if (isFilm) {
                inlineKeyboardButton
                        .setText(result.getValue().getTitle() + " (" + result.getValue().getReleaseDate() + ")");
            } else {
                inlineKeyboardButton
                        .setText(result.getValue().getName() + " (" + result.getValue().getFirstAirDate() + ")");
            }
            inlineKeyboardButton.setCallbackData(result.getKey());
            InlineKeyboardButton moreData = new InlineKeyboardButton();
            moreData.setText("ℹ️ Info");
            moreData.setCallbackData("data" + result.getKey());
            keyboardButtonsRow1.add(inlineKeyboardButton);
            keyboardButtonsRow1.add(moreData);
            rowsInline.add(keyboardButtonsRow1);
        }
        List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
        inlineKeyboardButton.setText("Ninguno de los anteriores");
        inlineKeyboardButton.setCallbackData("0");
        keyboardButtonsRow1.add(inlineKeyboardButton);
        rowsInline.add(keyboardButtonsRow1);
        inlineKeyboardMarkup.setKeyboard(rowsInline);
        return inlineKeyboardMarkup;
    }

    public static String abbreviate(String str, int size) {
        if (str.length() <= size)
            return str;
        int index = str.lastIndexOf(' ', size);
        if (index <= -1)
            return "";
        return str.substring(0, index);
    }

}
