package uk.gov.dwp.uc.pairtest.exception;

public class ThirdPartyCallException extends Exception {
    private String errorMessage;
    public ThirdPartyCallException(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
