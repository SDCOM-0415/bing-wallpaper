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
        if (imagesList == null || imagesList.isEmpty()) {
            LogUtils.log("图片列表为空，无法下载图片");
            return;
        }
        
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
            if (image == null || image.getUrl() == null || image.getDate() == null) {
                LogUtils.log("跳过无效的图片数据");
                continue;
            }
            
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
        
        if (url == null || url.isEmpty()) {
            LogUtils.log("图片URL为空，无法下载");
            return;
        }
        
        if (date == null || date.isEmpty() || date.length() < 7) {
            LogUtils.log("图片日期格式无效: %s，使用当前日期", date);
            date = java.time.LocalDate.now().toString();
        }
        
        try {
            // 提取年月作为目录名
            String yearMonth = date.substring(0, 7);
            Path monthDir = Paths.get(LOCAL_IMG_DIR, yearMonth);
            if (!Files.exists(monthDir)) {
                Files.createDirectories(monthDir);
            }
            
            // 提取图片名称
            String imgName = extractImageName(url);
            if (imgName == null || imgName.isEmpty()) {
                LogUtils.log("无法从URL提取图片名称: %s", url);
                imgName = "bing_" + date.replace("-", "");
            }
            
            LogUtils.log("处理图片: %s, 提取的名称: %s", url, imgName);
            
            // 根据URL类型选择不同的下载参数
            if (url.contains("cn.bing.com/th") && url.contains("id=")) {
                // 必应壁纸URL格式
                downloadImageWithResolution(url, monthDir, imgName, "480", "&pid=hp&w=480");
                downloadImageWithResolution(url, monthDir, imgName, "1920", "&pid=hp&w=1920");
                downloadImageWithResolution(url, monthDir, imgName, "UHD", "");
            } else {
                // 其他URL格式
                downloadImageWithResolution(url, monthDir, imgName, "384x216", "&pid=hp&w=384&h=216&rs=1&c=4");
                downloadImageWithResolution(url, monthDir, imgName, "1000", "&w=1000");
                downloadImageWithResolution(url, monthDir, imgName, "4k", "");
            }
            
            LogUtils.log("已下载图片 %s 的多种分辨率版本到 %s", imgName, monthDir);
        } catch (Exception e) {
            LogUtils.log("处理图片时出错: %s, 错误: %s", url, e.getMessage());
            throw e;
        }
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
        try {
            // 构建完整的URL
            String fullUrl = baseUrl;
            
            // 处理必应壁纸URL的特殊情况
            if (baseUrl.contains("cn.bing.com/th") && baseUrl.contains("id=")) {
                // 如果是必应壁纸URL，保留id参数，替换或添加其他参数
                int idStart = baseUrl.indexOf("id=");
                int idEnd = baseUrl.indexOf("&", idStart);
                
                if (idEnd == -1) {
                    // URL只有id参数
                    fullUrl = baseUrl + urlSuffix;
                } else {
                    // URL有多个参数，保留id参数，替换其他参数
                    fullUrl = baseUrl.substring(0, idEnd) + urlSuffix;
                }
            } else if (urlSuffix != null && !urlSuffix.isEmpty()) {
                // 处理普通URL
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
            LogUtils.log("正在下载图片: %s 到 %s", fullUrl, targetFile);
            
            try (InputStream in = new URL(fullUrl).openStream()) {
                Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
                LogUtils.log("图片下载成功: %s", fileName);
            } catch (Exception e) {
                LogUtils.log("下载图片失败: %s, 错误: %s", fullUrl, e.getMessage());
                throw e;
            }
        } catch (Exception e) {
            LogUtils.log("处理图片URL时出错: %s, 分辨率: %s, 错误: %s", baseUrl, resolution, e.getMessage());
            throw new IOException("下载图片失败", e);
        }
    }
    
    /**
     * 从URL中提取图片名称
     * 
     * @param url 图片URL
     * @return 图片名称
     */
    private static String extractImageName(String url) {
        try {
            if (url == null || url.isEmpty()) {
                return null;
            }
            
            // 必应壁纸URL格式通常是：https://cn.bing.com/th?id=OHR.SaypeDubai_EN-US5078679271_UHD.jpg&pid=hp&w=1920
            // 需要从id参数中提取图片名称
            
            if (url.contains("id=")) {
                // 提取id参数值
                int idStart = url.indexOf("id=") + 3;
                int idEnd = url.indexOf("&", idStart);
                if (idEnd == -1) {
                    idEnd = url.length();
                }
                
                String id = url.substring(idStart, idEnd);
                
                // 移除文件扩展名
                if (id.contains(".")) {
                    id = id.substring(0, id.lastIndexOf('.'));
                }
                
                return id;
            }
            
            // 如果URL不包含id参数，尝试从路径中提取文件名
            String baseUrl = url.contains("?") ? url.split("\\?")[0] : url;
            
            // 提取文件名
            int lastSlashIndex = baseUrl.lastIndexOf('/');
            if (lastSlashIndex == -1 || lastSlashIndex == baseUrl.length() - 1) {
                // URL格式不正确，无法提取文件名
                return "bing_image_" + System.currentTimeMillis();
            }
            
            String fileName = baseUrl.substring(lastSlashIndex + 1);
            
            // 移除文件扩展名
            if (fileName.contains(".")) {
                fileName = fileName.substring(0, fileName.lastIndexOf('.'));
            }
            
            // 如果文件名为空，使用默认名称
            if (fileName.isEmpty()) {
                fileName = "bing_image_" + System.currentTimeMillis();
            }
            
            return fileName;
        } catch (Exception e) {
            LogUtils.log("提取图片名称时出错: %s, 错误: %s", url, e.getMessage());
            return "bing_image_" + System.currentTimeMillis();
        }
    }
}