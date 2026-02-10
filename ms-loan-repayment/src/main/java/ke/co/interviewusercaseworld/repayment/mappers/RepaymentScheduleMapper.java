package ke.co.interviewusercaseworld.repayment.mappers;

import ke.co.interviewusercaseworld.repayment.model.dto.response.RepaymentScheduleResponseDto;
import ke.co.interviewusercaseworld.repayment.model.entities.RepaymentSchedule;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RepaymentScheduleMapper {

    RepaymentSchedule toEntity(RepaymentScheduleResponseDto dto);
    RepaymentScheduleResponseDto toDto(RepaymentSchedule entity);
}
