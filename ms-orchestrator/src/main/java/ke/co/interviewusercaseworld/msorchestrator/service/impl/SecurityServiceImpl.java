package ke.co.interviewusercaseworld.msorchestrator.service.impl;

import ke.co.interviewusercaseworld.commons.dto.requests.DefaultRequestHeader;
import ke.co.interviewusercaseworld.commons.dto.requests.GenericRequest;
import ke.co.interviewusercaseworld.commons.dto.responses.DefaultResponseHeader;
import ke.co.interviewusercaseworld.commons.dto.responses.GenericResponse;
import ke.co.interviewusercaseworld.commons.dto.responses.UserValidationResponse;
import ke.co.interviewusercaseworld.msorchestrator.configs.OrchestratorProperties;
import ke.co.interviewusercaseworld.msorchestrator.model.dto.request.LoanApplicationRequest;
import ke.co.interviewusercaseworld.msorchestrator.service.SecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.security.autoconfigure.SecurityProperties;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class SecurityServiceImpl implements SecurityService {

    private final OrchestratorProperties appProperties;

    @Override
    public Mono<GenericResponse<DefaultResponseHeader, UserValidationResponse>> validateUser(Map<String, String> headers, GenericRequest<DefaultRequestHeader, LoanApplicationRequest> request) {
        appProperties.getServiceProperties().getOrchestratorProperties().getExternalMicroServices().get("");
        return null;
    }
}
