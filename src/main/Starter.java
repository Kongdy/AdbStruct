package main;

import java.io.File;
import java.util.*;

public class Starter {

    public static void main(String[] args) {
        AdbScriptManager.getInstance().prepareEnvironment();
        showChooseDeviceMode(AdbTools.getInstance().getDeviceList());
    }

    private static void showChooseDeviceMode(List<DeviceBean> deviceBeanList) {
        LogUtils.DEBUG("device list:");
        LogUtils.DEBUG("****************************\n");
        for (int i = 0; i < deviceBeanList.size(); i++) {
            String deviceOutput = (i + 1) + "." + deviceBeanList.get(i).getDeviceId();
            if (i == deviceBeanList.size() - 1) {
                deviceOutput += "\n";
            }
            LogUtils.DEBUG(deviceOutput);
        }
        LogUtils.DEBUG("****************************");
        LogUtils.DEBUG("please choose device and take the next step!!!\n");

        Scanner scanner = new Scanner(System.in);
        int index = scanner.nextInt();
        if (index > 0 && index <= deviceBeanList.size()) {
            DeviceBean dstDevice = deviceBeanList.get(index - 1);
            showHomeMode(dstDevice);
        } else {
            LogUtils.ERROR("invalided device index,please try again!!!");
            showChooseDeviceMode(deviceBeanList);
        }
    }

    private static void showHomeMode(DeviceBean dstDevice) {
        LogUtils.DEBUG("please choose program mode:");
        LogUtils.DEBUG("1.goto boot mode");
        LogUtils.DEBUG("2.goto script mode");
        LogUtils.DEBUG("3.back to device list");
        LogUtils.DEBUG("0.exit program");

        Scanner scanner = new Scanner(System.in);
        int index = scanner.nextInt();
        switch (index) {
            case 0:
                System.exit(0);
                break;
            case 1:
                showChooseFunctionMode(dstDevice);
                break;
            case 2:
                showScriptMode(dstDevice);
                break;
            case 3:
                showChooseDeviceMode(AdbTools.getInstance().getDeviceList());
                break;
            default:
                LogUtils.ERROR("unknown command,please try again!!!");
                showHomeMode(dstDevice);
                break;
        }
    }

    private static void showChooseFunctionMode(DeviceBean dstDevice) {
        LogUtils.DEBUG("please choose boot mode:");
        LogUtils.DEBUG("1.reboot device");
        LogUtils.DEBUG("2.reboot to recovery");
        LogUtils.DEBUG("3.rebootLoader");
        LogUtils.DEBUG("4.boot into TWRP Img");
        LogUtils.DEBUG("5.TWRP Restore");
        LogUtils.DEBUG("6.TWRP Backup");
        LogUtils.DEBUG("7.wait for device adb connect");
        LogUtils.DEBUG("8.back to home mode");
        LogUtils.DEBUG("0.exit program");

        Scanner scanner = new Scanner(System.in);

        int index = scanner.nextInt();
        switch (index) {
            case 1:
                AdbTools.getInstance().rebootDevice(dstDevice.getDeviceId());
                LogUtils.DEBUG("rebooting...");
                break;
            case 2:
                AdbTools.getInstance().rebootToRecovery(dstDevice.getDeviceId());
                break;
            case 3:
                AdbTools.getInstance().rebootLoader(dstDevice.getDeviceId());
                break;
            case 4:
                bootTWRPImg(dstDevice);
                LogUtils.DEBUG("boot script is running,back to device list");
                showChooseDeviceMode(AdbTools.getInstance().getDeviceList());
                break;
            case 5:
                handleTWRPRestore(dstDevice);
                LogUtils.DEBUG("restore finish,back to function choose mode");
                showChooseDeviceMode(AdbTools.getInstance().getDeviceList());
                break;
            case 6:
                handleTWRPBackUp(dstDevice);
                LogUtils.DEBUG("backUp finish,back to function choose mode");
                showChooseDeviceMode(AdbTools.getInstance().getDeviceList());
                break;
            case 7:
                AdbTools.getInstance().waitForDeviceAdbConnect(dstDevice.getDeviceId());
                LogUtils.DEBUG("wait " + dstDevice.getDeviceId() + " success,back to function choose mode");
                showChooseDeviceMode(AdbTools.getInstance().getDeviceList());
                break;
            case 8:
                showHomeMode(dstDevice);
                break;
            case 0:
                System.exit(0);
                break;
            default:
                LogUtils.ERROR("unknown command,please try again!!!");
                showChooseFunctionMode(dstDevice);
                break;
        }
    }

    private static void bootTWRPImg(DeviceBean dstDevice) {
        LogUtils.DEBUG("please drag img file to here!!!");
        Scanner scanner = new Scanner(System.in);

        String imgPath = scanner.nextLine();
        if (TextTools.isEmpty(imgPath)) {
            LogUtils.ERROR("imgPath files is empty please try again");
            bootTWRPImg(dstDevice);
            return;
        }

        AdbTools.getInstance().bootTWRPImg(imgPath, dstDevice.getDeviceId());
    }

    private static void handleTWRPRestore(DeviceBean dstDevice) {
        LogUtils.DEBUG("please input backUp file directory,and you can input 1 for back to function choose mode");
        Scanner scanner = new Scanner(System.in);

        String backUpDirectory = scanner.nextLine();
        int backFlag = scanner.nextInt();

        if (backFlag == 1) {
            showChooseFunctionMode(dstDevice);
            return;
        }

        if (TextTools.isEmpty(backUpDirectory)) {
            LogUtils.ERROR("directory is empty please try again");
            handleTWRPRestore(dstDevice);
            return;
        }

        File backUpDirectoryFile = new File(backUpDirectory);

        if (!backUpDirectoryFile.exists()) {
            LogUtils.ERROR("back up directory is not exits,please try again");
            handleTWRPRestore(dstDevice);
            return;
        }

        if (!backUpDirectoryFile.isDirectory()) {
            LogUtils.ERROR("back up directory is not a directory,please try again");
            handleTWRPRestore(dstDevice);
            return;
        }

        File[] files = backUpDirectoryFile.listFiles();

        if (null == files) {
            LogUtils.ERROR("back up directory is empty,please try again");
            handleTWRPRestore(dstDevice);
            return;
        }

        AdbTools.getInstance().rebootLoader(dstDevice.getDeviceId());

        LogUtils.DEBUG("wait 15 seconds");
        try {
            Thread.sleep(15 * 1000);
            AdbTools.getInstance().waitForDeviceFastbootConnect(dstDevice.getDeviceId());
            for (File file : files) {
                LogUtils.DEBUG("restore file:" + file.getName());
                AdbTools.getInstance().execTWRPRestore(file.getAbsolutePath(), dstDevice.getDeviceId());
                LogUtils.DEBUG("restore suc,waiting next...");
                Thread.sleep(10 * 1000);
            }
            AdbTools.getInstance().rebootDevice(dstDevice.getDeviceId());
            LogUtils.DEBUG("device is rebooting,wait device on line");
            AdbTools.getInstance().waitForDeviceAdbConnect(dstDevice.getDeviceId());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private static void handleTWRPBackUp(DeviceBean dstDevice) {
        File fileHome = new File(AdbTools.getInstance().getClass().getResource("/").getPath());

        File deviceBackUpHome = new File(fileHome.getAbsolutePath() + File.separator + dstDevice.getDeviceId());

        AdbTools.getInstance().execTWRPBackup(deviceBackUpHome.getAbsolutePath(), dstDevice.getDeviceId());
    }


    private static void showScriptMode(DeviceBean dstDevice) {
        LogUtils.DEBUG("please choose script mode:");
        LogUtils.DEBUG("1.auto publish");
        LogUtils.DEBUG("2.back to home");
        LogUtils.DEBUG("0.exit program");


        Scanner scanner = new Scanner(System.in);

        int index = scanner.nextInt();

        switch (index) {
            case 0:
                System.exit(0);
                break;
            case 1:
                autoPublish(dstDevice);
                break;
            case 2:
                showHomeMode(dstDevice);
                break;
            default:
                LogUtils.ERROR("unknown command,please try again!!!");
                showScriptMode(dstDevice);
                break;
        }
    }

    private static void autoPublish(DeviceBean dstDevice) {
        File fileHome = new File(AdbTools.getInstance().getClass().getResource("/").getPath());

        Calendar calendar = Calendar.getInstance(TimeZone.getDefault(), Locale.CHINA);

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        String oneLevel1FileHome = year + "-" + month + "-" + day;

        File publishFileLevel3Directory = new File(fileHome.getAbsolutePath() + File.separator + oneLevel1FileHome + File.separator + dstDevice.getDeviceId());

        if (!publishFileLevel3Directory.exists()) {
            LogUtils.ERROR("publish file not found! please check publish directory!");
            showScriptMode(dstDevice);
            return;
        }

        LogUtils.INFO("start publishing...");

        AdbScriptManager.getInstance().execGetRequest("/postmes", "device=" + dstDevice.getDeviceId(), "path=" + publishFileLevel3Directory.getAbsolutePath());

        LogUtils.INFO("all publish done!!! back to script choose mode");
        showScriptMode(dstDevice);

    }
}
