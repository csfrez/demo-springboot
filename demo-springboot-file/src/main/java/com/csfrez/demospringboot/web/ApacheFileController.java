package com.csfrez.demospringboot.web;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

@Controller
public class ApacheFileController {

    /**
     * 定义文件上传的目录
     */
    private static final String FILE_DIR = "E:/tmp/file";


    /**
     * 访问 upload3 路径时，跳转到apacheUpload.html页面
     *
     * @return
     */
    @GetMapping("/upload")
    public String index() {
        return "upload";
    }

    /**
     * 上传文件，支持多文件/表单上传
     *
     * @param request
     * @throws Exception
     */
    @PostMapping("/apacheFileUpload")
    @ResponseBody
    public String fileUpload(HttpServletRequest request) throws Exception {
        // 判断上传的文件是普通的表单还是带文件的表单
        boolean isMultipart = ServletFileUpload.isMultipartContent(request);
        if (!isMultipart) {
            // 终止方法运行，说明这是一个普通的表单，直接返回
            return "Upload file fail";
        }
        // 1.创建DiskFileItemFactory对象，处理文件上传路径或者大小限制的
        DiskFileItemFactory factory = getDiskFileItemFactory();
        // 2.获取ServletFileUpload
        ServletFileUpload upload = getServletFileUpload(factory);
        // 3.处理上传的文件
        List<FileItem> fileItems = upload.parseRequest(request);
        for (FileItem fileItem : fileItems) {
            // 判断上传的文件是普通的表单还是带文件的表单
            if (fileItem.isFormField()) {
                String name = fileItem.getFieldName();
                String value = fileItem.getString("UTF-8"); // 处理乱码
                System.out.println(name + ": " + value);
            } else {
                // 处理文件
                String filePath = FILE_DIR + "/" + fileItem.getName();
                System.out.println("fileName=" + fileItem.getName() + ", fileSize=" + fileItem.getSize());
                if(fileItem.getSize() <= 0){
                    continue;
                }
                try (InputStream inputStream = fileItem.getInputStream(); OutputStream outputStream = new FileOutputStream(filePath)) {

                    // 拷贝文件流
                    IOUtils.copy(inputStream, outputStream);
                }
                // 清除临时文件
                fileItem.delete();
                System.out.println("上传成功，文件名：" + fileItem.getName());
            }
        }
        return "Upload file success";
    }


    /**
     * 创建DiskFileItemFactory对象
     *
     * @return
     */
    private DiskFileItemFactory getDiskFileItemFactory() {
        DiskFileItemFactory factory = new DiskFileItemFactory();
        // 设置一个缓冲区大小, 当文件大于这个缓冲区大小的时候, 就会放到临时磁盘目录，防止内存崩溃
        factory.setSizeThreshold(1024 * 1024);
        // 设置临时磁盘目录, 接收上传的 File
        factory.setRepository(new File(FILE_DIR + "/cache"));
        return factory;
    }

    /**
     * 获取ServletFileUpload
     *
     * @param factory
     * @return
     */
    private ServletFileUpload getServletFileUpload(DiskFileItemFactory factory) {
        ServletFileUpload upload = new ServletFileUpload(factory);
        // 监听上传进度
        upload.setProgressListener((pBytesRead, pContentLength, pItems) -> System.out.println("总大小：" + pContentLength + "，已上传：" + pBytesRead));
        // 处理乱码问题
        upload.setHeaderEncoding("UTF-8");
        // 设置单个文件的最大值，-1：表示无限制
        upload.setFileSizeMax(-1L);
        return upload;
    }
}