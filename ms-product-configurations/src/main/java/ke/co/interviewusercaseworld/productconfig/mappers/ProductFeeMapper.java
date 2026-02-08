package ke.co.interviewusercaseworld.productconfig.mappers;

import ke.co.interviewusercaseworld.commons.dto.responses.DefaultResponseHeader;
import ke.co.interviewusercaseworld.commons.dto.responses.GenericResponse;
import ke.co.interviewusercaseworld.productconfig.model.dto.request.FeeRequestDto;
import ke.co.interviewusercaseworld.productconfig.model.dto.response.FeeResponseDto;
import ke.co.interviewusercaseworld.productconfig.model.entity.ProductFee;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductFeeMapper {
    FeeResponseDto toDto(ProductFee productFee);
    ProductFee toEntity(FeeRequestDto feeResponseDto);
}
