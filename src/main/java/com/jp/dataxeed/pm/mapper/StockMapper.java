package com.jp.dataxeed.pm.mapper;

import com.jp.dataxeed.pm.dto.StockUpdateParam;

public interface StockMapper {

    void upsertAdd(StockUpdateParam param);

    void upsertSub(StockUpdateParam param);

    Long selectForUpdate(Integer productId);
}
