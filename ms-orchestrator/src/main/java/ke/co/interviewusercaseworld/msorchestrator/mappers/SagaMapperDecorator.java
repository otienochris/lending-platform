package ke.co.interviewusercaseworld.msorchestrator.mappers;

import ke.co.interviewusercaseworld.msorchestrator.model.dto.response.SagaQueryResponse;
import ke.co.interviewusercaseworld.msorchestrator.model.entities.Saga;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import static ke.co.interviewusercaseworld.commons.utils.Helpers.parseToObject;

public class SagaMapperDecorator implements SagaMapper {

    @Autowired
    @Qualifier("delegate")
    private SagaMapper sagaMapper;

    @Override
    public SagaQueryResponse toDto(Saga saga) {
        SagaQueryResponse dto = sagaMapper.toDto(saga);
        dto.setOriginalRequest(parseToObject(saga.getOriginalRequest()));
        return dto;
    }
}
