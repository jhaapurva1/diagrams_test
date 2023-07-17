package com.meesho.cps.config;

import com.meesho.cps.converter.readconverter.DocumentToCampaignDateWiseMetricsConverter;
import com.meesho.cps.converter.readconverter.DocumentToCampaignMetricsConverter;
import com.meesho.cps.converter.writeconverter.CampaignDateWiseMetricsToDocumentConverter;
import com.meesho.cps.converter.writeconverter.CampaignMetricsToDocumentConverter;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

@Configuration
public class MongoConverterConfig extends AbstractMongoClientConfiguration {

    @Autowired
    ApplicationProperties applicationProperties;

    @Override
    public String getDatabaseName() {
        return "campaign-performance";
    }

    @Override
    protected void configureConverters(MongoCustomConversions.MongoConverterConfigurationAdapter adapter) {
        adapter.registerConverter(new DocumentToCampaignDateWiseMetricsConverter());
        adapter.registerConverter(new DocumentToCampaignMetricsConverter());
        adapter.registerConverter(new CampaignDateWiseMetricsToDocumentConverter());
        adapter.registerConverter(new CampaignMetricsToDocumentConverter());
    }

    @Override
    public MongoClient mongoClient() {
        ConnectionString connectionString = new ConnectionString(applicationProperties.getMongoDBUri());
        MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .build();
        return MongoClients.create(mongoClientSettings);
    }
}
