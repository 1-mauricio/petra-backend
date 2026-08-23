package com.marmorarias.platformbilling.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookEventRepository extends JpaRepository<WebhookEventEntity, String> {
}
