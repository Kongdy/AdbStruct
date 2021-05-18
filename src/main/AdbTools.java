package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class AdbTools {

    private static AdbTools _instance;
    private static final String ADB_RELATIVE_PATH = "adb\\adb.exe";
    private static final String FASTBOOT_RELATIVE_PATH = "adb\\fastboot.exe";

    private AdbTools() {
            URL url = this.getClass().getResource("");
            File projectFile = new File(url.getPath());
            ADB_ABSOLUTE_PATH = projectFile.getParentFile().getParent().replace("file:\\","") + File.separator + ADB_RELATIVE_PATH;

            LogUtils.DEBUG("ADB_ABSOLUTE_PATH:"+ADB_ABSOLUTE_PATH);
            FASTBOOT_ABSOLUTE_PATH = projectFile.getParentFile().getParent().replace("file:\\","") + File.separator + FASTBOOT_RELATIVE_PATH;
            LogUtils.DEBUG("FASTBOOT_ABSOLUTE_PATH:"+FASTBOOT_ABSOLUTE_PATH);
    }

    public static AdbTools getInstance() {
        if (_instance == null) {
            _instance = new AdbTools();
        }
        return _instance;
    }

    /* instance start **/
    private String ADB_ABSOLUTE_PATH;
    private String FASTBOOT_ABSOLUTE_PATH;

    /**
     * execute adb command
     *
     * @param commandStr command
     * @return result line list
     */
    public List<String> executeCommandWithLine(String commandStr) {
        List<String> resultList = new ArrayList<>();
        if (TextTools.isEmpty(commandStr)) {
            return resultList;
        }
        final String orgStr = commandStr;
        commandStr = commandStr.replace("adb", ADB_ABSOLUTE_PATH);
        commandStr = commandStr.replace("fastboot", FASTBOOT_ABSOLUTE_PATH);
        try {
            LogUtils.INFO("execute command:" + commandStr);
            Process process = Runtime.getRuntime().exec(commandStr);
            //handle result
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream(), "gbk"));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                LogUtils.DEBUG(line);
                resultList.add(line);
            }
            return resultList;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resultList;
    }

    /**
     * execute adb command
     *
     * @param commandStr command
     * @return result line str
     */
    public String executeCommand(String commandStr) {
        List<String> resultList = executeCommandWithLine(commandStr);
        StringBuilder stringBuilder = new StringBuilder();
        for (String str : resultList) {
            stringBuilder.append(str);
        }
        return stringBuilder.toString();
    }

    /**
     * pick all device
     *
     * @return device list
     */
    public List<DeviceBean> getDeviceList() {
        List<DeviceBean> deviceBeans = new ArrayList<>();

        String command = "adb devices";
        List<String> resultList = executeCommandWithLine(command);
        int startIndex = findStrContainsIndex(resultList,"List of devices attached")+1;
        if (startIndex > 0 && startIndex < resultList.size()) {
            for (int i = startIndex; i < resultList.size(); i++) {
                String line = resultList.get(i);
                if (TextTools.isEmpty(line)) {
                    continue;
                }
                String[] lineArray = line.split("\t");
                if (lineArray.length != 2) {
                    continue;
                }
                DeviceBean deviceBean = new DeviceBean();
                deviceBean.setDeviceId(lineArray[0]);
                deviceBean.setStatus(lineArray[1]);
                deviceBeans.add(deviceBean);
            }
        } else {
            LogUtils.ERROR("adb service start failed");
        }

        return deviceBeans;
    }

    /**
     * do twrp backup
     * @param dstDirectoryPath save directory
     * @param deviceId deviceID
     * @return command exec result
     */
    public String execTWRPBackup(String dstDirectoryPath,String deviceId) {
        if(TextTools.isEmpty(deviceId)) {
            LogUtils.ERROR("execTWRPBackup deviceId is empty");
            return null;
        }
        if(TextTools.isEmpty(dstDirectoryPath)) {
            LogUtils.ERROR("execTWRPBackup dstDirectoryPath is empty");
            return null;
        }
//        try {
//            File dstFile = null;
            File file = new File(dstDirectoryPath);
            if(!file.exists()) {
                //noinspection ResultOfMethodCallIgnored
                file.mkdirs();
            }
//            if(file.isFile()) {
//                dstFile = file;
//                if(!dstFile.exists()) {
//                    //noinspection ResultOfMethodCallIgnored
//                    dstFile.createNewFile();
//                }
//            }
//            else if(file.isDirectory()) {
//                String dstFilePath = file.getAbsolutePath()+File.separator+deviceId+"_backUp.ab";
//                dstFile = new File(dstFilePath);
//                //noinspection ResultOfMethodCallIgnored
//                dstFile.createNewFile();
//            }

//            if(null == dstFile) {
//                LogUtils.ERROR("create backup file failed,deviceId:"+deviceId);
//                return null;
//            }

            String command = "adb -s "+deviceId+" backup -f "+file.getAbsolutePath()+ " --twrp -all";
//            String command = "adb -s "+deviceId+" backup -f "+dstFile.getAbsolutePath()+ " -all";
            return executeCommand(command);

//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return null;
    }


    /**
     * do twrp restore
     * @param backUpFile ab file absolute path
     * @param deviceId deviceId
     * @return cmd exec callback
     */
    public String execTWRPRestore(String backUpFile,String deviceId) {
        if(TextTools.isEmpty(deviceId)) {
            LogUtils.ERROR("execTWRPRestore deviceId is empty");
            return null;
        }
        if(TextTools.isEmpty(backUpFile)) {
            LogUtils.ERROR("execTWRPRestore backUpFile is empty");
            return null;
        }

        //File file = new File(backUpFile+File.separator+"1.ab");
        File file = new File(backUpFile);
        if(!file.exists()) {
            LogUtils.ERROR("deviceId:"+deviceId+",backUp not exists");
            return null;
        }
        String command = "adb -s "+deviceId+" restore "+file.getAbsolutePath();
        return executeCommand(command);
    }

    /**
     * flash boot twrp img to device
     * @param twrpImgPath the img path that corresponding to the device
     * @param deviceId device id
     * @return command exec callback
     */
    public String bootTWRPImg(String twrpImgPath,String deviceId) {
        if(TextTools.isEmpty(deviceId)) {
            LogUtils.ERROR("bootTWRPImg deviceId is empty");
            return null;
        }
        if(TextTools.isEmpty(twrpImgPath)) {
            LogUtils.ERROR("bootTWRPImg twrpImgPath is empty");
            return null;
        }

        rebootLoader(deviceId);
        try {
            // wait 15 seconds
            Thread.sleep(15 * 1000);
            // wait device
            waitForDeviceFastbootConnect(deviceId);
            // device connect suc
            String bootCommand = "fastboot flash recovery "+twrpImgPath;
            return executeCommand(bootCommand);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * reboot and enter in recovery mode
     * @param deviceId deviceId
     * @return nothing
     */
    public String rebootToRecovery(String deviceId) {
        if(TextTools.isEmpty(deviceId)) {
            LogUtils.ERROR("rebootToRecovery deviceId is empty");
            return null;
        }
        String command = "adb -s "+deviceId+" reboot recovery";
        return executeCommand(command);
    }

    /**
     * just reboot device
     * @param deviceId deviceId
     * @return nothing
     */
    public String rebootDevice(String deviceId) {
        if(TextTools.isEmpty(deviceId)) {
            LogUtils.ERROR("rebootDevice deviceId is empty");
            return null;
        }
        String command = "adb -s "+deviceId+" reboot";
        return executeCommand(command);
    }

    /**
     * wait for device connect.
     * <h1>
     *     if not connect,the command will been waiting forever
     * </h1>
     * @param deviceId deviceId
     * @return nothing
     */
    public String waitForDeviceAdbConnect(String deviceId) {
        if(TextTools.isEmpty(deviceId)) {
            LogUtils.ERROR("waitForDeviceConnect deviceId is empty");
            return null;
        }
        String command = "adb -s "+deviceId+" wait-for-device";
        return executeCommand(command);
    }

    /**
     * wait for device fastboot connect
     * @param deviceId deviceId
     * @return nothing
     */
    public String waitForDeviceFastbootConnect(String deviceId) {
        if(TextTools.isEmpty(deviceId)) {
            LogUtils.ERROR("waitForDeviceFastbootConnect deviceId is empty");
            return null;
        }
        String command = "fastboot -s "+deviceId+" wait-for-device";
        return executeCommand(command);
    }

    /**
     * reboot and enter in bootloader mode
     * @param deviceId
     * @return
     */
    public String rebootLoader(String deviceId) {
        if(TextTools.isEmpty(deviceId)) {
            LogUtils.ERROR("waitForDeviceConnect deviceId is empty");
            return null;
        }
        final String rebootLoaderCommand = "adb -s "+deviceId+" reboot bootloader";
        return executeCommand(rebootLoaderCommand);
    }


    /**
     * find str is or not contain in str list
     * @param strList line str list
     * @param dstStr need str
     * @return find index,if not found,will return -1
     */
    private int findStrContainsIndex(List<String> strList,String dstStr) {
        int index = -1;
        for(int i = 0;i < strList.size();i++){
            String line = strList.get(i);
            if(null != line && line.contains(dstStr)) {
                index = i;
                break;
            }
        }
        return index;
    }

}
