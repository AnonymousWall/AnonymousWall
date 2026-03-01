package com.anonymous.wall.notification.device;

import io.micronaut.core.annotation.Introspected;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Introspected
public class RegisterDeviceRequest {

    @NotBlank
    private String deviceToken;

    @NotBlank
    @Pattern(regexp = "IOS")
    private String platform;

    public RegisterDeviceRequest() {}

    public RegisterDeviceRequest(String deviceToken, String platform) {
        this.deviceToken = deviceToken;
        this.platform = platform;
    }

    public String getDeviceToken() { return deviceToken; }
    public void setDeviceToken(String deviceToken) { this.deviceToken = deviceToken; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
}
