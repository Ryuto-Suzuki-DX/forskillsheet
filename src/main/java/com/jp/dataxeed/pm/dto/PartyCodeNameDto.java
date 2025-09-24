package com.jp.dataxeed.pm.dto;

import lombok.Data;

@Data
public class PartyCodeNameDto {
    private Integer id; // ★追加：partyId を渡すために必要
    private String partyCode;
    private String partyName;
}
