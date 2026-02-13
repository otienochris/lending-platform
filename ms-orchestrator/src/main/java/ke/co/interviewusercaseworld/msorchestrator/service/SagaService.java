package ke.co.interviewusercaseworld.msorchestrator.service;

import ke.co.interviewusercaseworld.commons.dto.responses.DefaultResponseHeader;
import ke.co.interviewusercaseworld.commons.dto.responses.GenericResponse;
import ke.co.interviewusercaseworld.commons.enums.SagaTypeEnum;
import ke.co.interviewusercaseworld.msorchestrator.model.dto.response.SagaQueryResponse;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface SagaService {
    Mono<GenericResponse<DefaultResponseHeader, List<SagaQueryResponse>>> findAllByTypeAndBusinessKey(SagaTypeEnum type, UUID sagaId);
}
