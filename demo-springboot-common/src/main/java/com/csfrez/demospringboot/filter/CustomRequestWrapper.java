package com.csfrez.demospringboot.filter;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartRequest;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

/**
 * @ClassName : CustomRequestWrapper
 * @Description : 自定义Request，解决request请求流中的数据二次或多次使用问题
 * 继承HttpServletRequestWrapper，将请求体中的流copy一份，覆写getInputStream()和getReader()方法供外部使用。
 * 每次调用覆写后的getInputStream()方法都是从复制出来的二进制数组中进行获取，这个二进制数组在对象存在期间一直存在，这样就实现了流的重复读取。
 */
@Slf4j
public class CustomRequestWrapper extends HttpServletRequestWrapper {

    /**
     * 存储Body数据
     */
    private byte[] body;

    /**
     * 保存原始Request对象，当请求为 MultipartRequest 文件上传类的请求操作
     */
    private HttpServletRequest request;
    /**
     * 额外参数可以加到这个里面 重写getParameter() 方法 ，从而使请求中不存在的参数，通过该Map集合中获取！！
     */
    private Map<String, String[]> parameterMap = new LinkedHashMap<>();


    /**
     * Description: customRequestWrapper 请求包装类的构造方法
     *
     * @param request
     * @return
     */
    public CustomRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        //[文件上传相关的操作]
        this.request = request;
        if (request instanceof MultipartRequest) {
            // 如果是[文件上传类]请求
            this.parseBody(request);
        } else {
            //[普通请求类]将Body数据存储起来
            String bodyString = getBodyString(request);
            body = bodyString.getBytes(Charset.defaultCharset());
        }
    }
    /* [MultipartRequest 文件上传]相关操作接口 */

    /**
     * 如果是 MultipartRequest，需要解析参数信息
     */
    private void parseBody(HttpServletRequest request) {
        Map<String, Object> parameterMap = new LinkedHashMap<>();
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String name = parameterNames.nextElement();
            String[] values = request.getParameterValues(name);
            parameterMap.put(name, (values != null && values.length == 1) ? values[0] : values);
        }
        // 将解析出来的参数，转换成JSON并设置到body中保存
        this.body = JSONUtil.toJsonStr(parameterMap).getBytes(Charset.defaultCharset());
    }

    public void setBody(byte[] body) {
        this.body = body;

        try {
            if (this.request instanceof MultipartRequest) {
                //[文件上传请求相关的操作]

                //todo 将Json格式body数据转换为mp
                //this.setParameterMap(JsonUtil.json2map(body));
                String bodyStr = new String(body, "UTF-8");
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> bodyMap = objectMapper.readValue(bodyStr, new TypeReference<Map<String, Object>>() {
                });
                this.setParameterMap(bodyMap);
            }
        } catch (Exception e) {
            log.error("转换参数异常，参数：{}，异常：{}", body, e);
        }
    }

    /**
     * Description:读取请全体Body中数据 [从 customRequestWrapper中读取]
     *
     * @param
     * @return java.lang.String
     */
    public String getBodyString() {
        final InputStream inputStream = new ByteArrayInputStream(body);
        return inputStreamToString(inputStream);
    }

    /**
     * Description: 读取请求体Body中数据 [从HttpServletRequest中读取]
     *
     * @param request
     * @return java.lang.String
     */
    public String getBodyString(final ServletRequest request) {
        try {
            return inputStreamToString(request.getInputStream());
        } catch (IOException e) {
            log.error("Read Request Body IO_Stream Fail !", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Description: 将inputStream流里面的数据读取出来并转换为字符串形式
     *
     * @param inputStream
     * @return java.lang.String
     */
    private String inputStreamToString(InputStream inputStream) {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new InputStreamReader(inputStream, Charset.defaultCharset()));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            log.error("BufferedReader is Fail !", e);
            throw new RuntimeException(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return sb.toString();
    }


    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(body);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
            }

            @Override
            public int read() throws IOException {
                return inputStream.read();
            }
        };
    }


// [Get请求相关操作，封装Request.getParameter()中的相关参数 ]

    /**
     * Description: The default behavior of this method is to return getParameter(String name) on the wrapped request object.
     *
     * @param name
     * @return java.lang.String
     * @date 2024-05-13
     */
    @Override
    public String getParameter(String name) {
        String result = super.getParameter(name);
        // 如果参数获取不到则尝试从参数(自定义封装的存贮零时请求数据的集合)Map中获取，并且只返回第一个
        if (result == null && this.parameterMap.containsKey(name)) {
            result = this.parameterMap.get(name)[0];
        }
        return result;
    }

    /**
     * The default behavior of this method is to return getParameterMap() on the
     * wrapped request object.
     */
    @Override
    public Map<String, String[]> getParameterMap() {
        // 需要将原有的参数加上新参数 返回
        Map<String, String[]> map = new HashMap<>(super.getParameterMap());
        for (String key : this.parameterMap.keySet()) {
            map.put(key, this.parameterMap.get(key));
        }
        return Collections.unmodifiableMap(map);
    }

    /**
     * The default behavior of this method is to return
     * getParameterValues(String name) on the wrapped request object.
     *
     * @param name
     */
    @Override
    public String[] getParameterValues(String name) {
        String[] result = super.getParameterValues(name);
        if (result == null && this.parameterMap.containsKey(name)) {
            result = this.parameterMap.get(name);
        }
        return result;
    }

    /**
     * The default behavior of this method is to return getParameterNames() on
     * the wrapped request object.
     */
    @Override
    public Enumeration<String> getParameterNames() {
        Enumeration<String> parameterNames = super.getParameterNames();
        Set<String> names = new LinkedHashSet<>();
        if (parameterNames != null) {
            while (parameterNames.hasMoreElements()) {
                names.add(parameterNames.nextElement());
            }
        }
        // 添加后期设置的参数Map
        if (!this.parameterMap.isEmpty()) {
            names.addAll(this.parameterMap.keySet());
        }
        return Collections.enumeration(names);
    }

    /**
     * 设置参数map
     *
     * @param json2map
     */
    public void setParameterMap(Map<String, Object> json2map) {
        if (json2map != null && !json2map.isEmpty()) {
            for (String key : json2map.keySet()) {
                //获取map中对应key的value
                Object value = json2map.get(key);
                if (this.parameterMap.containsKey(key)) {
                    //如果额外参数HashLink中包含该参数，则在赋值加入到String[] 中
                    String[] originalArray = this.parameterMap.get(key);
                    int originalLength = originalArray.length;
                    originalArray = Arrays.copyOf(originalArray, originalLength + 1);
                    originalArray[originalLength] = String.valueOf(value);
                    //this.parameterMap.put(key, Collection.add(this.parameterMap.get(key), value));
                    this.parameterMap.put(key, originalArray);
                } else {
                    this.parameterMap.put(key, new String[]{String.valueOf(value)});
                }
            }
        }
    }
}
