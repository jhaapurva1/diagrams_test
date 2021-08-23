package com.meesho.cps.utils;

import com.meesho.ads.lib.data.internal.RedshiftProcessedMetadata;

/**
 * @author shubham.aggarwal
 * 09/08/21
 */
public class CommonUtils {

    public static RedshiftProcessedMetadata getDefaultRedshitProcessedMetadata() {
        return RedshiftProcessedMetadata.builder().processedDataSize(0).lastEntryCreatedAt(null).build();
    }

}
