package io.waves.WavesCryptoExchangerBot.model;

public enum ButtonNameEnum {
    GET_BUTTON("Получить"), BTC_TO_USDT_BUTTON("BTC на USDT"), USDT_TO_BTC_BUTTON("USDT на BTC");

    private final String buttonName;

    private ButtonNameEnum(String buttonName){
        this.buttonName = buttonName;
    }

    public String getButtonName(){
        return buttonName;
    }
}
