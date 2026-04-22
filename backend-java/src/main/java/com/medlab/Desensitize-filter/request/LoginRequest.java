package com.medlab.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequest {

    @NotBlank(message = "身份证号不能为空")
    @Size(min = 18, max = 18, message = "身份证号应为18位")
    private String idNumber;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 255, message = "密码长度应为 6-255 字符")
    private String password;

    public LoginRequest() {
    }

    public LoginRequest(String idNumber, String password) {
        this.idNumber = idNumber;
        this.password = password;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
