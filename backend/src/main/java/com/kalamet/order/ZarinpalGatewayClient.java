package com.kalamet.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kalamet.config.KalametProperties;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

/**
 * Zarinpal REST API v4. Amounts are sent in Rial ({@code currency: IRR}). Codes: 100 success,
 * 101 already verified (treated as success, so a repeated callback is harmless).
 */
@Slf4j
@Component
class ZarinpalGatewayClient implements PaymentGatewayClient {

    private final RestClient restClient;
    private final KalametProperties.Payment.Zarinpal settings;

    ZarinpalGatewayClient(RestClient.Builder builder, KalametProperties properties) {
        this.settings = properties.payment().zarinpal();
        this.restClient = builder.baseUrl(settings.baseUrl()).build();
    }

    @Override
    public PaymentGateway gateway() {
        return PaymentGateway.ZARINPAL;
    }

    @Override
    public boolean enabled() {
        return settings.configured();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record StartRequest(@JsonProperty("merchant_id") String merchantId, long amount, String currency,
                        String description, @JsonProperty("callback_url") String callbackUrl,
                        Map<String, String> metadata) {
    }

    record VerifyRequest(@JsonProperty("merchant_id") String merchantId, long amount, String authority) {
    }

    @Override
    public StartResult start(long amount, String description, String mobile, String callbackUrl) {
        JsonNode response = post("/pg/v4/payment/request.json", new StartRequest(settings.merchantId(), amount,
                "IRR", description, callbackUrl, mobile == null ? null : Map.of("mobile", mobile)));
        JsonNode data = response.path("data");
        if (data.path("code").asInt() != 100) {
            throw new GatewayException("Zarinpal request failed: " + errorOf(response));
        }
        String authority = data.path("authority").asString();
        return new StartResult(authority, settings.baseUrl() + "/pg/StartPay/" + authority);
    }

    @Override
    public VerifyResult verify(String authority, long amount) {
        JsonNode response;
        try {
            response = post("/pg/v4/payment/verify.json", new VerifyRequest(settings.merchantId(), amount, authority));
        } catch (GatewayException ex) {
            return VerifyResult.failed(ex.getMessage());
        }
        JsonNode data = response.path("data");
        int code = data.path("code").asInt();
        if (code == 100 || code == 101) {
            return VerifyResult.ok(data.path("ref_id").asString(), data.path("card_pan").asString(null));
        }
        return VerifyResult.failed("Zarinpal verify failed: " + errorOf(response));
    }

    private JsonNode post(String path, Object body) {
        try {
            // exchange() instead of retrieve(): Zarinpal reports errors as JSON with 4xx codes.
            JsonNode response = restClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .exchange((request, reply) -> reply.bodyTo(JsonNode.class));
            if (response == null) {
                throw new GatewayException("Empty response from Zarinpal");
            }
            return response;
        } catch (RestClientException ex) {
            log.warn("Zarinpal call {} failed", path, ex);
            throw new GatewayException("Zarinpal unreachable: " + ex.getMessage());
        }
    }

    private static String errorOf(JsonNode response) {
        JsonNode errors = response.path("errors");
        if (errors.isObject()) {
            return errors.path("code").asString("?") + " " + errors.path("message").asString("");
        }
        return "code " + response.path("data").path("code").asString("?");
    }

    static class GatewayException extends RuntimeException {

        GatewayException(String message) {
            super(message);
        }
    }
}
