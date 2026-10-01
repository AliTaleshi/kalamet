package com.kalamet.user.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Development sender: writes the code to the log instead of sending an SMS. */
@Slf4j
@Component
@ConditionalOnProperty(name = "kalamet.sms.provider", havingValue = "LOG", matchIfMissing = true)
class LogSmsSender implements SmsSender {

    @Override
    public void sendLoginCode(String mobile, String code) {
        log.info("Login code for {}: {}", mobile, code);
    }
}
