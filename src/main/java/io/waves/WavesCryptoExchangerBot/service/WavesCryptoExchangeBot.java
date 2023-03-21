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
import java.util.HashMap;
import java.util.Optional;

@Slf4j
@Service
public class WavesCryptoExchangeBot extends TelegramLongPollingBot {

    private final Bot bot;
    private final String BTCAssetId = "8LQW8f7P5d5PZM7GtZEBgaqRPGSzS3DfPuiXrURJ4AJS";
    private final String USDTAssetId = "34N9YcEETLWn93qYQ64EsP1x89tSruJU44RrEMSXXEPJ";
    private final String BTC = " BTC";
    private final String USDT = " USDT";

    private HashMap<Long, String> chatTrades = new HashMap<>();

    private final String rateUrlString = "https://matcher.waves.exchange/matcher/orderbook/" +
            BTCAssetId + "/" +
            USDTAssetId + "/" +
            "status";

    private final String usdtToBtcPaymentUrl = "https://waves.exchange/withdraw/BTC";

    private final String btcToUsdtPaymentUrl = "https://waves.exchange/withdraw/USDT";

    private final String walletAddress = "3P7HYnjWHoykYxpiTgg6iEAx825zRTJy1vC";

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
                        sellBtcCommandReceived(chatId);
                    } catch (TelegramApiException e) {
                        log.error("Error occurred: " + e.getMessage());
                    }
                    break;
                case "USDT на BTC":
                    try{
                        sellUsdtCommandReceived(chatId);
                    } catch (TelegramApiException e){
                        log.error("Error occurred: " + e.getMessage());
                    }
                    break;
                default:
                    if (!chatTrades.get(chatId).isEmpty()){
                        try {
                            double amount = 0;

                            try{
                                amount = Double.parseDouble(message);
                                makePaymentRequest(chatId, amount);
                            } catch (NumberFormatException e){
                                sendMessage(chatId, "Enter the number, please");
                            }

                            if (amount != 0){
                                makePaymentRequest(chatId, amount);
                            } else {
                            }
                        } catch (TelegramApiException e) {
                            log.error("Error occurred: " + e.getMessage());
                        }
                    } else {
                        try {
                            sendMessage(chatId, "Sorry, this command is not supported yet");
                        } catch (TelegramApiException e) {
                            log.error("Error occureed: " + e.getMessage());
                        }
                    }

            }
        }
    }
    private void startCommandReceived(long chatId, String userName) throws TelegramApiException{
        String answer = "Hi, " + userName + ", welcome to Waves Exchanger Crypto bot.";
        sendMessage(chatId, answer);
    }
    private void rateCommandReceived(long chatId) throws TelegramApiException, ScriptException {
        sendMessage(chatId, makeRateMessage(makeRateRequest().get()).toString());
    }

    private StringBuilder makeRateMessage(StringBuilder rate){
        return new StringBuilder("The current rate BTC/USDT: " + rate);
    }

    private StringBuilder normalizeRate(StringBuilder rate){
        return rate.delete(rate.length()-7, rate.length()-1);
    }

    private void sellBtcCommandReceived(long chatId) throws TelegramApiException {
        chatTrades.put(chatId, USDT);
        String message = "Enter amount of BTC changing to USDT";
        sendMessage(chatId, message);
    }

    private void sellUsdtCommandReceived(long chatId) throws TelegramApiException {
        chatTrades.put(chatId, BTC);
        String message = "Enter amount of USDT changing to BTC";
        sendMessage(chatId, message);
    }

    private void makePaymentRequest(long chatId, double amount) throws TelegramApiException{
        StringBuilder rate = makeRateRequest().get();
        double rateDouble = Double.parseDouble(rate.toString());
        StringBuilder message = makeRateMessage(rate);
        String asset = chatTrades.get(chatId);
        message.append("\nYou'll receive ");
        if (chatTrades.get(chatId).equals(BTC)){
            message.append(amount/rateDouble);
            message.append(asset);
            message.append("\nWaves.Exchange payment available by this link: ");
            message.append(usdtToBtcPaymentUrl);
        } else {
            message.append(amount * rateDouble);
            message.append(asset);
            message.append("\nWaves.Exchange payment available by this link: ");
            message.append(btcToUsdtPaymentUrl);
        }
        message.append("\nWallet address to deposit " + asset + " on Waves.Exchange is :\n" + walletAddress);
        sendMessage(chatId, message.toString());
        chatTrades.remove(chatId);
    }

    private Optional<StringBuilder> makeRateRequest(){
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
        Optional<StringBuilder> rateOptional = Optional.empty();
        if(rate != null){
            rateOptional = Optional.of(normalizeRate(rate));
        }
        return rateOptional;
    }

    private void sendMessage(long chatId, String textToSend) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(textToSend);
        message.setReplyMarkup(mainMenuMaker.getMainMenuKeyboard());
        execute(message);
    }


}
