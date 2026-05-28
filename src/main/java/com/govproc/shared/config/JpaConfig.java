package com.govproc.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Habilita o JPA Auditing para que @CreatedDate / @LastModifiedDate
 * em {@link com.govproc.shared.domain.BaseEntity} sejam preenchidos.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
