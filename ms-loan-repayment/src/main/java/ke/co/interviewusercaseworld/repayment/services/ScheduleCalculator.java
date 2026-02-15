package ke.co.interviewusercaseworld.repayment.services;

import ke.co.interviewusercaseworld.repayment.model.dto.requests.LoanRepaymentSchedulingDto;
import ke.co.interviewusercaseworld.repayment.model.dto.response.RepaymentScheduleResponseDto;
import reactor.core.publisher.Flux;

import java.util.function.Function;

public interface ScheduleCalculator extends Function<LoanRepaymentSchedulingDto, Flux<RepaymentScheduleResponseDto>> {
}
