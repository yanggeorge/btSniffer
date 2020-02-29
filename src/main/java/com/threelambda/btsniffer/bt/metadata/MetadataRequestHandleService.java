package com.threelambda.btsniffer.bt.metadata;

import com.threelambda.btsniffer.bt.util.Util;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author ym
 * @date 2019/12/05
 */
@Slf4j
@Component
public class MetadataRequestHandleService implements ApplicationListener<ContextStartedEvent> {
    @Resource(name = "metadataRequestQueue")
    private BlockingQueue<MetadataRequest> metadataRequestQueue;
    @Resource(name = "metadataQueue")
    private BlockingQueue<Metadata> metadataQueue;
    @Resource(name = "scheduleExecutor")
    private ScheduledExecutorService scheduleExecutor;
    @Resource(name = "metadataExecutor")
    private ExecutorService metadataExecutor;
    @Resource(name = "metadataRequestExecutor")
    private ExecutorService metadataRequestExecutor;
    @Resource(name = "eventLoopGroup")
    private EventLoopGroup eventLoopGroup;

    @Override
    public void onApplicationEvent(ContextStartedEvent contextStartedEvent) {
        try {
            log.info("metadataRequest handler start");
            Runnable handleMetadataRequest = this::handleMetadataRequest;
            metadataRequestExecutor.submit(handleMetadataRequest);
            Runnable handleMetadata = this::handleMetadata;
            metadataExecutor.submit(handleMetadata);
        } catch (Exception e) {
            log.error("error", e);
        }
    }

    private void handleMetadata() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Metadata metadata = metadataQueue.poll(1, TimeUnit.SECONDS);
                if (metadata != null) {
                    try {
                        Map<String, Object> metadataDic = Util.decode(metadata.getMetadata());
                        log.info("infoHashHex={}|name={}|length={}", metadata.getInfoHashHex(),metadataDic.get("name"),metadataDic.get("length"));
                    } catch (Exception e) {
                        log.error("error|infoHashHex=" + metadata.getInfoHashHex()
                                        + "|rawMetadata="+ ByteBufUtil.hexDump(metadata.getMetadata()), e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("error", e);
        }
    }

    private void handleMetadataRequest() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                MetadataRequest request = metadataRequestQueue.poll(1, TimeUnit.SECONDS);
                if (request != null) {
                    log.debug("metadataRequest={}", request);
                    submitRequestMetadataJob(request);
                }
            }
        } catch (Exception e) {
            log.error("error", e);
        }
    }

    /**
     * 提交获取元数据的任务
     *
     * @param request
     */
    private void submitRequestMetadataJob(MetadataRequest request) {
        Runnable r = () -> {
            final String infoHashHex = request.getInfoHashHex();
            final String ip = request.getIp();
            final int port = request.getPort();

            Bootstrap b = new Bootstrap();
            b.group(eventLoopGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel sc) throws Exception {
                            ChannelPipeline p = sc.pipeline();
                            int timeout = 15;
                            p.addLast(new ReadTimeoutHandler(timeout, TimeUnit.SECONDS));
                            p.addLast(new MetadataDecoder());
                            p.addLast(new MetadataHandler(infoHashHex, ip, port, metadataQueue));
                        }
                    });
            b.connect(new InetSocketAddress(ip, port));
        };
        scheduleExecutor.submit(r);
    }
}
