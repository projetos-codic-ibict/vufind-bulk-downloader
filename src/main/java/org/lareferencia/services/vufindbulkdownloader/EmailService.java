package org.lareferencia.services.vufindbulkdownloader;

import java.nio.charset.StandardCharsets;

import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

  private final JavaMailSender mailSender;
  private final String fromAddress;

  public EmailService(
      JavaMailSender mailSender,
      @Value("${spring.mail.username}") String fromAddress) {
    this.mailSender = mailSender;
    this.fromAddress = fromAddress;
  }

  public void sendSimpleEmail(String to, String subject, String text) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(fromAddress);
    message.setTo(to);
    message.setSubject(subject);
    message.setText(text);
    mailSender.send(message);
  }

  public void sendHtmlEmail(String to, String subject, String htmlBody) throws Exception {
    MimeMessage message = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
    helper.setFrom(fromAddress);
    helper.setTo(to);
    helper.setSubject(subject);
    helper.setText(htmlBody, true);
    mailSender.send(message);
  }
}