package com.marmorarias.channels;

/**
 * Porta para canais externos (WhatsApp, Instagram, e-mail...). Vazia no MVP — nenhum adapter
 * concreto ainda; existe só para o núcleo poder depender de uma abstração, nunca do canal em si.
 */
public interface NotificationPort {

    void notificar(String destinatario, String mensagem);
}
