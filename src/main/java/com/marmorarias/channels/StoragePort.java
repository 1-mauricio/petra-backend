package com.marmorarias.channels;

/**
 * Porta para armazenamento de arquivos (comprovantes, anexos...). Adapter concreto fala com o
 * Supabase Storage — o núcleo nunca depende do Storage em si, só desta abstração.
 */
public interface StoragePort {

    /** Envia o arquivo e devolve a URL pública para acessá-lo depois. */
    String upload(String path, byte[] conteudo, String contentType);
}
