package com.jp.dataxeed.pm.mapper;

import com.jp.dataxeed.pm.dto.PictureDto;
import java.util.List;

public interface PictureMapper {
    void insertPicture(PictureDto picture);

    List<PictureDto> findPicturesByOrderId(int orderId);

    // ★ 追加
    PictureDto findById(int id);

    int deleteById(int id);

    /** order_pictures + product_pictures の合計使用数 */
    int countUsage(int pictureId);
}
