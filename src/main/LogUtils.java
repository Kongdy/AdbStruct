package main;

public class LogUtils {

    private static final String TAG = "AdbTools/";
    private static final String ERROR_TAB = "ERROR:";
    private static final String DEBUG_TAB = "DEBUG:";
    private static final String INFO_TAB = "INFO:";
    private static final String WARNING_TAB = "WARNING:";


    public static void ERROR(String text) {
        print(ERROR_TAB,text);
    }

    public static void DEBUG(String text) {
        print(DEBUG_TAB,text);
    }

    public static void INFO(String text) {
        print(INFO_TAB,text);
    }

    public static void WARNING(String text) {
        print(WARNING_TAB,text);
    }

    private static void print(String tab,String text) {
        if(TextTools.isEmpty(text) || TextTools.isEmpty(tab)) {
            return;
        }
        System.out.println(TAG+tab+text);
    }

}
