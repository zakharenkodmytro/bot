package com.bot.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Product {

    private String productId;
    private BigDecimal buyPrice;
    private BigDecimal upperLimit;
    private BigDecimal lowerLimit;

}
