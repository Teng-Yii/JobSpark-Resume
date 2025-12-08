package com.tengYii.jobspark.common.utils;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;

@Slf4j
public class SnowflakeUtil {

    private static final Snowflake SNOW_FLAKE = IdUtil.getSnowflake(getWorkId(), getDataCenterId());

    private SnowflakeUtil() {
    }

    /**
     * 雪花算法，生成唯一id。
     */
    public static synchronized long snowflakeId() {
        return SNOW_FLAKE.nextId();
    }

    public static synchronized long snowflakeId(long workerId, long dataCenterId) {
        Snowflake snowflake = IdUtil.getSnowflake(workerId, dataCenterId);
        return snowflake.nextId();
    }

    /**
     * 获取机器id
     */
    private static Long getWorkId() {
        try {
            String hostAddress = InetAddress.getLocalHost().getHostAddress();
            //commons-lang3 的版本，可以选择 3.9.0 及以上。太低的版本没有这个方法
            //StringUtils.toCodePoints 用于将字符串转换为一系列的Unicode码点(code points)
            int[] ints = StringUtils.toCodePoints(hostAddress);
            int sums = 0;
            for (int b : ints) {
                sums += b;
            }
            return (long) (sums % 32);
        } catch (UnknownHostException e) {
            log.error("UnknownHostException error.", e);
            // 如果获取失败，则使用随机数备用
            return RandomUtils.nextLong(0, 31);
        }
    }

    /**
     * 获取数据id
     */
    protected static long getDataCenterId() {
        long id = 0L;
        try {
            //获取本机(或者服务器ip地址)，类似 DESKTOP-abcdefg/192.168.1.87
            InetAddress ip = InetAddress.getLocalHost();
            NetworkInterface network = NetworkInterface.getByInetAddress(ip);
            if (network == null) {
                id = 1L;
            } else {
                //获取物理网卡地址
                byte[] mac = network.getHardwareAddress();
                if (null != mac) {
                    id = ((0x000000FF & (long) mac[mac.length - 2]) |
                            (0x0000FF00 & (((long) mac[mac.length - 1]) << 8))) >> 6;
                    id = id % (31 + 1);
                }
            }
        } catch (Exception e) {
            return RandomUtils.nextLong(0, 31);
        }
        return id;
    }
}
