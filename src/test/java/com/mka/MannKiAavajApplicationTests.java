package com.mka;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MannKiAavajApplicationTests {

    @org.springframework.beans.factory.annotation.Autowired
    private com.mka.config.openai.OpenAIProperties openAIProperties;

    @org.springframework.beans.factory.annotation.Autowired
    private com.mka.client.openai.OpenAIClient openAIClient;

    @Test
    void contextLoads() {

    }
}
