package com.marmorarias.channels.storage;

import com.marmorarias.channels.StoragePort;
import com.marmorarias.channels.config.StorageProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Envia arquivos para um bucket público do Supabase Storage via API REST. */
@Component
public class SupabaseStorageAdapter implements StoragePort {

    private final StorageProperties properties;
    private final RestClient restClient;

    public SupabaseStorageAdapter(StorageProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.create();
    }

    @Override
    public String upload(String path, byte[] conteudo, String contentType) {
        String objectUrl = properties.url() + "/storage/v1/object/" + properties.bucket() + "/" + path;
        restClient.post()
                .uri(objectUrl)
                .header("Authorization", "Bearer " + properties.serviceRoleKey())
                .header("apikey", properties.serviceRoleKey())
                .header("x-upsert", "true")
                .contentType(contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM)
                .body(conteudo)
                .retrieve()
                .toBodilessEntity();
        return properties.url() + "/storage/v1/object/public/" + properties.bucket() + "/" + path;
    }
}
