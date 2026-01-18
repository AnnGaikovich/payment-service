package org.example.paymentservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "randomApiClient", url = "https://www.random.org")
public interface RandomApiClient {

    @GetMapping("/integers/?num=1&min=1&max=100&col=1&base=10&format=plain&rnd=new")
    String getRandomNumber();
}