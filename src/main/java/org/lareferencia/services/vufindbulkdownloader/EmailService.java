package org.lareferencia.services.vufindbulkdownloader;

import jakarta.mail.internet.MimeMessage;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

  private final JavaMailSender mailSender;

  public EmailService(JavaMailSender mailSender) {
    this.mailSender = mailSender;
  }

  public void sendSimpleEmail(String to, String subject, String text) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setTo(to);
    message.setSubject(subject);
    message.setText(text);
    mailSender.send(message);
  }

  public void sendHtmlEmail(String to, String subject, String htmlBody) throws Exception {
    MimeMessage message = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message, true);
    helper.setTo(to);
    helper.setSubject(subject);
    helper.setText(htmlBody, true);
    mailSender.send(message);
  }
}