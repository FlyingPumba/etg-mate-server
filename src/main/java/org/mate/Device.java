package org.mate;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

/**
 * Created by marceloeler on 14/09/18.
 */
public class Device {

    public static Hashtable<String,Device> devices;

    private String deviceID;
    private String packageName;
    private boolean busy;
    private int APIVersion;
    private String emmaCoverageReceiver;

    public Device(String deviceID){
        this.deviceID = deviceID;
        this.packageName = "";
        this.emmaCoverageReceiver = "";
        this.busy = false;
        APIVersion = this.getAPIVersionFromADB();
    }

    public String getDeviceID() {
        return deviceID;
    }

    public void setDeviceID(String deviceID) {
        this.deviceID = deviceID;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
        if (!packageName.isEmpty()) {
            this.emmaCoverageReceiver = getEmmaCoverageReceiverFromADB();
        }
    }

    public void setBusy(boolean busy){
        this.busy = busy;
    }

    public boolean isBusy(){
        return this.busy;
    }

    public int getAPIVersion() {
        return APIVersion;
    }

    private int getAPIVersionFromADB(){
        String cmd = "adb -s " + deviceID + " shell getprop ro.build.version.sdk";
        System.out.println(cmd);
        List<String> result = ADB.runCommand(cmd);
        if (result != null && result.size() > 0) {
            System.out.println("API consulta: " + result.get(0));
            return Integer.valueOf(result.get(0));
        }
        return 23;
    }

    private String getEmmaCoverageReceiverFromADB(){
        String cmd = "adb -s " + deviceID + " shell dumpsys package | grep -i " + packageName + " | grep Emma | cut -d\" \" -f10";
        System.out.println(cmd);
        List<String> result = ADB.runCommand(cmd);
        if (result != null && result.size() > 0) {
            System.out.println("Emma coverage receiver class consulta: " + result.get(0));
            return result.get(0);
        }
        return "";
    }

    public String getCurrentPackageName() {
        String response="unknown";

        String cmd = "adb -s " + deviceID + "shell dumpsys activity recents | grep 'Recent #0' | cut -d= -f2 | sed 's| .*||' | cut -d '/' -f1";
        List<String> result = ADB.runCommand(cmd);
        if (result != null && result.size() > 0)
            response = result.get(0);
        System.out.println("activity: " + response);

        return response;
    }

    public String getCurrentActivity(){

        String response="unknown";
        String cmd = "adb -s " + deviceID +" shell dumpsys activity activities | grep mFocusedActivity | cut -d \" \" -f 6";
        if (getAPIVersion()==23 || getAPIVersion()==25){
            if (ADB.isWin) {
                cmd = "powershell -command " + "\"$focused = adb -s " + deviceID + " shell dumpsys activity activities "
                        + "| select-string mFocusedActivity ; \"$focused\".Line.split(\" \")[5]\"";
                System.out.println(cmd);
            } else {
                cmd = "adb -s " + deviceID + " shell dumpsys activity activities | grep mFocusedActivity | cut -d \" \" -f 6";
            }
        }

        if (getAPIVersion()==26 || getAPIVersion()==27){
            if (ADB.isWin) {
                cmd = "powershell -command " + "\"$focused = adb -s " + deviceID + " shell dumpsys activity activities "
                        + "| select-string mFocusedActivity ; \"$focused\".Line.split(\" \")[7]\"";
                System.out.println(cmd);
            } else {
                cmd = "adb -s " + deviceID + " shell dumpsys activity activities | grep mResumedActivity | cut -d \" \" -f 8";
            }
        }

        /*
        * 27.04.2019
        *
        * The record 'mFocusedActivity' is not available anymore under Windows for API Level 28 (tested on Nexus5 and PixelC),
        * although it is available still under Linux (tested on Nexus5), which is somewhat strange.
        * Instead, we need to search for the 'realActivity' record, pick the second one (seems to be the current active Activity)
        * and split on '='.
         */
        if (getAPIVersion() >= 28) {
            if (ADB.isWin) {
                cmd = "powershell -command " + "\"$activity = adb -s " + deviceID + " shell dumpsys activity activities "
                       + "| select-string \"realActivity\" ; $focused = $activity[1] ; $final = $focused -split '=' ; echo $final[1]\"";
                        // Alternatively use: "$focused.Line.split(=)[1] \"";
                System.out.println(cmd);
            } else {
                cmd = "adb -s " + deviceID + " shell dumpsys activity activities | grep mResumedActivity | cut -d \" \" -f 8";
            }
        }

        List<String> result = ADB.runCommand(cmd);
        if (result != null && result.size() > 0)
            response = result.get(0);
        System.out.println("activity: " + response);

        return response;
    }

    public List<String> getActivities() {
        String cmd = "";
        if (ADB.isWin) {
            System.out.println("Running windows get source lines command!");
            cmd = "powershell -command " + "\"aapt dump xmltree " + packageName + ".apk AndroidManifest.xml | python getActivityNames.py" + "\"";
        } else {
            cmd = "aapt dump xmltree " + packageName + ".apk AndroidManifest.xml | ./scripts/getActivityNames.py";
        }
        List<String> response = ADB.runCommand(cmd);
        System.out.println("activities:");
        for (String activity : response) {
            System.out.println("\t" + activity);
        }
        return response;
    }

    public List<String> getSourceLines() {
        String cmd = "";
        if (ADB.isWin) {
            System.out.println("Running windows get source lines command!");
            cmd = "powershell -command " + "\"python getSourceLines.py " + packageName + "\"";
        } else {
            cmd = "./scripts/getSourceLines.py " + packageName;
        }
        return ADB.runCommand(cmd);
    }

    public String clearApp() {
        if (packageName.isEmpty()) {
            System.out.println("Skipping clearApp command for empty package");
        }

        String cmd = "";
        if (ADB.isWin) {
            System.out.println("Running windows clear app command!");
            cmd = "powershell -command " + "\"python clearApp.py " + deviceID + " " + packageName + "\"";
        } else {
            cmd = "./scripts/clearApp.py " + deviceID + " " + packageName;
        }
        List<String> response = ADB.runCommand(cmd);
        String clearAppOutput = String.join("\n", response);

        // Grant all permissions after clearing package data
        grantAllPermissions();

        return clearAppOutput;
    }

    private void grantAllPermissions() {
        String permissionsCmd = "adb -s " + deviceID + " shell pm list permissions -d -g | grep permission: | cut -d':' -f2";
        List<String> permissions = ADB.runCommand(permissionsCmd);
        for (String permission: permissions) {
            grantPermission(permission);
        }
    }

    private void grantPermission(String permission) {
        String grantCmd = String.format("adb -s %s shell pm grant %s %s >/dev/null 2>&1",
                deviceID, packageName, permission);
        ADB.runCommand(grantCmd);
    }

    public String storeCurrentTraceFile() {
        System.out.println("Storing current Trace file!");
        String cmd = "";
        if (ADB.isWin) {
            cmd = "powershell -command " + "\"python storeCurrentTraceFile.py" + " " + deviceID + " " + packageName + "\"";
        } else {
            cmd = "./scripts/storeCurrentTraceFile.py " + deviceID + " " + packageName;
        }
        return String.join("\n", ADB.runCommand(cmd));
    }

    public String storeCoverageData(String chromosome, String entity) {
        System.out.println("Storing coverage data");
        String cmd = "";
        if (ADB.isWin) {
            System.out.println("Running windows storing coverage command!");
            cmd = "powershell -command " + "\"python storeCoverageData.py " + deviceID + " " + packageName + " " + chromosome + "\"";
        } else {
            cmd = "./scripts/storeCoverageData_mod.py " + deviceID + " " + packageName + " " + chromosome + " " + emmaCoverageReceiver;
        }
        if (entity != null) {
            cmd += " " + entity;
        }
        List<String> response = ADB.runCommand(cmd);
        String joinedResponse = String.join("\n", response);
        System.out.println(joinedResponse);
        return joinedResponse;
    }

    public String copyCoverageData(String chromosome_source, String chromosome_target, String entities) {
        System.out.println("Copying coverage data");
        String cmd = "";
        if (ADB.isWin) {
            System.out.println("Running windows copy coverage command!");
            cmd = "powershell -command " + "\"python copyCoverageData.py " + packageName + " " + chromosome_source
                    + " " + chromosome_target + " " + entities + "\"";
        } else {
            cmd = "./scripts/copyCoverageData.py " + packageName + " " + chromosome_source + " " + chromosome_target + " " + entities;
        }
        List<String> response = ADB.runCommand(cmd);
        return String.join("\n", response);
    }

    public String getCoverage(String chromosome) {
        String response="unknown";
        String cmd = "";
        if (ADB.isWin) {
            System.out.println("Running windows get coverage command!");
            cmd = "powershell -command " + "\"python getCoverage.py " + packageName + " " + chromosome + "\"";
        } else {
            cmd = "./scripts/getCoverage.py " + packageName + " " + chromosome;
        }
        List<String> result = ADB.runCommand(cmd);
        if (result != null && result.size() > 0)
            response = result.get(result.size() - 1);
        System.out.println("coverage: " + response);

        return response;
    }

    public List<String> getLineCoveredPercentage(String chromosome, String line) {
        String cmd = "";
        if (ADB.isWin) {
            System.out.println("Running windows get line coverage command!");
            cmd = "powershell -command " + "\"python getLineCoveredPercentage.py " + packageName + " " + chromosome + "\"";
        } else {
            cmd = "./scripts/getLineCoveredPercentage.py " + packageName + " " + chromosome;
        }
        try {
            // TODO: refactor and use ProcessRunner.runProces() (no Windows support yet)
            ProcessBuilder pb = new ProcessBuilder(Arrays.asList("bash", "-c", cmd));
            pb.redirectErrorStream(true);
            Process p = pb.start();
            BufferedWriter br = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
            br.write(line);
            br.flush();
            br.close();
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String _temp;
            List<String> result = new ArrayList<>();
            while ((_temp = in.readLine()) != null) {
                result.add(_temp);
            }

            System.out.println("result after command: " + result);
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getCombinedCoverage(String chromosomes) {
        String response="unknown";
        String cmd = "";
        if (ADB.isWin) {
            System.out.println("Running windows get combined coverage command!");
            cmd = "powershell -command " + "\"python getCombinedCoverage.py " + packageName + " " + chromosomes + "\"";
        } else {
            cmd = "./scripts/getCombinedCoverage.py " + packageName + " " + chromosomes;
        }
        List<String> result = ADB.runCommand(cmd);
        if (result != null && result.size() > 0)
            response = result.get(result.size() - 1);
        System.out.println("combined coverage: " + response);

        return response;
    }

    public String storeJsonTestCases(String jsonContent) {
        System.out.println("Storing json test cases");
        try {
            PrintWriter out = new PrintWriter(new FileOutputStream("mate-test-cases.json"), true);
            out.print(jsonContent);
            out.close();
        } catch (FileNotFoundException e) {
            System.out.println("A problem occurred dumping json test cases to file: " + e.toString());
        }

        return "";
    }

    public static void loadActiveDevices(){
        if (devices==null)
            devices = new Hashtable<String,Device>();
        String cmd = "adb devices";
        List<String> resultDevices = ADB.runCommand(cmd);
        for (String deviceStr:resultDevices){
            String devID="";
            if (deviceStr.contains("device") && !deviceStr.contains("attached")) {
                devID = deviceStr.replace("device", "");
                devID = devID.replace(" ", "");
                if (devID.length() > 13)
                    devID = devID.substring(0, devID.length() - 1);
                if (devices.get(devID) == null) {
                    Device device = new Device(devID);
                    devices.put(devID, device);
                }
            }
        }
    }

    public static void listActiveDevices() {
        for (String devID: devices.keySet()) {
            Device device = devices.get(devID);
            System.out.println(device.getDeviceID()+ " - " + device.isBusy()+ ": " + device.getPackageName());
        }
    }

    public static String allocateDevice(String cmdStr){
        String parts[] = cmdStr.split(":");
        String packageName = parts[1];

        if (Server2.emuName != null) {
            Device device = devices.get(Server2.emuName);
            device.setPackageName(packageName);
            device.setBusy(true);
            return Server2.emuName;
        }

        String deviceID = getDeviceRunningPackage(packageName);
        Device device = devices.get(deviceID);
        if (device!=null) {
            device.setPackageName(packageName);
            device.setBusy(true);
        }
/*
        if (deviceID.equals("")){
            int i=0;
            boolean emulatorFound = false;
            Enumeration<String> keys = emulatorsAllocated.keys();
            while (keys.hasMoreElements() && !emulatorFound){
                String key = keys.nextElement();
                boolean allocated = emulatorsAllocated.get(key);
                if (!allocated){
                    response = key;
                    emulatorFound=true;
                    emulatorsAllocated.put(response, Boolean.TRUE);
                    emulatorsPackage.put(response,packageName);
                    System.out.println("found: " + response);
                }
            }
            if (!emulatorFound)
                response="";
        }*/
        //response = "4f60d1bb";
        return deviceID;
    }

    public static String getDeviceRunningPackage(String packageName){
        for (String key: devices.keySet()){
            String cmd = "adb -s " + key + " shell ps " + packageName;
            List<String> result = ADB.runCommand(cmd);
            for (String res: result){
                System.out.println(res);
                if (res.contains(packageName))
                    return key;
            }
        }
        return "";
    }

    public static String releaseDevice(String cmdStr){
        String response = "";
        String[] parts = cmdStr.split(":");
        if (parts!=null){
            if (parts.length>0) {
                String deviceID = parts[1];
                Device device = devices.get(deviceID);
                if (device!=null) {
                    device.setPackageName("");
                    device.setBusy(false);
                    response = "released";
                }
            }
        }
        return response;
    }
}
