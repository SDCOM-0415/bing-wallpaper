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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    
    // 存储图片URL映射关系
    private static Map<String, String> imageUrlMap = new HashMap<>();
    
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
     * 生成归档页面，完全使用首页内容，只替换图片链接为本地文件
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
        
        // 直接复制首页内容
        Path indexFile = Paths.get("docs/index.html");
        if (!Files.exists(indexFile)) {
            LogUtils.log("首页文件不存在: %s", indexFile);
            return;
        }
        
        String content = new String(Files.readAllBytes(indexFile), StandardCharsets.UTF_8);
        
        // 替换页面标题
        content = content.replace(
            "<title>必应壁纸|Bing Wallpaper</title>",
            "<title>本地归档|必应壁纸|Bing Wallpaper</title>"
        );
        
        // 修改导航栏，高亮归档页面
        content = content.replace(
            "<a href=\"/archive.html\" class=\"w3-bar-item w3-button w3-hover-green\">本地归档</a>",
            "<a href=\"/archive.html\" class=\"w3-bar-item w3-button w3-hover-green w3-green\">本地归档</a>"
        );
        
        // 替换所有必应图片链接为本地图片链接
        content = replaceImageLinksWithLocal(content, imagesList);
        
        // 写入归档页面文件
        Files.write(archiveFile, content.getBytes(StandardCharsets.UTF_8));
        LogUtils.log("已生成归档页面: %s", archiveFile);
    }
    
    
    /**
     * 替换HTML内容中的图片链接为本地图片链接
     * 
     * @param content HTML内容
     * @param imagesList 图片列表
     * @return 替换后的HTML内容
     */
    private static String replaceImageLinksWithLocal(String content, List<Images> imagesList) {
        if (imagesList == null || imagesList.isEmpty()) {
            return content;
        }
        
        // 为每个图片创建URL映射
        Map<String, Map<String, String>> imageUrlMappings = new HashMap<>();
        
        for (Images image : imagesList) {
            if (image == null || image.getUrl() == null || image.getDate() == null) {
                continue;
            }
            
            Map<String, String> actualFiles = getActualDownloadedFiles(image);
            if (!actualFiles.isEmpty()) {
                imageUrlMappings.put(image.getUrl(), actualFiles);
            }
        }
        
        // 替换背景图片URL
        Pattern bgPattern = Pattern.compile("background-image:\\s*url\\([\"']?(https://cn\\.bing\\.com/th\\?id=[^\"'\\)&]+)([^\"'\\)]*)[\"']?\\)");
        Matcher bgMatcher = bgPattern.matcher(content);
        StringBuffer sb = new StringBuffer();
        
        while (bgMatcher.find()) {
            String baseUrl = bgMatcher.group(1);
            String params = bgMatcher.group(2);
            String originalUrl = baseUrl + params;
            
            String localUrl = findLocalImageUrl(baseUrl, imageUrlMappings, "1920");
            if (localUrl != null) {
                bgMatcher.appendReplacement(sb, "background-image: url(\"" + localUrl + "\")");
                LogUtils.log("替换背景图片URL: %s -> %s", originalUrl, localUrl);
            } else {
                bgMatcher.appendReplacement(sb, bgMatcher.group(0));
            }
        }
        bgMatcher.appendTail(sb);
        content = sb.toString();
        
        // 替换img标签的src属性
        Pattern imgPattern = Pattern.compile("src=[\"'](https://cn\\.bing\\.com/th\\?id=[^\"'&]+)([^\"']*)[\"']");
        Matcher imgMatcher = imgPattern.matcher(content);
        sb = new StringBuffer();
        
        while (imgMatcher.find()) {
            String baseUrl = imgMatcher.group(1);
            String params = imgMatcher.group(2);
            String originalUrl = baseUrl + params;
            
            String resolution = "480";
            if (params.contains("w=1920")) {
                resolution = "1920";
            } else if (!params.contains("w=")) {
                resolution = "UHD";
            }
            
            String localUrl = findLocalImageUrl(baseUrl, imageUrlMappings, resolution);
            if (localUrl != null) {
                imgMatcher.appendReplacement(sb, "src=\"" + localUrl + "\"");
                LogUtils.log("替换img标签URL: %s -> %s", originalUrl, localUrl);
            } else {
                imgMatcher.appendReplacement(sb, imgMatcher.group(0));
            }
        }
        imgMatcher.appendTail(sb);
        content = sb.toString();
        
        // 替换下载链接
        Pattern linkPattern = Pattern.compile("href=[\"'](https://cn\\.bing\\.com/th\\?id=[^\"'&]+)([^\"']*)[\"']");
        Matcher linkMatcher = linkPattern.matcher(content);
        sb = new StringBuffer();
        
        while (linkMatcher.find()) {
            String baseUrl = linkMatcher.group(1);
            String params = linkMatcher.group(2);
            String originalUrl = baseUrl + params;
            
            String resolution = "UHD";
            if (params.contains("w=1920")) {
                resolution = "1920";
            } else if (params.contains("w=480")) {
                resolution = "480";
            }
            
            String localUrl = findLocalImageUrl(baseUrl, imageUrlMappings, resolution);
            if (localUrl != null) {
                linkMatcher.appendReplacement(sb, "href=\"" + localUrl + "\"");
                LogUtils.log("替换下载链接URL: %s -> %s", originalUrl, localUrl);
            } else {
                linkMatcher.appendReplacement(sb, linkMatcher.group(0));
            }
        }
        linkMatcher.appendTail(sb);
        content = sb.toString();
        
        return content;
    }
    
    /**
     * 查找对应的本地图片URL
     * 
     * @param baseUrl 基础URL
     * @param imageUrlMappings 图片URL映射
     * @param preferredResolution 首选分辨率
     * @return 本地图片URL，如果找不到则返回null
     */
    private static String findLocalImageUrl(String baseUrl, Map<String, Map<String, String>> imageUrlMappings, String preferredResolution) {
        // 遍历所有图片映射，查找匹配的基础URL
        for (Map.Entry<String, Map<String, String>> entry : imageUrlMappings.entrySet()) {
            String originalUrl = entry.getKey();
            if (originalUrl.startsWith(baseUrl)) {
                Map<String, String> files = entry.getValue();
                
                // 首先尝试获取首选分辨率
                String localUrl = files.get(preferredResolution);
                if (localUrl != null) {
                    return localUrl;
                }
                
                // 如果首选分辨率不存在，按优先级返回其他分辨率
                if ("UHD".equals(preferredResolution)) {
                    localUrl = files.get("1920");
                    if (localUrl != null) return localUrl;
                    localUrl = files.get("480");
                    if (localUrl != null) return localUrl;
                } else if ("1920".equals(preferredResolution)) {
                    localUrl = files.get("UHD");
                    if (localUrl != null) return localUrl;
                    localUrl = files.get("480");
                    if (localUrl != null) return localUrl;
                } else if ("480".equals(preferredResolution)) {
                    localUrl = files.get("1920");
                    if (localUrl != null) return localUrl;
                    localUrl = files.get("UHD");
                    if (localUrl != null) return localUrl;
                }
            }
        }
        
        return null;
    }
    
    /**
     * 获取实际下载的文件信息
     * 
     * @param image 图片信息
     * @return 包含实际文件路径的映射，键为分辨率，值为文件路径
     */
    private static Map<String, String> getActualDownloadedFiles(Images image) {
        Map<String, String> files = new HashMap<>();
        
        try {
            String yearMonth = image.getDate().substring(0, 7);
            Path monthDir = Paths.get("docs/local_img", yearMonth);
            
            if (!Files.exists(monthDir) || !Files.isDirectory(monthDir)) {
                return files;
            }
            
            // 获取图片名称
            String imgName = extractImageName(image.getUrl());
            if (imgName == null || imgName.isEmpty()) {
                return files;
            }
            
            // 检查各种分辨率的文件是否存在
            String[] resolutions = {"480", "1920", "UHD"};
            
            for (String resolution : resolutions) {
                String fileName = imgName + "_" + resolution + ".jpg";
                Path filePath = monthDir.resolve(fileName);
                
                if (Files.exists(filePath)) {
                    String webPath = "/local_img/" + yearMonth + "/" + fileName;
                    files.put(resolution, webPath);
                    LogUtils.log("找到本地文件: %s -> %s", resolution, webPath);
                }
            }
            
            // 如果没有找到预期的文件，尝试扫描目录中的所有文件
            if (files.isEmpty()) {
                LogUtils.log("未找到预期文件，扫描目录: %s", monthDir);
                Files.list(monthDir)
                    .filter(path -> path.toString().endsWith(".jpg"))
                    .filter(path -> path.getFileName().toString().contains(imgName.substring(0, Math.min(imgName.length(), 10))))
                    .forEach(path -> {
                        String fileName = path.getFileName().toString();
                        String webPath = "/local_img/" + yearMonth + "/" + fileName;
                        
                        // 根据文件名推断分辨率
                        if (fileName.contains("_480")) {
                            files.put("480", webPath);
                        } else if (fileName.contains("_1920")) {
                            files.put("1920", webPath);
                        } else if (fileName.contains("_UHD")) {
                            files.put("UHD", webPath);
                        } else {
                            // 如果无法确定分辨率，默认作为UHD
                            files.put("UHD", webPath);
                        }
                        
                        LogUtils.log("扫描到文件: %s", webPath);
                    });
            }
            
        } catch (Exception e) {
            LogUtils.log("获取实际下载文件时出错: %s, 错误: %s", image.getUrl(), e.getMessage());
        }
        
        return files;
    }
    
}