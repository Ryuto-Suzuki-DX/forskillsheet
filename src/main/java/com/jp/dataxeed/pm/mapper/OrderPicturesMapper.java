package com.jp.dataxeed.pm.mapper;

import org.apache.ibatis.annotations.Param;

public interface OrderPicturesMapper {
    int insertOrderPicture(@Param("orderId") int orderId, @Param("pictureId") int pictureId);

    int deleteByOrderId(int orderId);

    int deleteByPictureId(int pictureId);

    // ★ 追加：この注文とこの画像のリンクだけ消す
    int deleteLink(@Param("orderId") int orderId, @Param("pictureId") int pictureId);
}
