package ke.co.interviewusercaseworld.msorchestrator.controller;

import ke.co.interviewusercaseworld.commons.dto.responses.DefaultResponseHeader;
import ke.co.interviewusercaseworld.commons.dto.responses.GenericResponse;
import ke.co.interviewusercaseworld.commons.enums.SagaTypeEnum;
import ke.co.interviewusercaseworld.msorchestrator.model.dto.response.SagaQueryResponse;
import ke.co.interviewusercaseworld.msorchestrator.service.SagaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sagas")
public class SagaController {

    private final SagaService sagaService;

    @GetMapping("/{businessKey}")
    public Mono<ResponseEntity<GenericResponse<DefaultResponseHeader, List<SagaQueryResponse>>>> querySaga(@PathVariable UUID businessKey, @RequestParam SagaTypeEnum type) {
        return sagaService.findAllByTypeAndBusinessKey(type, businessKey)
                .map(res -> {
                    if (res.getHeader().getResponseCode().name().startsWith("RC_2")) {
                        return ResponseEntity.ok(res);
                    } else {
                        return ResponseEntity.badRequest().body(res);
                    }
                });
    }
}
