package com.jp.dataxeed.pm.controller;

import com.jp.dataxeed.pm.dto.PictureDto;
import com.jp.dataxeed.pm.mapper.PictureMapper;
import com.jp.dataxeed.pm.service.PictureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/order/api")
public class OrderPictureController {

    private final PictureService pictureService;
    private final PictureMapper pictureMapper;

    /** 注文に紐づく画像一覧（filePath は DBに保存済みの /files/... をそのまま返す） */
    @GetMapping(value = "/{orderId}/pictures", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<PictureDto> list(@PathVariable int orderId) {
        return pictureMapper.findPicturesByOrderId(orderId);
    }

    /** 画像アップロード（複数可）: PictureService が /files/... をセットして返す */
    @PostMapping(value = "/{orderId}/pictures", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public List<PictureDto> upload(
            @PathVariable int orderId,
            @RequestParam("files") List<MultipartFile> files) throws IOException {

        List<PictureDto> result = new ArrayList<>();
        if (files == null || files.isEmpty())
            return result;

        for (MultipartFile f : files) {
            if (f == null || f.isEmpty())
                continue;
            result.add(pictureService.savePictureForOrder(f, orderId)); // ← ここで /files/... が入る
        }
        return result;
    }

    /** 画像削除（リンク解除→未使用なら実ファイル/レコード削除は Service 側） */
    @DeleteMapping("/{orderId}/pictures/{pictureId}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable int orderId, @PathVariable int pictureId) throws IOException {
        pictureService.deletePictureFromOrder(orderId, pictureId);
        return ResponseEntity.noContent().build();
    }
}
