package org.example.utils;

import com.aliyun.oss.OSS;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class FileUtil {

    @Autowired
    private OSS ossClient;

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.bucketName}")
    private String bucketName;

    @Value("${upload.path}")
    private String uploadPath;

    public boolean isImage(MultipartFile file) {
        try{
            Tika tika = new Tika();
            String mimeType = tika.detect(file.getInputStream());
            return mimeType != null && mimeType.startsWith("image/");
        }catch(Exception e){
            return false;
        }
    }
    // 将头像图片保存到本地
    public String uploadLocal(MultipartFile file) throws IOException {
        File targetFolder = new File(uploadPath);
        if (!targetFolder.exists()) {
            targetFolder.mkdirs();
        }

        // 2. 生成文件名
        String originalFilename = file.getOriginalFilename();
        String fileName = UUID.randomUUID().toString() + "_" + originalFilename;

        // 3. 保存文件
        File dest = new File(targetFolder, fileName);
        file.transferTo(dest);

        // 4. 返回访问路径
        return "/images/" + fileName;
    }

    public String uploadImageOSS(MultipartFile file) {
        // 1. 生成唯一文件名 (如: 2026/03/avatar_uuid.png)
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = UUID.randomUUID().toString() + extension;

        // 建议按日期归类，方便在 OSS 控制台查找
        String objectName = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd")) + "/" + fileName;

        try {
            // 2. 上传至 OSS
            ossClient.putObject(bucketName, objectName, file.getInputStream());

            // 3. 返回访问路径
            // 格式: https://bucketName.endpoint/objectName
            return "https://" + bucketName + "." + endpoint + "/" + objectName;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("文件上传到阿里云失败");
        }
    }

    public String uploadVideoOSS(File file,String title) {
        try {
            ossClient.putObject(bucketName, title, file);
            // 返回文件的访问 URL
            return "https://" + bucketName + "." + endpoint + "/" + title;
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }


}
