package io.waves.WavesCryptoExchangerBot.keyboard;

import io.waves.WavesCryptoExchangerBot.model.ButtonNameEnum;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.Arrays;

@Component
public class ReplyKeyboardMaker {
    public ReplyKeyboardMarkup getMainMenuKeyboard(){
        KeyboardRow row = new KeyboardRow();
        //TODO взять из енама значения
        row.add(new KeyboardButton(ButtonNameEnum.GET_BUTTON.getButtonName()));
        row.add(new KeyboardButton(ButtonNameEnum.BTC_TO_USDT_BUTTON.getButtonName()));
        row.add(new KeyboardButton(ButtonNameEnum.USDT_TO_BTC_BUTTON.getButtonName()));

        final ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setKeyboard(Arrays.asList(row));
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);
        return replyKeyboardMarkup;
    }

}
