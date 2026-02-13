package ke.co.interviewusercaseworld.msorchestrator.mappers;

import ke.co.interviewusercaseworld.msorchestrator.model.dto.response.SagaQueryResponse;
import ke.co.interviewusercaseworld.msorchestrator.model.entities.Saga;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
@DecoratedWith(SagaMapperDecorator.class)
public interface SagaMapper {


    SagaQueryResponse toDto(Saga saga);
}
