package org.lareferencia.services.vufindbulkdownloader;

import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class Mailer {

	Log log = LogFactory.getLog(Mailer.class);
	private Properties properties = System.getProperties();
	private Session session;

	public Mailer(String host, String port, String user, String pwd) {

		properties.setProperty("mail.smtp.host", host);
		properties.setProperty("mail.smtp.port", port);
		properties.setProperty("mail.smtp.starttls.enable", "true");
		properties.setProperty("mail.smtp.auth", "true");
		properties.setProperty("mail.smtp.ssl.protocols", "TLSv1.2");

		session = Session.getInstance(properties, new Authenticator() {

			@Override
			protected PasswordAuthentication getPasswordAuthentication() {

				return new PasswordAuthentication(user, pwd);
			}
		});
	}

	public void sendMail(String from, String to, String subject, String msg) {
		this.log.info("init sendMail ");
		try {
			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(from));
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
			message.setSubject(subject, "utf-8");
			message.setContent(msg, "text/html; charset=utf-8");
			Transport.send(message);
			this.log.info("email sent");
		} catch (MessagingException e) {
			e.printStackTrace();
		}
	}
}
