import javax.mail.*;
import java.util.Properties;
import javax.mail.internet.*;

public class MailSender {
    public static void main(String ... args) {
        final String username = "lessonsjavavt@gmail.com";
        final String password = "FdfgKJ34";
        final String msgResiver = "strikeoffical@mail.ru";
        final String msgTheme = "Password";
        final String msgContent = "Test";

        Properties prop = new Properties();
        prop.put("mail.smtp.host", "smtp.gmail.com");
        prop.put("mail.smtp.port", "587");
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(prop,
                new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        try {
            javax.mail.Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("from@gmail.com"));
            message.setRecipients(
                    javax.mail.Message.RecipientType.TO,
                    InternetAddress.parse(msgResiver)
            );

            message.setSubject(msgTheme);
            message.setText(msgContent);

            Transport.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        System.out.println("GGGGGGGGG");
    }
}