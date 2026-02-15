package ke.co.interviewusercaseworld.repayment.services.impl;

import ke.co.interviewusercaseworld.commons.enums.InterestRateTypeEnum;
import ke.co.interviewusercaseworld.repayment.model.dto.requests.LoanRepaymentSchedulingDto;
import ke.co.interviewusercaseworld.repayment.model.dto.response.RepaymentScheduleResponseDto;
import ke.co.interviewusercaseworld.repayment.services.LoanRepaymentScheduleService;
import ke.co.interviewusercaseworld.repayment.services.ScheduleCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;

import static ke.co.interviewusercaseworld.commons.utils.Helpers.convertFirstCharToLowerCase;

@Service
@RequiredArgsConstructor
public class LoanRepaymentScheduleServiceImpl implements LoanRepaymentScheduleService {

    private final Map<String, ScheduleCalculator> scheduleCalculators;


    @Override
    public Flux<RepaymentScheduleResponseDto> generateSchedule(LoanRepaymentSchedulingDto repaymentSchedulingDto) {


        InterestRateTypeEnum interestRateType = repaymentSchedulingDto.getInterestRateType();
        String chosenScheduleCalculator = convertFirstCharToLowerCase(interestRateType.name().replace("_", "") + "ScheduleCalculator");
        ScheduleCalculator calculator = scheduleCalculators.get(chosenScheduleCalculator);

        if (calculator == null) {
            throw new RuntimeException("No calculator found for " + interestRateType.name());
        }

        return calculator.apply(repaymentSchedulingDto);

    }


}
