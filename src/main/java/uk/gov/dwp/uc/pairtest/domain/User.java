package uk.gov.dwp.uc.pairtest.domain;

public final class User {
    private final Long accountId;

    public User(Long accountId) {
        this.accountId = accountId;
    }

    public Long getAccountId() {
        return accountId;
    }

}
