package com.example.WebDichVu_SpringBoot.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public String uploadImage(MultipartFile file) throws IOException {
        // 1. Upload file lên Cloudinary (Lấy public_id)
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
        String publicId = uploadResult.get("public_id").toString();

        // 2. Tạo URL đã được tối ưu hóa (Transformation)
        String optimizedUrl = cloudinary.url()
                .secure(true) // Luôn dùng HTTPS
                .transformation(new Transformation()
                        .width(1000).crop("limit") // Thu nhỏ nếu ảnh lớn hơn 1000px
                        .quality("auto") // Tự động nén (Giảm dung lượng mà mắt thường khó thấy)
                        .fetchFormat("auto"))
                .generate(publicId);

        return optimizedUrl;
    }
}