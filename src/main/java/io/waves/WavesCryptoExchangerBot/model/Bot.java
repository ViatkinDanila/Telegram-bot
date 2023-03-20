package io.waves.WavesCryptoExchangerBot.model;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@Data
@PropertySource("application.properties")
public class Bot {

    @Value("${bot.name}")
    private String name;
    @Value("${bot.token}")
    private String token;
}
