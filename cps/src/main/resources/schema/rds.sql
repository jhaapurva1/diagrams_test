-- Create syntax for TABLE 'campaign_performance'
CREATE TABLE `campaign_performance` (
                                        `id` bigint(20) NOT NULL AUTO_INCREMENT,
                                        `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        `iso_country_code` varchar(255) DEFAULT NULL,
                                        `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                        `budget_utilised` decimal(19,2) DEFAULT NULL,
                                        `campaign_id` bigint(20) DEFAULT NULL,
                                        `catalog_id` bigint(20) DEFAULT NULL,
                                        `supplier_id` bigint(20) DEFAULT NULL,
                                        `cpc` decimal(19,2) DEFAULT NULL,
                                        `order_count` int(11) DEFAULT NULL,
                                        `revenue` decimal(19,2) DEFAULT NULL,
                                        `total_clicks` bigint(20) DEFAULT NULL,
                                        `total_views` bigint(20) DEFAULT NULL,
                                        PRIMARY KEY (`id`),
                                        UNIQUE KEY `campaign_catalog_key` (`campaign_id`, `catalog_id`),
                                        KEY `campaign_id_idx` (`campaign_id`),
                                        KEY `supplier_id_idx` (`supplier_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Create syntax for TABLE 'real_estate_metadata'
CREATE TABLE `real_estate_metadata`
(
    `id`               bigint(10) NOT NULL AUTO_INCREMENT,
    `name`             varchar(255)  NOT NULL,
    `click_multiplier` decimal(6, 3) NOT NULL,
    `created_at`       timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `iso_country_code` varchar(255)           DEFAULT NULL,
    `updated_at`       timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `name_iso_country_code_key` (`name`,`iso_country_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
