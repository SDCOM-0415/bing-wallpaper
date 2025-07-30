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
     * 替换HTML文件中的图片链接为本地图片链接
     * 
     * @param htmlDir HTML文件目录
     * @throws IOException 如果处理过程中出错
     */
    public static void replaceImageUrlsInHtml(Path htmlDir) throws IOException {
        if (!Files.exists(htmlDir) || !Files.isDirectory(htmlDir)) {
            LogUtils.log("HTML目录不存在或不是目录: %s", htmlDir);
            return;
        }
        
        LogUtils.log("开始替换HTML文件中的图片链接为本地图片链接: %s", htmlDir);
        
        // 递归处理所有HTML文件
        Files.walk(htmlDir)
            .filter(path -> path.toString().endsWith(".html"))
            .forEach(htmlFile -> {
                try {
                    replaceImageUrlsInFile(htmlFile);
                } catch (IOException e) {
                    LogUtils.log("替换HTML文件中的图片链接时出错: %s, 错误: %s", htmlFile, e.getMessage());
                }
            });
        
        LogUtils.log("HTML文件中的图片链接替换完成");
    }
    
    /**
     * 替换单个HTML文件中的图片链接
     * 
     * @param htmlFile HTML文件路径
     * @throws IOException 如果处理过程中出错
     */
    private static void replaceImageUrlsInFile(Path htmlFile) throws IOException {
        LogUtils.log("处理HTML文件: %s", htmlFile);
        
        String content = new String(Files.readAllBytes(htmlFile), StandardCharsets.UTF_8);
        boolean modified = false;
        
        // 替换背景图片URL
        // 匹配 background-image: url("https://cn.bing.com/th?id=xxx") 格式的URL
        Pattern bgPattern = Pattern.compile("background-image:\\s*url\\([\"']?(https://cn\\.bing\\.com/th\\?id=[^\"'\\)]+)[\"']?\\)");
        Matcher bgMatcher = bgPattern.matcher(content);
        StringBuffer sb = new StringBuffer();
        
        while (bgMatcher.find()) {
            String originalUrl = bgMatcher.group(1);
            String localUrl = getLocalImageUrl(originalUrl, htmlFile);
            
            if (localUrl != null) {
                // 替换为本地URL
                bgMatcher.appendReplacement(sb, "background-image: url(\"" + localUrl + "\")");
                modified = true;
                LogUtils.log("替换背景图片URL: %s -> %s", originalUrl, localUrl);
            }
        }
        bgMatcher.appendTail(sb);
        content = sb.toString();
        
        // 替换图片链接URL
        // 匹配 href="https://cn.bing.com/th?id=xxx" 格式的URL
        Pattern linkPattern = Pattern.compile("href=[\"'](https://cn\\.bing\\.com/th\\?id=[^\"']+)[\"']");
        Matcher linkMatcher = linkPattern.matcher(content);
        sb = new StringBuffer();
        
        while (linkMatcher.find()) {
            String originalUrl = linkMatcher.group(1);
            String localUrl = getLocalImageUrl(originalUrl, htmlFile);
            
            if (localUrl != null) {
                // 替换为本地URL
                linkMatcher.appendReplacement(sb, "href=\"" + localUrl + "\"");
                modified = true;
                LogUtils.log("替换图片链接URL: %s -> %s", originalUrl, localUrl);
            }
        }
        linkMatcher.appendTail(sb);
        content = sb.toString();
        
        // 替换img标签的src属性
        Pattern imgPattern = Pattern.compile("src=[\"'](https://cn\\.bing\\.com/th\\?id=[^\"']+)[\"']");
        Matcher imgMatcher = imgPattern.matcher(content);
        sb = new StringBuffer();
        
        while (imgMatcher.find()) {
            String originalUrl = imgMatcher.group(1);
            String localUrl = getLocalImageUrl(originalUrl, htmlFile);
            
            if (localUrl != null) {
                // 替换为本地URL
                imgMatcher.appendReplacement(sb, "src=\"" + localUrl + "\"");
                modified = true;
                LogUtils.log("替换img标签URL: %s -> %s", originalUrl, localUrl);
            }
        }
        imgMatcher.appendTail(sb);
        content = sb.toString();
        
        // 如果有修改，写回文件
        if (modified) {
            Files.write(htmlFile, content.getBytes(StandardCharsets.UTF_8));
            LogUtils.log("已更新HTML文件: %s", htmlFile);
        } else {
            LogUtils.log("HTML文件无需更新: %s", htmlFile);
        }
    }
    
    /**
     * 获取本地图片URL
     * 
     * @param originalUrl 原始URL
     * @param htmlFile HTML文件路径
     * @return 本地图片URL，如果找不到对应的本地图片则返回null
     */
    private static String getLocalImageUrl(String originalUrl, Path htmlFile) {
        // 如果已经有缓存的映射，直接返回
        if (imageUrlMap.containsKey(originalUrl)) {
            return imageUrlMap.get(originalUrl);
        }
        
        try {
            // 提取图片名称
            String imgName = extractImageName(originalUrl);
            if (imgName == null || imgName.isEmpty()) {
                return null;
            }
            
            // 确定分辨率
            String resolution = "UHD";
            if (originalUrl.contains("w=480") || originalUrl.contains("w=384")) {
                resolution = "480";
            } else if (originalUrl.contains("w=1920") || originalUrl.contains("w=1000")) {
                resolution = "1920";
            }
            
            // 从HTML文件路径中提取日期信息
            String date = extractDateFromHtmlPath(htmlFile);
            if (date == null) {
                return null;
            }
            
            // 构建本地图片路径
            String yearMonth = date.substring(0, 7);
            String localPath = "/local_img/" + yearMonth + "/" + imgName + "_" + resolution + ".jpg";
            
            // 检查本地图片是否存在
            Path localImgPath = Paths.get("docs" + localPath);
            if (!Files.exists(localImgPath)) {
                LogUtils.log("本地图片不存在: %s", localImgPath);
                return null;
            }
            
            // 缓存映射关系
            imageUrlMap.put(originalUrl, localPath);
            
            return localPath;
        } catch (Exception e) {
            LogUtils.log("获取本地图片URL时出错: %s, 错误: %s", originalUrl, e.getMessage());
            return null;
        }
    }
    
    /**
     * 从HTML文件路径中提取日期信息
     * 
     * @param htmlFile HTML文件路径
     * @return 日期字符串（YYYY-MM-DD格式），如果无法提取则返回null
     */
    private static String extractDateFromHtmlPath(Path htmlFile) {
        try {
            String pathStr = htmlFile.toString();
            
            // 尝试从路径中提取日期
            // 例如：docs/day/202507/31.html -> 2025-07-31
            Pattern datePattern = Pattern.compile("day/(\\d{4})(\\d{2})/(\\d{2})\\.html$");
            Matcher dateMatcher = datePattern.matcher(pathStr);
            
            if (dateMatcher.find()) {
                String year = dateMatcher.group(1);
                String month = dateMatcher.group(2);
                String day = dateMatcher.group(3);
                return year + "-" + month + "-" + day;
            }
            
            // 如果是首页或其他页面，尝试读取文件内容提取日期
            String content = new String(Files.readAllBytes(htmlFile), StandardCharsets.UTF_8);
            Pattern contentDatePattern = Pattern.compile("<h1 class=\"w3-xlarge\">(\\d{4}-\\d{2}-\\d{2})</h1>");
            Matcher contentDateMatcher = contentDatePattern.matcher(content);
            
            if (contentDateMatcher.find()) {
                return contentDateMatcher.group(1);
            }
            
            // 如果无法提取日期，使用当前日期
            return java.time.LocalDate.now().toString();
        } catch (Exception e) {
            LogUtils.log("从HTML文件路径提取日期时出错: %s, 错误: %s", htmlFile, e.getMessage());
            return null;
        }
    }
}