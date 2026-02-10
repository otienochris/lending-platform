package ke.co.interviewusercaseworld.repayment.mappers;

import ke.co.interviewusercaseworld.repayment.model.dto.response.LoanQueryResponseDto;
import ke.co.interviewusercaseworld.repayment.model.entities.Loan;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LoanMapper {

    Loan toEntity(LoanQueryResponseDto dto);
    LoanQueryResponseDto toDto(Loan entity);
}
