package com.wdbyte.bing;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    
    /**
     * 生成归档页面，使用模板系统但图片链接指向本地文件
     * 
     * @param imagesList 图片列表
     * @param archiveFile 归档页面文件路径
     * @throws IOException 如果处理过程中出错
     */
    public static void generateArchivePage(List<Images> imagesList, Path archiveFile) throws IOException {
        // 确保本地图片目录存在
        Path localImgDir = Paths.get("docs/local_img");
        if (!Files.exists(localImgDir)) {
            Files.createDirectories(localImgDir);
            LogUtils.log("创建本地图片目录: %s", localImgDir);
        }
        
        LogUtils.log("开始生成归档页面，图片数量: %d", imagesList.size());
        
        // 创建本地图片版本的Images列表
        List<Images> localImagesList = createLocalImagesList(imagesList);
        
        // 使用WebSiteGenerator的逻辑生成归档页面
        generateArchivePageWithTemplate(localImagesList, archiveFile);
        
        LogUtils.log("已生成归档页面: %s", archiveFile);
    }
    
    /**
     * 创建指向本地图片的Images列表
     * 
     * @param originalImagesList 原始图片列表
     * @return 指向本地图片的Images列表
     */
    private static List<Images> createLocalImagesList(List<Images> originalImagesList) {
        return originalImagesList.stream()
            .map(ImageDownloader::createLocalImages)
            .filter(img -> img != null)
            .collect(Collectors.toList());
    }
    
    /**
     * 创建指向本地图片的Images对象
     * 
     * @param originalImage 原始图片对象
     * @return 指向本地图片的Images对象，如果本地文件不存在则返回null
     */
    private static Images createLocalImages(Images originalImage) {
        try {
            if (originalImage == null || originalImage.getDate() == null) {
                return null;
            }
            
            String yearMonth = originalImage.getDate().substring(0, 7);
            String imgId = extractImageId(originalImage.getUrl());
            
            if (imgId == null || imgId.isEmpty()) {
                LogUtils.log("无法提取图片ID: %s", originalImage.getUrl());
                return null;
            }
            
            // 检查本地文件是否存在（优先使用UHD，然后1920，最后480）
            String[] resolutions = {"UHD", "1920", "480"};
            String localUrl = null;
            
            for (String resolution : resolutions) {
                String fileName = imgId + "_" + resolution + ".jpg";
                Path filePath = Paths.get("docs/local_img", yearMonth, fileName);
                
                if (Files.exists(filePath)) {
                    localUrl = "/local_img/" + yearMonth + "/" + fileName;
                    LogUtils.log("找到本地图片: %s -> %s", originalImage.getUrl(), localUrl);
                    break;
                }
            }
            
            if (localUrl == null) {
                LogUtils.log("未找到本地图片文件: %s", imgId);
                return originalImage; // 如果本地文件不存在，返回原始图片
            }
            
            // 创建新的Images对象，URL指向本地文件
            Images localImage = new Images(originalImage.getDesc(), originalImage.getDate(), localUrl);
            return localImage;
            
        } catch (Exception e) {
            LogUtils.log("创建本地图片对象时出错: %s, 错误: %s", originalImage.getUrl(), e.getMessage());
            return originalImage; // 出错时返回原始图片
        }
    }
    
    /**
     * 从URL中提取图片ID
     * 
     * @param url 图片URL
     * @return 图片ID
     */
    private static String extractImageId(String url) {
        return extractImageName(url); // 复用extractImageName方法
    }
    
    /**
     * 使用模板系统生成归档页面
     * 
     * @param localImagesList 本地图片列表
     * @param archiveFile 归档页面文件路径
     * @throws IOException 如果处理过程中出错
     */
    private static void generateArchivePageWithTemplate(List<Images> localImagesList, Path archiveFile) throws IOException {
        // 读取模板文件
        String templateContent = new String(Files.readAllBytes(Paths.get("docs/bing-template.html")), StandardCharsets.UTF_8);
        
        // 替换页面标题
        templateContent = templateContent.replace(
            "<title>必应壁纸 | Bing Wallpaper</title>",
            "<title>本地归档 | 必应壁纸 | Bing Wallpaper</title>"
        );
        
        // 高亮本地归档导航
        templateContent = templateContent.replace(
            "<a href=\"/archive.html\" class=\"w3-bar-item w3-button w3-hover-green\">本地归档</a>",
            "<a href=\"/archive.html\" class=\"w3-bar-item w3-button w3-hover-green w3-green\">本地归档</a>"
        );
        
        if (localImagesList.isEmpty()) {
            LogUtils.log("本地图片列表为空，无法生成归档页面");
            return;
        }
        
        // 使用第一张图片作为头部图片
        Images headImage = localImagesList.get(0);
        templateContent = templateContent.replace("${head_img_url}", headImage.getSimpleUrl());
        templateContent = templateContent.replace("${head_img_desc}", headImage.getDesc());
        templateContent = templateContent.replace("${head_title}", "本地归档 | Bing Wallpaper");
        
        // 生成图片卡片列表
        StringBuilder imgCardList = new StringBuilder();
        int count = 0;
        for (Images image : localImagesList) {
            if (count >= 30) break; // 限制显示30张图片
            
            String imgCard = generateLocalImgCard(image);
            imgCardList.append(imgCard);
            count++;
        }
        templateContent = templateContent.replace("${img_card_list}", imgCardList.toString());
        
        // 简化侧边栏和月份历史（归档页面不需要复杂的月份导航）
        templateContent = templateContent.replace("${sidabar}", 
            "<a href=\"/\" onclick=\"w3_close()\" class=\"w3-bar-item w3-button w3-hover-green w3-large\">返回首页</a>");
        templateContent = templateContent.replace("${month_history}", 
            "<a class=\"w3-tag w3-button w3-hover-green w3-green w3-margin-bottom\" href=\"/archive.html\">本地归档</a>");
        
        // 写入文件
        Files.write(archiveFile, templateContent.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * 生成本地图片卡片HTML
     * 
     * @param image 本地图片对象
     * @return 图片卡片HTML
     */
    private static String generateLocalImgCard(Images image) {
        String imgCard = ""
            + "<div class=\"w3-third \" style=\"position: relative;height:249px\">\n"
            + "  <img class=\"smallImg\" src=\"" + image.getSimpleUrl() + "\"  style=\"width:95%;\" />"
            + "<a href=\"" + image.getDetailUrlPath() + "\"  target=\"_blank\"> <img class=\"bigImg w3-hover-shadow\" src=\"" + image.getSimpleUrl() + "\" style=\"width:95%\" onload=\"imgloading(this)\"></a>\n"
            + " <p>" + image.getDate() + " <a href=\"" + image.getSimpleUrl() + "\" target=\"_blank\" download>下载本地图片</a> "
            + "<button class=\"like-button img-btn\" onclick=\"updateLove('local','" + image.getDate() + "')\">喜欢</button>"
            + "</p>\n"
            + "</div>";
        
        return imgCard;
    }
}