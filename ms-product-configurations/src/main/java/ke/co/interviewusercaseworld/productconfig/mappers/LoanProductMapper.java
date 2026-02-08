package ke.co.interviewusercaseworld.productconfig.mappers;

import ke.co.interviewusercaseworld.commons.entities.product.LoanProduct;
import ke.co.interviewusercaseworld.productconfig.mappers.decorators.LoanProductMapperDecorator;
import ke.co.interviewusercaseworld.productconfig.model.dto.request.LoanProductCreationRequest;
import ke.co.interviewusercaseworld.productconfig.model.dto.response.LoanProductCreationResponseDto;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
@DecoratedWith(LoanProductMapperDecorator.class)
public interface LoanProductMapper {

    @Mapping(target = "tenureOptions", ignore = true)
    LoanProduct toEntity(LoanProductCreationRequest loanProductCreationRequest);

    @Mapping(target = "tenureOptions", ignore = true)
    LoanProductCreationResponseDto toDto(LoanProduct loanProduct);
}
