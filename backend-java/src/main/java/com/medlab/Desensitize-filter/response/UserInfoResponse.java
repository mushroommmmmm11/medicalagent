package com.medlab.dto.response;

import java.util.UUID;

public class UserInfoResponse {

    private UUID id;
    private String realName;

    public UserInfoResponse() {
    }

    public UserInfoResponse(UUID id, String realName) {
        this.id = id;
        this.realName = realName;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public static final class Builder {
        private UUID id;
        private String realName;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder realName(String realName) {
            this.realName = realName;
            return this;
        }

        public UserInfoResponse build() {
            return new UserInfoResponse(id, realName);
        }
    }
}
