package com.jrasp.agent.core.util;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;

/**
 * @author jrasp
 * @date 2023-11-16
 */
public class IpUtil {
    private static final String priorityNetworkSegmentStr = "10.,172.,11.";
    private static final List<String> ignoreNiNames = Arrays.asList("vnic", "docker", "vmnet", "vmbox", "vbox");

    private static boolean isIgnoreNI(String niName) {
        for (String item : ignoreNiNames) {
            if (null != niName && null != item && niName.toLowerCase().contains(item)) {
                return true;
            }
        }
        return false;
    }

    public static String getlocalIp(boolean isV4address) {
        String ip = "";
        List<String> ips = fetchLocalIps(isV4address);
        if (isV4address) {
            List<String> filterIps = sortByPriority(new HashSet<String>(ips));
            if (filterIps.size() > 0) {
                ip = filterIps.get(0);
            }
            if (ips.size() >= 2) {
                for (String str : ips) {
                    if (str.startsWith("10.")) {
                        ip = str;
                        break;
                    }
                }
            }

            if (isBlank(ip)) {
                ip = "";
            }
        } else if (ips.size() > 0) {
            ip = ips.get(0);
        }
        return ip;
    }

    public static List<String> fetchLocalIps(boolean isV4address) {
        List<String> ips = new ArrayList<String>();
        Enumeration<NetworkInterface> networkInterface;
        try {
            networkInterface = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            return ips;
        }
        Set<String> ipsV4 = new HashSet<String>();
        Set<String> ipsV6 = new HashSet<String>();
        while (networkInterface.hasMoreElements()) {
            NetworkInterface ni = networkInterface.nextElement();
            // 忽略虚拟网卡的IP,docker 容器的IP
            String niName = (null != ni) ? ni.getName() : "";
            if (isIgnoreNI(niName)) {
                continue;
            }

            Enumeration<InetAddress> inetAddress = null;
            try {
                if (null != ni) {
                    inetAddress = ni.getInetAddresses();
                }
            } catch (Exception e) {
                // ignore
            }
            while (null != inetAddress && inetAddress.hasMoreElements()) {
                InetAddress ia = inetAddress.nextElement();
                if (!ia.isLoopbackAddress() && !ia.isLinkLocalAddress() && ia instanceof Inet6Address) {
                    String ipv6 = ia.getHostAddress();
                    int index = ipv6.indexOf('%');
                    if (index > 0) {
                        ipv6 = ipv6.substring(0, index);
                    }
                    ipsV6.add(ipv6);
                }
                String thisIp = ia.getHostAddress();
                // 排除 回送地址
                if (!ia.isLoopbackAddress() && !thisIp.contains(":")) {
                    ipsV4.add(thisIp);
                }
            }
        }

        return isV4address ? new ArrayList<String>(ipsV4) : new ArrayList<String>(ipsV6);
    }


    private static List<String> sortByPriority(Set<String> ips) {
        ArrayList<String> finalIps = new ArrayList<String>(ips);
        HashMap<String, Integer> priorityMap = new HashMap<String, Integer>();
        String[] priorityArr = priorityNetworkSegmentStr.split(",");
        for (int i = 0; i < priorityArr.length; i++) {
            priorityMap.put(priorityArr[i], i);
        }
        final HashMap<String, Integer> sortIdxMap = new HashMap<String, Integer>();
        for (int i = 0; i < finalIps.size(); i++) {
            //找到当前ip的前缀
            String thisIp = finalIps.get(i);
            String prefix = thisIp;
            for (String networkSegment : priorityArr) {
                if (thisIp.startsWith(networkSegment)) {
                    prefix = networkSegment;
                    break;
                }
            }
            if (priorityMap.containsKey(prefix)) {
                sortIdxMap.put(thisIp, priorityMap.get(prefix));
            } else {
                sortIdxMap.put(thisIp, priorityMap.size() + 1);
            }
        }

        Comparator<String> comparator = new Comparator<String>() {
            @Override
            public int compare(String ip1, String ip2) {
                return sortIdxMap.get(ip1) - sortIdxMap.get(ip2);
            }
        };
        Collections.sort(finalIps, comparator);
        return finalIps;
    }

    public static boolean isBlank(CharSequence cs) {
        int strLen = length(cs);
        if (strLen == 0) {
            return true;
        } else {
            for (int i = 0; i < strLen; ++i) {
                if (!Character.isWhitespace(cs.charAt(i))) {
                    return false;
                }
            }

            return true;
        }
    }

    public static int length(CharSequence cs) {
        return cs == null ? 0 : cs.length();
    }

}