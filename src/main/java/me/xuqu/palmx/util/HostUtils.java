package me.xuqu.palmx.util;

import me.xuqu.palmx.common.PalmxConfig;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class HostUtils {

    public static String getLocalAddr() {
        String addr;
        try {
            addr = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            throw new RuntimeException("getLocalAddr err cause = ", e.getCause());
        }
        return addr;
    }

    public static int getLocalPort() {
        return PalmxConfig.getPalmxServerPort();
    }

}
