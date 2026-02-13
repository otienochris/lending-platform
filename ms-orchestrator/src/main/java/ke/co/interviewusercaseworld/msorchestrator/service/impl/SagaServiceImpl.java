package ke.co.interviewusercaseworld.msorchestrator.service.impl;

import ke.co.interviewusercaseworld.commons.dto.responses.DefaultResponseHeader;
import ke.co.interviewusercaseworld.commons.dto.responses.GenericResponse;
import ke.co.interviewusercaseworld.commons.enums.OperationNameEnum;
import ke.co.interviewusercaseworld.commons.enums.ResponseCodes;
import ke.co.interviewusercaseworld.commons.enums.SagaTypeEnum;
import ke.co.interviewusercaseworld.msorchestrator.mappers.SagaMapper;
import ke.co.interviewusercaseworld.msorchestrator.mappers.SagaStepMapper;
import ke.co.interviewusercaseworld.msorchestrator.model.dto.response.SagaQueryResponse;
import ke.co.interviewusercaseworld.msorchestrator.repository.SagaRepository;
import ke.co.interviewusercaseworld.msorchestrator.repository.SagaStepRepository;
import ke.co.interviewusercaseworld.msorchestrator.service.SagaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SagaServiceImpl implements SagaService {

    private final SagaRepository sagaRepository;
    private final SagaStepRepository sagaStepRepository;
    private final SagaMapper sagaMapper;
    private final SagaStepMapper sagaStepMapper;

    @Override
    public Mono<GenericResponse<DefaultResponseHeader, List<SagaQueryResponse>>> findAllByTypeAndBusinessKey(SagaTypeEnum type, UUID businessKey) {
        return sagaRepository.findAllBySagaTypeAndBusinessKey(type.name(), businessKey)
                .map(sagaMapper::toDto)
                .flatMap(sagaQueryResponse -> {
                    return sagaStepRepository.findAllBySagaId(sagaQueryResponse.getId())
                            .map(sagaStepMapper::toDto)
                            .collectList()
                            .defaultIfEmpty(List.of())
                            .map(sagaStepDtos -> {
                                sagaQueryResponse.setSteps(sagaStepDtos);
                                return sagaQueryResponse;

                            });
                })
                .collectList()
                .defaultIfEmpty(List.of())
                .onErrorResume(error -> Mono.just(List.of()))
                .flatMap(items -> {
                    if (items.isEmpty()) {
                        return Mono.just(GenericResponse.<DefaultResponseHeader, List<SagaQueryResponse>>builder()
                                .header(DefaultResponseHeader.builder()
                                        .correlationId(businessKey)
                                        .responseCode(ResponseCodes.RC_400)
                                        .responseRefId(businessKey.toString())
                                        .operation(OperationNameEnum.SAGA_QUERY)
                                        .debugMessage("No " + type.name() + " found with id: " + businessKey)
                                        .customerMessage("No saga found with id: " + businessKey)
                                        .build())
                                .build());
                    }


                    return Mono.just(GenericResponse.<DefaultResponseHeader, List<SagaQueryResponse>>builder()
                            .header(DefaultResponseHeader.builder()
                                    .correlationId(businessKey)
                                    .responseRefId(businessKey.toString())
                                    .responseCode(ResponseCodes.RC_200)
                                    .operation(OperationNameEnum.SAGA_QUERY)
                                    .debugMessage("Saga details retrieved successfully.")
                                    .customerMessage("Saga details retrieved successfully.")
                                    .build())
                            .body(items)
                            .build());
                });
    }
}
