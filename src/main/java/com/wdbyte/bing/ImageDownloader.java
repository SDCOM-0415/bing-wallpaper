package com.wdbyte.bing;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * 图片下载工具类
 * 
 * @author SDCOM-0415
 * @date 2023/11/15
 */
public class ImageDownloader {

    // 本地图片存储根目录
    private static final String LOCAL_IMG_DIR = "docs/local_img";
    
    /**
     * 下载并保存图片到本地
     * 
     * @param imagesList 图片列表
     * @throws IOException 如果下载或保存过程中出错
     */
    public static void downloadImages(List<Images> imagesList) throws IOException {
        // 创建本地图片目录
        Path localImgPath = Paths.get(LOCAL_IMG_DIR);
        if (!Files.exists(localImgPath)) {
            Files.createDirectories(localImgPath);
        }
        
        LogUtils.log("开始下载图片到本地目录: %s", LOCAL_IMG_DIR);
        
        // 限制下载最近的10张图片
        int count = 0;
        int maxImages = 10;
        
        for (Images image : imagesList) {
            if (count >= maxImages) break;
            if (image.getUrl() == null) continue;
            
            try {
                downloadImage(image);
                count++;
            } catch (Exception e) {
                LogUtils.log("下载图片失败: %s, 错误: %s", image.getUrl(), e.getMessage());
            }
        }
        
        LogUtils.log("图片下载完成，共下载 %d 张图片", count);
    }
    
    /**
     * 下载单张图片的不同分辨率版本
     * 
     * @param image 图片信息
     * @throws IOException 如果下载或保存过程中出错
     */
    private static void downloadImage(Images image) throws IOException {
        String url = image.getUrl();
        String date = image.getDate();
        
        // 提取年月作为目录名
        String yearMonth = date.substring(0, 7);
        Path monthDir = Paths.get(LOCAL_IMG_DIR, yearMonth);
        if (!Files.exists(monthDir)) {
            Files.createDirectories(monthDir);
        }
        
        // 提取图片名称
        String imgName = extractImageName(url);
        
        // 下载不同分辨率的图片
        downloadImageWithResolution(url, monthDir, imgName, "384x216", "&pid=hp&w=384&h=216&rs=1&c=4");
        downloadImageWithResolution(url, monthDir, imgName, "1000", "&w=1000");
        downloadImageWithResolution(url, monthDir, imgName, "4k", "");
        
        LogUtils.log("已下载图片 %s 的三种分辨率版本到 %s", imgName, monthDir);
    }
    
    /**
     * 下载指定分辨率的图片
     * 
     * @param baseUrl 基础URL
     * @param targetDir 目标目录
     * @param imgName 图片名称
     * @param resolution 分辨率标识
     * @param urlSuffix URL后缀参数
     * @throws IOException 如果下载或保存过程中出错
     */
    private static void downloadImageWithResolution(String baseUrl, Path targetDir, String imgName, 
                                                  String resolution, String urlSuffix) throws IOException {
        // 构建完整的URL
        String fullUrl = baseUrl;
        if (!urlSuffix.isEmpty()) {
            // 如果URL已经包含参数，则添加&开头的参数，否则添加?开头的参数
            if (baseUrl.contains("?")) {
                fullUrl = baseUrl + urlSuffix;
            } else {
                fullUrl = baseUrl + "?" + urlSuffix.substring(1);
            }
        }
        
        // 构建目标文件路径
        String fileName = imgName + "_" + resolution + ".jpg";
        Path targetFile = targetDir.resolve(fileName);
        
        // 下载图片
        try (InputStream in = new URL(fullUrl).openStream()) {
            Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }
    
    /**
     * 从URL中提取图片名称
     * 
     * @param url 图片URL
     * @return 图片名称
     */
    private static String extractImageName(String url) {
        // 移除URL参数
        String baseUrl = url.split("\\?")[0];
        
        // 提取文件名
        String fileName = baseUrl.substring(baseUrl.lastIndexOf('/') + 1);
        
        // 移除文件扩展名
        if (fileName.contains(".")) {
            fileName = fileName.substring(0, fileName.lastIndexOf('.'));
        }
        
        return fileName;
    }
}