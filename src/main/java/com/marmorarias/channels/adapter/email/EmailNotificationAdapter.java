package com.marmorarias.channels.adapter.email;

import com.marmorarias.channels.NotificationPort;
import com.marmorarias.channels.config.MailProperties;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

/**
 * Único adapter concreto do NotificationPort hoje. Sem SMTP configurado (SMTP_HOST vazio), só loga
 * — não falha, não bloqueia o fluxo do pedido que disparou a notificação (ver OrderService).
 */
@Component
public class EmailNotificationAdapter implements NotificationPort {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationAdapter.class);

    private final MailProperties properties;

    public EmailNotificationAdapter(MailProperties properties) {
        this.properties = properties;
    }

    @Override
    public void notificar(String destinatario, String mensagem) {
        if (destinatario == null || destinatario.isBlank()) {
            return;
        }
        if (!properties.configurado()) {
            log.info("SMTP não configurado — notificação não enviada para {}: {}", destinatario, mensagem);
            return;
        }

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(properties.host());
        sender.setPort(properties.port());
        sender.setUsername(properties.username());
        sender.setPassword(properties.password());
        Properties mailProps = sender.getJavaMailProperties();
        mailProps.put("mail.smtp.auth", "true");
        mailProps.put("mail.smtp.starttls.enable", "true");

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.from());
        message.setTo(destinatario);
        message.setSubject("Atualização do seu pedido — Marmoraria");
        message.setText(mensagem);
        sender.send(message);
    }
}
