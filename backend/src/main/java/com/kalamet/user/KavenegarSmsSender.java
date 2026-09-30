package com.kalamet.user;

import com.kalamet.common.ApiException;
import com.kalamet.common.MobileNumber;
import com.kalamet.config.KalametProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Sends codes through Kavenegar's "verify lookup" API, which fills a pre-approved template
 * (for example "کد ورود شما به کالامت: %token").
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "kalamet.sms.provider", havingValue = "KAVENEGAR")
class KavenegarSmsSender implements SmsSender {

    private final RestClient restClient;
    private final KalametProperties.Sms.Kavenegar settings;

    KavenegarSmsSender(RestClient.Builder builder, KalametProperties properties) {
        this.settings = properties.sms().kavenegar();
        if (settings.apiKey() == null || settings.apiKey().isBlank()) {
            throw new IllegalStateException("kalamet.sms.kavenegar.api-key (KAVENEGAR_API_KEY) is required");
        }
        this.restClient = builder.baseUrl(settings.baseUrl()).build();
    }

    @Override
    public void sendLoginCode(String mobile, String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("receptor", mobile);
        form.add("token", code);
        form.add("template", settings.template());
        try {
            restClient.post()
                    .uri("/v1/{apiKey}/verify/lookup.json", settings.apiKey())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            log.error("Kavenegar failed for {}", MobileNumber.mask(mobile), ex);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "SMS_FAILED",
                    "ارسال پیامک با خطا مواجه شد. لطفاً چند لحظه دیگر تلاش کنید.");
        }
    }
}
