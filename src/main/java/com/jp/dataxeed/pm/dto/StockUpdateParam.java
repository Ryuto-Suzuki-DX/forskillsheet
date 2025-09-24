package com.jp.dataxeed.pm.dto;

import lombok.Data;

@Data
public class StockUpdateParam {
    private Integer productId;
    private Long delta; // IN:+n / OUT:-n ではなく、呼び出し側で符号は分けてもOK
}