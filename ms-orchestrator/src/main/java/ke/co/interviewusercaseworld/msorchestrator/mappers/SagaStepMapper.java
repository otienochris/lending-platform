package ke.co.interviewusercaseworld.msorchestrator.mappers;

import ke.co.interviewusercaseworld.msorchestrator.model.dto.response.SagaStepQueryResponse;
import ke.co.interviewusercaseworld.msorchestrator.model.entities.SagaStep;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
@DecoratedWith(SagaStepMapperDecorator.class)
public interface SagaStepMapper {
    SagaStepQueryResponse toDto(SagaStep sagaStep);
}
