package ke.co.interviewusercaseworld.productconfig.mappers;

import ke.co.interviewusercaseworld.commons.dto.responses.ProductFeeResponseDto;
import ke.co.interviewusercaseworld.productconfig.model.dto.request.FeeRequestDto;
import ke.co.interviewusercaseworld.productconfig.model.entity.ProductFee;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductFeeMapper {
    ProductFeeResponseDto toDto(ProductFee productFee);
    ProductFee toEntity(FeeRequestDto feeResponseDto);
}
