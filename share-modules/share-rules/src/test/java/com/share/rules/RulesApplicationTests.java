package com.share.rules;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RulesApplicationTests {
    @Autowired
    private KieContainer kieContainer;
    @Test
    void contextLoads() {
            KieSession session=kieContainer.newKieSession();

            session.fireAllRules();
            session.dispose();

    }

}
