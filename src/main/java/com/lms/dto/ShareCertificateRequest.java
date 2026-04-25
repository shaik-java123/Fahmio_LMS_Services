package com.lms.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShareCertificateRequest {
    private String recipientEmail;
    private String recipientName;
    private String personalMessage;
}

