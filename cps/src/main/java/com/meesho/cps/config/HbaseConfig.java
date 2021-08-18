package com.meesho.cps.config;

import com.meesho.cps.constants.DBConstants;

import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author shubham.aggarwal
 * 04/08/21
 */
@Configuration
public class HbaseConfig {

    @Value(DBConstants.HBase.ZOOKEEPER_HOST)
    private String zkHost;

    @Value(DBConstants.HBase.ZOOKEEPER_PORT)
    private String zkPort;

    @Value(DBConstants.HBase.ZOOKEEPER_RECOVERY_RETRIES)
    private String zkRecoveryRetries;

    @Value(DBConstants.HBase.CLIENT_PAUSE)
    private String clientPause;

    @Value(DBConstants.HBase.CLIENT_RETRIES)
    private String clientRetries;

    @Value(DBConstants.HBase.RPC_TIMEOUT)
    private String rpcTimeout;

    @Bean
    public Connection hBaseConnection() throws Exception {
        org.apache.hadoop.conf.Configuration config = HBaseConfiguration.create();

        config.set("hbase.zookeeper.quorum", zkHost);
        config.set("hbase.zookeeper.property.clientPort", zkPort);
        config.set("hbase.client.pause", clientPause);
        config.set("hbase.client.retries.number", clientRetries);
        config.set("zookeeper.recovery.retry", zkRecoveryRetries);
        config.set("hbase.rpc.timeout", rpcTimeout);

        Connection connection = ConnectionFactory.createConnection(config);
        return connection;
    }

}
