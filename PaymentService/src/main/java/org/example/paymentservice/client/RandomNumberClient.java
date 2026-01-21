package org.example.paymentservice.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class RandomNumberClient {

    private final RandomApiClient randomApiClient;
    private final Random fallbackRandom = new Random();

    public int getRandomNumber() {
        try {
            String response = randomApiClient.getRandomNumber();
            return Integer.parseInt(response.trim());
        } catch (Exception e) {
            log.warn("Failed to get random number from external API, using fallback", e);
            return fallbackRandom.nextInt(100) + 1;
        }
    }

    public boolean isEven(int number) {
        return number % 2 == 0;
    }
}