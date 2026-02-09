package ke.co.interviewusercaseworld.msorchestrator.utils;

import ke.co.interviewusercaseworld.commons.configs.AppProperties;
import ke.co.interviewusercaseworld.commons.enums.AuthTypeEnum;
import ke.co.interviewusercaseworld.commons.enums.LogLevelEnum;
import ke.co.interviewusercaseworld.commons.enums.OperationNameEnum;
import ke.co.interviewusercaseworld.commons.utils.Helpers;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class GlobalHelpers {
    public static Mono<String> executeRequest(String requestRefId, OperationNameEnum operation, Map<String, String> headers, String body, AppProperties.ApiSpec apiSpec, WebClient webClient) {

        Helpers.log(requestRefId, LogLevelEnum.info, operation, "Initiating api call for " + apiSpec.getUrl(), null);
        Map<String, String> defaultHeaders = apiSpec.getDefaultHeaders();
        defaultHeaders.putAll(headers);


        return webClient.post()
                .uri(apiSpec.getUrl())
                .headers(httpHeaders -> defaultHeaders.forEach(httpHeaders::add))
                .body(BodyInserters.fromValue(body))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(error -> {
                                            Helpers.log(requestRefId, LogLevelEnum.error, operation, "Error performing api call for " + apiSpec.getUrl(), new RuntimeException(error));
                                            return Mono.error(new RuntimeException("OAuth error: " + error));
                                        }
                                )
                )
                .bodyToMono(String.class)
                .doOnSuccess(response -> Helpers.log(requestRefId, LogLevelEnum.info, operation, "Successfully performed api call for  " + apiSpec.getUrl(), null));
    }

    public static Mono<String> executeAuthRequest(String requestRefId, OperationNameEnum operation, Map<String, String> headers, String body, AppProperties.AuthApiSpec authApiSpec, WebClient webClient) {
        Helpers.log(requestRefId, LogLevelEnum.info, operation, "Initiating api call for " + authApiSpec.getUrl(), null);

        if (authApiSpec == null) {
            return Mono.empty();
        }

        AuthTypeEnum authType = authApiSpec.getAuthType();

        Map<String, String> defaultHeaders = authApiSpec.getDefaultHeaders();
        defaultHeaders.putAll(headers);

        BodyInserter with = switch (authType) {
            case OAUTH -> BodyInserters.fromFormData("grant_type", "password")
                    .with("username", authApiSpec.getUsername())
                    .with("password", authApiSpec.getPassword());
            case BASIC, BEARER -> BodyInserters.fromValue(body);
        };

        return webClient.post()
                .uri(authApiSpec.getUrl())
                .headers(httpHeaders -> defaultHeaders.forEach(httpHeaders::add))
                .body(with)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(error -> {
                                            Helpers.log(requestRefId, LogLevelEnum.error, operation, "Error performing api call for authentication " + authApiSpec.getUrl(), new RuntimeException(error));
                                            return Mono.error(new RuntimeException("Api call failed " + error));
                                        }
                                )
                )
                .bodyToMono(String.class)
                .doOnSuccess(response -> Helpers.log(requestRefId, LogLevelEnum.info, operation, "Successfully performed api call for authentication " + authApiSpec.getUrl(), null));
    }
}
