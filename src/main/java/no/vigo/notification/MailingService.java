package no.vigo.notification;

import com.google.api.client.util.Base64;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

@Service
@Slf4j
public class MailingService {

    private final Gmail gmail;
    private final String sender;

    public MailingService(Gmail gmail, String sender) {
        this.gmail = gmail;
        this.sender = sender;
    }

    public String send(String title, String content, String recipient) {
        try {
            log.info("Creating email from {} to {} ...", sender, recipient);
            MimeMessage mimeMessage = createEmail(sender, recipient, title, content);
            Message message = sendMessage(sender, mimeMessage);
            return message.getId();
        } catch (MessagingException | IOException e) {
            log.error("Unable to send message!", e);
            return null;
        }
    }


    private MimeMessage createEmail(
            String from,
            String to,
            String subject,
            String bodyText)
            throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMultipart content = new MimeMultipart("related");

        MimeMessage email = new MimeMessage(session);

        MimeBodyPart htmlPart = new MimeBodyPart();

        email.setFrom(new InternetAddress(from));
        email.addRecipient(javax.mail.Message.RecipientType.TO,
                new InternetAddress(to));
        email.setSubject(subject);
        htmlPart.setText(bodyText, "UTF-8", "html");

        MimeBodyPart imagePart = new MimeBodyPart();
        try {
            imagePart.attachFile(new ClassPathResource("static/logo.png").getFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
        imagePart.setContentID("<logo>");
        imagePart.setDisposition(MimeBodyPart.INLINE);

        content.addBodyPart(htmlPart);
        content.addBodyPart(imagePart);

        email.setContent(content);

        return email;
    }

    private Message createMessageWithEmail(
            MimeMessage emailContent)
            throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }

    private Message sendMessage(
            String userId,
            MimeMessage emailContent)
            throws MessagingException, IOException {
        Message message = createMessageWithEmail(emailContent);
        message = gmail.users().messages().send(userId, message).execute();
        log.info("Message: {}", message.toPrettyString());
        return message;
    }

}