package com.bot.model.trade;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@ToString
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BuyOrder {

    private String productId;
    private InvestingAmount investingAmount;

    private Integer leverage;
    private String direction;
}
