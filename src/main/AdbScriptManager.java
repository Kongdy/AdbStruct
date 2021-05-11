package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class AdbScriptManager {

    private final List<DeviceBean> deviceBeanList = new ArrayList<>();
    private static final String AUTOMATOR_SCRIPT_PATH = "assets\\test(1).py";
    private final String ip = "http://127.0.0.1:5000/";

    private static AdbScriptManager _instance;

    public static AdbScriptManager getInstance() {
        if(null == _instance) {
            _instance = new AdbScriptManager();
        }
        return _instance;
    }

    private AdbScriptManager() {
    }


    public void prepareEnvironment() {
        deviceBeanList.clear();
        deviceBeanList.addAll(AdbTools.getInstance().getDeviceList());

        File projectFile = new File(this.getClass().getResource("").getPath());
        String automatorScriptAbsolutePath = projectFile.getParent() + File.separator + AUTOMATOR_SCRIPT_PATH;

        try {
            Runtime.getRuntime().exec("cmd /c start " + automatorScriptAbsolutePath);
            LogUtils.DEBUG("wait 15 seconds for server start...");
            Thread.sleep(15 * 1000);
            LogUtils.DEBUG("start finish,check environment");
            if(checkConnection(ip+"getDevicesList")) {
                LogUtils.DEBUG("check finish,server is start success!");
            } else {
                LogUtils.ERROR("check finish,server start failed");
            }
        } catch (IOException e) {
            LogUtils.ERROR(e.toString());
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public String execGetRequest(String routeUrl,String... params) {
        String url = ip + routeUrl;
        return handleGetRequest(url,params);
    }

    public String handleGetRequest(String url, String... params) {
        StringBuilder result = new StringBuilder();
        BufferedReader in;
        try {
            URL requestUrl = parseUrl(url, params);
            if(null == requestUrl) {
                LogUtils.ERROR("get a null url");
                return result.toString();
            }

            HttpURLConnection urlConnection = (HttpURLConnection) requestUrl.openConnection();
            in = handleGet(urlConnection);
            if(null == in) {
                LogUtils.ERROR("get a null result");
                return result.toString();
            }
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result.toString();
    }

    private boolean checkConnection(String url, String... params) {

        try {
            URL requestUrl = parseUrl(url, params);
            assert requestUrl != null;
            HttpURLConnection urlConnection = (HttpURLConnection) requestUrl.openConnection();

            handleGet(urlConnection);
            return urlConnection.getResponseCode() == 200;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private URL parseUrl(String url, String... params) {
        try {
            if(TextTools.isEmpty(url)) {
                LogUtils.ERROR("get a null org url");
                return null;
            }
            boolean haveQueryBefore = url.contains("?");
            if(params.length > 0) {
                for(int i = 0 ; i < params.length;i++) {
                    if(i == 0) {
                        if(!haveQueryBefore) {
                            url += "?" + params[i];
                            continue;
                        }
                    }
                    //noinspection StringConcatenationInLoop
                    url += "&" + params[i];
                }
            }
            return new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private BufferedReader handleGet(HttpURLConnection urlConnection) {
        try {
            // 设置通用的请求属性
            urlConnection.setRequestProperty("accept", "*/*");
            urlConnection.setRequestProperty("connection", "Keep-Alive");
            urlConnection.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            urlConnection.setRequestMethod("GET");
            // 建立实际的连接
            urlConnection.connect();
            // 定义 BufferedReader输入流来读取URL的响应
            return new BufferedReader(new InputStreamReader(
                    urlConnection.getInputStream(), "gbk"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
