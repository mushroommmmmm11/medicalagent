package com.medlab.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "真实姓名不能为空")
    @Size(min = 2, max = 100, message = "真实姓名长度应为 2-100 字符")
    private String realName;

    @NotBlank(message = "身份证号不能为空")
    @Pattern(regexp = "^[0-9X]{18}$", message = "身份证号格式不正确")
    private String idNumber;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 255, message = "密码长度应为 6-255 字符")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    public RegisterRequest() {
    }

    public RegisterRequest(String realName, String idNumber, String password, String confirmPassword) {
        this.realName = realName;
        this.idNumber = idNumber;
        this.password = password;
        this.confirmPassword = confirmPassword;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
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

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
