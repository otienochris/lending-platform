package ke.co.interviewusercaseworld.msorchestrator.mappers;

import ke.co.interviewusercaseworld.msorchestrator.model.dto.response.SagaStepQueryResponse;
import ke.co.interviewusercaseworld.msorchestrator.model.entities.SagaStep;
import org.springframework.beans.factory.annotation.Autowired;

import static ke.co.interviewusercaseworld.commons.utils.Helpers.parseToObject;

public class SagaStepMapperDecorator implements SagaStepMapper {

    @Autowired
    private SagaStepMapper sagaStepMapper;

    @Override
    public SagaStepQueryResponse toDto(SagaStep sagaStep) {
        SagaStepQueryResponse dto = sagaStepMapper.toDto(sagaStep);
        dto.setOutcome(parseToObject(sagaStep.getOutcome()));
        return dto;
    }
}
