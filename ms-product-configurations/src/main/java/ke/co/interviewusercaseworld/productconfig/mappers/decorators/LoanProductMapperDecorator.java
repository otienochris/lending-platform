package ke.co.interviewusercaseworld.productconfig.mappers.decorators;

import ke.co.interviewusercaseworld.commons.entities.product.LoanProduct;
import ke.co.interviewusercaseworld.productconfig.mappers.LoanProductMapper;
import ke.co.interviewusercaseworld.productconfig.model.dto.request.LoanProductCreationRequest;
import ke.co.interviewusercaseworld.productconfig.model.dto.response.LoanProductCreationResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Arrays;
import java.util.List;


public class LoanProductMapperDecorator implements LoanProductMapper {

    @Autowired
    @Qualifier("delegate")
    private LoanProductMapper loanProductMapper;

    @Override
    public LoanProduct toEntity(LoanProductCreationRequest loanProductCreationRequest) {
        LoanProduct entity = loanProductMapper.toEntity(loanProductCreationRequest);
        entity.setTenureOptions(getTenureOptions(loanProductCreationRequest));
        return entity;
    }

    @Override
    public LoanProductCreationResponseDto toDto(LoanProduct loanProduct) {
        LoanProductCreationResponseDto dto = loanProductMapper.toDto(loanProduct);
        String tenureOptions = loanProduct.getTenureOptions();
        if (tenureOptions != null){
            dto.setTenureOptions(Arrays.stream(tenureOptions.split(",")).toList().stream().map(Integer::valueOf).toList());
        }
        return dto;
    }

    private static String getTenureOptions(LoanProductCreationRequest loanProductCreationRequest) {
        if (loanProductCreationRequest.getTenureOptions() == null || loanProductCreationRequest.getTenureOptions().isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        loanProductCreationRequest.getTenureOptions().forEach(it -> sb.append(it.toString()).append(","));
        sb.deleteCharAt(sb.length() - 1); // remove last comma
        return sb.toString();
    }
}
