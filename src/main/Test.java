package main;

import java.util.List;

public class Test {


    public static void main(String[] args) {
        List<DeviceBean> deviceBeanList = AdbTools.getInstance().getDeviceList();
        int deviceCount = deviceBeanList.size();
    }
}
