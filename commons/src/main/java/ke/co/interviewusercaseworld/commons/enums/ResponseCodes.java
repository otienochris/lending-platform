package ke.co.interviewusercaseworld.commons.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseCodes {
    RC_200("Operation Successful"),
    RC_400("Bad Request"),
    RC_401("Unauthorized"),
    RC_403("Forbidden"),
    RC_404("Not found"),
    RC_500("Internal Server Error"),
    RC_201("Resource Created Successfully"),
    RC_409("Duplicate Records");
    private final String description;

}
