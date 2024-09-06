package me.xuqu.palmx.net.netty;

import io.netty.channel.Channel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.incubator.channel.uring.IOUring;
import io.netty.incubator.channel.uring.IOUringDatagramChannel;
import lombok.extern.slf4j.Slf4j;
import me.xuqu.palmx.common.PalmxConfig;

/**
 * 处理响应数据包
 */
@Slf4j
public class DatagramChannelHandler {

    public static Class<? extends Channel> getChannelClass() {
        boolean iOUringAvailable = IOUring.isAvailable();
        boolean ioUringEnable = PalmxConfig.getIoUringEnable();
        if (iOUringAvailable && ioUringEnable) {
            return IOUringDatagramChannel.class;
        }
        log.info("get iOUringDatagramChannel fail, available = {} , enable =  {}", iOUringAvailable, ioUringEnable);
        return NioDatagramChannel.class;
    }
}
