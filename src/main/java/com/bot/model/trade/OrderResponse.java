package com.bot.model.trade;

import com.bot.model.Product;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@ToString
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderResponse {

    private String id;
    private String positionId;
    private Object product;
    private InvestingAmount investingAmount;
    private InvestingAmount price;
    private Integer leverage;
    private String direction;
    private String type;
    private Long dateCreated;

}
