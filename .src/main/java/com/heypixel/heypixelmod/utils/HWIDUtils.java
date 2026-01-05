package com.heypixel.heypixelmod.utils;

import oshi.SystemInfo;
import oshi.hardware.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;

public class HWIDUtils {

    private static String cachedHWID = null;

    private HWIDUtils() {
    }

    public static String getHWID() {
        if (cachedHWID != null) {
            return cachedHWID;
        }

        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        StringBuilder sb = new StringBuilder();

        try {

            CentralProcessor processor = hal.getProcessor();
            if (processor != null && processor.getProcessorIdentifier() != null) {
                String processorID = processor.getProcessorIdentifier().getProcessorID();
                if (processorID != null && !processorID.isEmpty()) {
                    sb.append("CPU:").append(processorID).append(":");
                }
            }

            List<HWDiskStore> diskStores = hal.getDiskStores();
            if (diskStores != null && !diskStores.isEmpty()) {
                for (HWDiskStore disk : diskStores) {
                    String diskSerial = disk.getSerial();
                    if (diskSerial != null && !diskSerial.isEmpty() && !"Unknown".equalsIgnoreCase(diskSerial)) {
                        sb.append("DISK:").append(diskSerial).append(":");
                        break;
                    }
                }
            }

            ComputerSystem computerSystem = hal.getComputerSystem();
            Baseboard baseboard = computerSystem.getBaseboard();

            String boardSerial = baseboard.getSerialNumber();
            if (boardSerial != null && !boardSerial.isEmpty() && !"Unknown".equalsIgnoreCase(boardSerial)) {
                sb.append("MB_SERIAL:").append(boardSerial).append(":");
            }

            String boardModel = baseboard.getModel();
            if (boardModel != null && !boardModel.isEmpty() && !"Unknown".equalsIgnoreCase(boardModel)) {
                sb.append("MB_MODEL:").append(boardModel).append(":");
            }

        } catch (Exception e) {
            System.err.println("Error fetching hardware information: " + e.getMessage());
        }

        if (sb.length() == 0) {
            sb.append("Fallback:").append(si.getOperatingSystem().getNetworkParams().getHostName());
        }

        String fullHash = generateHash(sb.toString());

        cachedHWID = fullHash.length() > 25 ? fullHash.substring(0, 25) : fullHash;
        return cachedHWID;
    }

    private static String generateHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("SHA-256 algorithm not found.");
            return "";
        }
    }
}