package com.meesho.cps.utils;

import com.meesho.ad.client.constants.FeedType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import static com.meesho.cps.constants.MongoFields.REAL_ESTATE_BUDGET_UTILISED_SUFFIX;

/**
 * @author shubham.aggarwal
 * 16/08/21
 */
public class FormattingUtils {

    public static Double round(Double input, int scale) {
        if(Objects.isNull(input))
            return null;
        return new BigDecimal(input).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    public static BigDecimal round(BigDecimal input, int scale) {
        if(Objects.isNull(input))
            return null;
        return input.setScale(scale, RoundingMode.HALF_UP);
    }

    public static String getRealEstateBudgetUtilisedField(FeedType realEstate) {
        return realEstate.toString() + "_" + REAL_ESTATE_BUDGET_UTILISED_SUFFIX;
    }

}
