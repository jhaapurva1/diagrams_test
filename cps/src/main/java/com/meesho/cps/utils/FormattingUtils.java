package com.meesho.cps.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

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

}
