package com.jp.dataxeed.pm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.jp.dataxeed.pm.dto.PictureDto;
import com.jp.dataxeed.pm.mapper.PictureMapper;
import com.jp.dataxeed.pm.mapper.OrderPicturesMapper;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PictureService {

    private final PictureMapper pictureMapper;
    private final OrderPicturesMapper orderPicturesMapper;

    @Value("${app.upload.dir}")
    private String uploadDir;

    private static String sanitize(String filename) {
        if (!StringUtils.hasText(filename))
            return "image";
        String cleaned = Paths.get(filename).getFileName().toString().replaceAll("[\\s\\\\/:*?\"<>|]+", "_");
        if (cleaned.length() > 120)
            cleaned = cleaned.substring(cleaned.length() - 120);
        return cleaned;
    }

    private static String uniqueName(String original) {
        String base = sanitize(original);
        String ext = "";
        int dot = base.lastIndexOf('.');
        if (dot >= 0 && dot < base.length() - 1) {
            ext = base.substring(dot);
            base = base.substring(0, dot);
        }
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        String rand = UUID.randomUUID().toString().substring(0, 8);
        return base + "_" + ts + "_" + rand + ext;
    }

    /** 画像保存（orders/{orderId}/）＋ pictures / order_pictures 登録 */
    @Transactional
    public PictureDto savePictureForOrder(MultipartFile file, int orderId) throws IOException {
        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("ファイルが空です");
        String contentType = Objects.toString(file.getContentType(), "");
        if (!contentType.startsWith("image/"))
            throw new IllegalArgumentException("画像以外はアップロードできません");

        Path base = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path orderDir = base.resolve("orders").resolve(String.valueOf(orderId));
        Files.createDirectories(orderDir);

        String storedName = uniqueName(file.getOriginalFilename());
        Path fsPath = orderDir.resolve(storedName);
        Files.copy(file.getInputStream(), fsPath, StandardCopyOption.REPLACE_EXISTING);

        PictureDto pic = new PictureDto();
        pic.setFileName(storedName);
        pic.setFileType(contentType);
        pic.setFileSize(file.getSize());
        pic.setFilePath("/files/orders/" + orderId + "/" + storedName); // 配信URL

        pictureMapper.insertPicture(pic);
        orderPicturesMapper.insertOrderPicture(orderId, pic.getId());
        return pic;
    }

    /** 削除：リンク解除 → 他で未使用なら pictures と実ファイルも削除 */
    @Transactional
    public void deletePictureFromOrder(int orderId, int pictureId) throws IOException {
        // 1) この注文とのリンクを外す
        orderPicturesMapper.deleteLink(orderId, pictureId);

        // 2) まだ他で使われている？
        int usage = pictureMapper.countUsage(pictureId); // order_pictures + product_pictures
        if (usage > 0)
            return; // 他で使われていればファイル/レコードは残すa

        // 3) 使われていない → ファイル削除して pictures からも削除
        PictureDto pic = pictureMapper.findById(pictureId);
        if (pic != null && pic.getFilePath() != null) {
            // filePath は /files/... の配信URLなので、実ファイルの場所に変換
            // 例：/files/orders/123/xxx.png → {app.upload.dir}/orders/123/xxx.png
            String relative = pic.getFilePath().replaceFirst("^/files/", "");
            Path fs = Paths.get(uploadDir).resolve(relative).toAbsolutePath().normalize();
            Files.deleteIfExists(fs);
        }
        pictureMapper.deleteById(pictureId);
    }
}
