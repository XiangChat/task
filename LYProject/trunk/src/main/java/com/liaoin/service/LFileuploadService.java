package com.liaoin.service;

import org.frame.response.JsonRestResponse;
import org.frame.common.Common;
import org.frame.util.QiniuUtil;
import org.frame.exception.BusinessException;
import org.frame.util.PropertiesUtil;
import org.frame.response.RestUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.UUID;

/**
 * 作者：Li.Wei
 * 时间：2018/8/3 16:37
 * 描述：
 */
@Service
@Slf4j
public class LFileuploadService {


    /**
     * 作者： Li.Wei
     * 时间： 2018/8/6 9:04
     * 描述： 上传文件入口
     **/
    public JsonRestResponse fileupload(MultipartFile file, HttpServletRequest request) throws Exception {
        Integer upload_switch = Integer.valueOf(PropertiesUtil.get("UPLOAD_SWITCH"));
        switch (upload_switch){
            case 0://本地
                return this.uploadFileToLocal(file, request);//本地
            case 1://七牛云
                return this.uploadFileToNetWork(file,request);//上传到网络
            default:
                    throw new BusinessException("common.file.upload.switch.erro");
        }
    }

    /**
     * 作者： Li.Wei
     * 时间： 2018/8/3 17:13
     * 描述： 上传到本地
     **/
    public JsonRestResponse uploadFileToLocal(MultipartFile file, HttpServletRequest request) throws Exception {
        String relativePath = "";//上传地址
        String fileType = file.getContentType();//文件类型
        //图片Type
        String[] images = PropertiesUtil.get("IMAGES").split(",");
        //视频Type
        String[] videos = PropertiesUtil.get("VIDEOS").split(",");
        //文件Type
        String[] files = PropertiesUtil.get("FILES").split(",");
        //音频Type
        String[] audios = PropertiesUtil.get("AUDIOS").split(",");
        if (Common.isExist(images, fileType)) {
            //类型为图片
            relativePath = PropertiesUtil.get("UPLOAD_IMAGE_PATH");
        } else if (Common.isExist(videos, fileType)) {
            //类型为视频
            relativePath = PropertiesUtil.get("UPLOAD_VIDEO_PATH");
        } else if (Common.isExist(files, fileType)) {
            //类型为文件
            relativePath = PropertiesUtil.get("UPLOAD_FILE_PATH");
        } else if (Common.isExist(audios, fileType)) {
            //类型为音频
            relativePath = PropertiesUtil.get("UPLOAD_AUDIO_PATH");
        }
        //在路径后方加入年月日
        relativePath += Common.numberDate() + "/";
        //当文件夹不存就创建
        File filePath = new File(relativePath);
        if (!filePath.exists()) {
            //创建文件夹路径
            filePath.mkdirs();
        }
        String fileID = UUID.randomUUID().toString();
        String fileName = file.getOriginalFilename();
        String name = "";
        if (!file.isEmpty()) {
            String extensionName = fileName
                    .substring(fileName.lastIndexOf("."));
            name = fileID + extensionName;
            File fileDir = new File(relativePath);
            if (!fileDir.exists()) {
                fileDir.mkdirs();
            }
            if (file != null && !file.isEmpty()) {
                FileOutputStream out = new FileOutputStream(relativePath
                        + File.separator + name);
                InputStream in = file.getInputStream();
                byte buffer[] = new byte[1024];
                int len = 0;
                while ((len = in.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }
                out.flush();
                out.close();
                in.close();
            }
        }
        return RestUtil.createResponse(relativePath.substring(
                relativePath.indexOf(PropertiesUtil.get("SUBSTRING_UPLOAD_FILE_URL"))).concat(name));
    }


    /**
     * 作者： Li.Wei
     * 时间： 2018/8/3 17:13
     * 描述： 上传到网络
     **/
    public JsonRestResponse uploadFileToNetWork(MultipartFile file, HttpServletRequest request) throws Exception {
        QiniuUtil qny = new QiniuUtil();
        return qny.upload(file);
    }

    /**
     * 作者： Li.Wei
     * 时间： 2018/8/3 17:25
     * 描述： 批量上传
     **/
    public JsonRestResponse batchFileupload(MultipartFile[] fileAll, HttpServletRequest request) throws Exception {
        StringBuilder fileUrlData = new StringBuilder();
        Integer upload_switch = Integer.valueOf(PropertiesUtil.get("UPLOAD_SWITCH"));
        for (MultipartFile file : fileAll) {
            Object fileUrl = null;
            switch (upload_switch){
                case 0://本地
                    fileUrl = this.uploadFileToLocal(file, request).getResult();
                    break;
                case 1://七牛云
                    fileUrl = this.uploadFileToNetWork(file,request).getResult();
                    break;
                default:
                    throw new BusinessException("common.file.upload.switch.erro");
            }
            if (Common.isNull(fileUrlData)) fileUrlData.append(fileUrl);
            else fileUrlData.append(",").append(fileUrl);
        }
        return RestUtil.createResponse(fileUrlData.toString());
    }

    /**
     * 作者： Li.Wei
     * 时间： 2018/8/6 9:04
     * 描述： 文件下载
     **/
    public void fileDown(HttpServletRequest request, HttpServletResponse response, String name, String path) throws Exception {
        String uploadPath = "";
        if (!Common.isNull(path)) {
            uploadPath = path + "//";
        }
        String filePath = name;
        String fileName = name;
        if (request.getHeader("User-Agent").toLowerCase()
                .indexOf("firefox") > 0) {
            fileName = new String(fileName.getBytes("UTF-8"), "ISO8859-1");
        } else if (request.getHeader("User-Agent").toUpperCase()
                .indexOf("MSIE") > 0) {
            fileName = URLEncoder.encode(fileName, "UTF-8");
        }
        response.setContentType("text/plain");
        response.setHeader("Location", fileName);
        response.reset();
        response.setHeader("Cache-Control", "max-age=0");
        response.setHeader("Content-Disposition", "attachment; filename="
                + fileName);
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        OutputStream fos = null;
        InputStream fis = null;
        filePath = uploadPath + filePath;
        fis = new FileInputStream(filePath);
        response.setIntHeader("Content-Length", fis.available());//让浏览器显示文件下载的大小
        bis = new BufferedInputStream(fis);
        fos = response.getOutputStream();
        bos = new BufferedOutputStream(fos);
        int bytesRead = 0;
        byte[] buffer = new byte[5120];
        while ((bytesRead = bis.read(buffer)) != -1) {
            bos.write(buffer, 0, bytesRead);
        }
        bos.close();
        bis.close();
        fos.close();
        fis.close();
    }
}
