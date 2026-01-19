package com.better.CommuteMate.auth.application;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     *
     * @param toEmail 수신자 이메일
     * @param code 6자리 인증번호
     */
    public void sendVerificationCode(String toEmail, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("[CommuteMate] 이메일 인증번호");

            // HTML 이메일 본문
            String htmlContent = buildEmailContent(code);
            helper.setText(htmlContent, true); // true = HTML

            // 이메일 발송
            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("이메일 발송 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 이메일 HTML 템플릿 (인라인 스타일 - 네이버, 아웃룩 등 호환)
     */
    private String buildEmailContent(String code) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                </head>
                <body style="margin: 0; padding: 0; font-family: Arial, sans-serif; line-height: 1.6; color: #333333;">
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0">
                        <tr>
                            <td align="center" style="padding: 20px;">
                                <table role="presentation" width="600" cellspacing="0" cellpadding="0" border="0" style="max-width: 600px; width: 100%%;">
                                    <!-- Header -->
                                    <tr>
                                        <td align="center" style="background-color: #4CAF50; color: #ffffff; padding: 20px;">
                                            <h1 style="margin: 0; font-size: 24px;">CommuteMate 이메일 인증</h1>
                                        </td>
                                    </tr>
                                    <!-- Content -->
                                    <tr>
                                        <td align="center" style="background-color: #f9f9f9; padding: 30px;">
                                            <h2 style="margin: 0 0 10px 0; color: #333333;">이메일 인증번호</h2>
                                            <p style="margin: 0 0 20px 0; color: #333333;">아래 인증번호를 입력하여 이메일 인증을 완료해주세요.</p>
                                            <table role="presentation" cellspacing="0" cellpadding="0" border="0">
                                                <tr>
                                                    <td align="center" style="padding: 20px 40px; background-color: #ffffff; border: 2px dashed #4CAF50; border-radius: 10px;">
                                                        <span style="font-size: 32px; font-weight: bold; letter-spacing: 8px; color: #4CAF50;">%s</span>
                                                    </td>
                                                </tr>
                                            </table>
                                            <p style="margin: 20px 0 0 0; color: #ff0000; font-size: 14px;">
                                                ⚠️ 이 인증번호는 <strong>5분</strong> 동안만 유효합니다.
                                            </p>
                                            <p style="margin: 10px 0 0 0; color: #777777; font-size: 14px;">
                                                본인이 요청하지 않았다면 이 메일을 무시하셔도 됩니다.
                                            </p>
                                        </td>
                                    </tr>
                                    <!-- Footer -->
                                    <tr>
                                        <td align="center" style="padding: 20px; color: #777777; font-size: 12px;">
                                            <p style="margin: 0;">본 메일은 발신 전용입니다. 문의사항은 고객센터를 이용해주세요.</p>
                                            <p style="margin: 5px 0 0 0;">&copy; 2025 CommuteMate. All rights reserved.</p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(code);
    }
}
