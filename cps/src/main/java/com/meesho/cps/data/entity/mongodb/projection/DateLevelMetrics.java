package com.meesho.cps.data.entity.mongodb.projection;

import com.meesho.cps.data.internal.BasePerformanceMetrics;
import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.Setter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class DateLevelMetrics extends BasePerformanceMetrics {

    @Id
    private String date;

}
