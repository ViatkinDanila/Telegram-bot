package io.waves.WavesCryptoExchangerBot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.waves.WavesCryptoExchangerBot.keyboard.ReplyKeyboardMaker;
import io.waves.WavesCryptoExchangerBot.model.Bot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.script.ScriptException;

@Slf4j
@Service
public class WavesCryptoExchangeBot extends TelegramLongPollingBot {

    private final Bot bot;
    private String BTCAssetId = "8LQW8f7P5d5PZM7GtZEBgaqRPGSzS3DfPuiXrURJ4AJS";
    private String USDTAssetId = "34N9YcEETLWn93qYQ64EsP1x89tSruJU44RrEMSXXEPJ";

    private final String rateUrlString = "https://matcher.waves.exchange/matcher/orderbook/" +
            BTCAssetId + "/" +
            USDTAssetId + "/" +
            "status";

    private final String usdtToBtcPaymentUrl = "https://waves.exchange/#send/" +
            USDTAssetId + "?recipient=3P7HYnjWHoykYxpiTgg6iEAx825zRTJy1vC&amount=100";

    private final String btcToUsdtPaymentString = "https://waves.exchange/#send/" +
            BTCAssetId + "?recipient=3P7HYnjWHoykYxpiTgg6iEAx825zRTJy1vC&amount=1";

    private final ReplyKeyboardMaker mainMenuMaker;

    @Autowired
    public WavesCryptoExchangeBot(Bot bot,
                                  ReplyKeyboardMaker mainMenuMaker){
        this.bot = bot;
        this.mainMenuMaker = mainMenuMaker;
    }

    @Override
    public String getBotUsername(){
        return bot.getName();
    }

    @Override
    public String getBotToken(){
        return bot.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()){
            String message = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            switch (message) {
                case "/start":
                    try {
                        startCommandReceived(chatId, update.getMessage().getChat().getUserName());
                    } catch (TelegramApiException e) {
                        log.error("Error occurred: " + e.getMessage());
                    }
                    break;
                case "Получить":
                    try {
                        rateCommandReceived(chatId);
                    } catch (TelegramApiException | ScriptException e) {
                        log.error("Error occurred: " + e.getMessage());
                    }
                    break;
                case "BTC на USDT":

                    try {
                        buyBtcCommandReceived(chatId);
                    } catch (TelegramApiException e) {
                        log.error("Error occurred: " + e.getMessage());
                    }
                    break;
                case "USDT на BTC":
                    try{
                        buyUsdtCommandReceived(chatId);
                    } catch (TelegramApiException e){
                        log.error("Error occurred: " + e.getMessage());
                    }
                    break;
                default:
                    try {
                        sendMessage(chatId, "Sorry, this command is not supported yet");
                    } catch (TelegramApiException e) {
                        log.error("Error occurred: " + e.getMessage());
                    }
            }
        }
    }
    private void startCommandReceived(long chatId, String userName) throws TelegramApiException{
        String answer = "Hi, " + userName + ", welcome to Waves Exchanger Crypto bot.";
        sendMessage(chatId, answer);
    }
    private void rateCommandReceived(long chatId) throws TelegramApiException, ScriptException {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity(rateUrlString, String.class);
        String body = response.getBody();
        ObjectMapper mapper = new ObjectMapper();
        StringBuilder rate = null;
        try {
            JsonNode rootNode = mapper.readTree(body);
            rate = new StringBuilder(rootNode.get("lastPrice").asText());
        } catch (JsonProcessingException e) {
            log.error("Error occurred: " + e.getMessage());
        }
        sendMessage(chatId, makeRateMessage(rate));
    }

    private String makeRateMessage(StringBuilder rate){
        rate.delete(rate.length() - 7, rate.length()-1);
        return "The current rate of 1 BTC: " + rate + " USDT";
    }

    private void buyBtcCommandReceived(long chatId) throws TelegramApiException {
        String message = "Please click on this link to buy BTC: " + btcToUsdtPaymentString;
        sendMessage(chatId, message);
    }

    private void buyUsdtCommandReceived(long chatId) throws TelegramApiException {
        String message = "Please click on this link to buy USDT: " + usdtToBtcPaymentUrl;
        sendMessage(chatId, message);
    }

    private void sendMessage(long chatId, String textToSend) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(textToSend);
        message.setReplyMarkup(mainMenuMaker.getMainMenuKeyboard());
        execute(message);
    }


}
