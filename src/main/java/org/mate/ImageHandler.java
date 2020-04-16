package org.mate;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.List;

/**
 * Created by marceloeler on 21/09/18.
 */
public class ImageHandler {

    public static String screenShotDir;

    public static String takeScreenshot(String cmdStr) {
        String[] parts = cmdStr.split(":");
        String emulator = parts[1];
        String imgPath = parts[2];

        // repeat the following process untill the screenshot is properly taken
        // from time to time screencap will return a blank image (i.e., all white), and we don't want that.
        // Note: requires imagemagick package installed
        while (true) {
            cmdStr = "adb -s " + emulator + " shell screencap -p /sdcard/" + imgPath +
                    " && adb -s " + emulator + " pull /sdcard/" + imgPath;

            System.out.println(cmdStr);
            ADB.runCommand(cmdStr);

            String checkBlankCmd = String.format("identify -format \"%[fx:(mean==1)?1:0]\" %s", imgPath);
            List<String> resultLines = ADB.runCommand(checkBlankCmd);

            if (!resultLines.isEmpty() && "0".equals(resultLines.get(0).trim())) {
                break;
            }
        }

        return imgPath;
    }


    public static String markImage(String cmdStr) {

        try {
            System.out.println(cmdStr);
            String[] parts = cmdStr.split(":");
            String imageName = parts[1];
            int x = Integer.parseInt(parts[2].split("-")[1]);
            int y = Integer.parseInt(parts[3].split("-")[1]);
            int width = Integer.parseInt(parts[4].split("-")[1]);
            int height = Integer.parseInt(parts[5].split("-")[1]);

            String fileName = imageName.split("_")[0] + imageName.split("_")[1] + ".txt";

            Writer output;
            output = new BufferedWriter(new FileWriter(fileName, true));

            output.append(imageName)
                    .append(",")
                    .append(parts[6])
                    .append(",")
                    .append(parts[7])
                    .append("\n");

            output.close();

            BufferedImage img = ImageIO.read(new File(imageName));
            Graphics2D g2d = img.createGraphics();
            g2d.setColor(Color.RED);
            g2d.setStroke(new BasicStroke(5));
            g2d.drawRect(x, y, width, height);
            ImageIO.write(img, "PNG", new File(imageName));
            g2d.dispose();
        }catch (Exception e){
            System.out.println("EXCEPTION --->" + e.getMessage());
        }
        System.out.println(cmdStr);

        return "";
    }

    public static String calculateConstratRatio(String cmdStr) {

        String response = "21";
        try {
            System.out.println(cmdStr);
            String[] parts = cmdStr.split(":");
            String packageName = parts[1];
            String stateId = parts[2];
            String coord = parts[3];

            String[] positions = coord.split(",");
            int x1 = Integer.valueOf(positions[0]);
            int y1 = Integer.valueOf(positions[1]);
            int x2 = Integer.valueOf(positions[2]);
            int y2 = Integer.valueOf(positions[3]);

            String fileName = screenShotDir + packageName + "_" + stateId + ".png";
            System.out.println(fileName);
            System.out.println(coord);
            double contrastRatio = AccessibilityUtils.getContrastRatio(fileName, x1, y1, x2, y2);
            System.out.println("contrast ratio: " + contrastRatio);
            response = String.valueOf(contrastRatio);
        } catch (Exception e) {
            System.out.println("PROBLEMS CALCULATING CONTRAST RATIO");
            response = "21";
        }
        return response;
    }
}
