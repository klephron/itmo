package ru.itmo.zavar.websocket;

import static ru.itmo.zavar.World.logger;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolConfig;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import ru.itmo.zavar.websocket.pipeline.WebSocketSessionHandler;
import ru.itmo.zavar.websocket.pipeline.WebSocketTextDecoder;
import ru.itmo.zavar.websocket.pipeline.WebSocketTextEncoder;
import ru.itmo.zavar.websocket.pipeline.WebSocketWorldHandler;

public class WebSocketServer {
    public static final int DEFAULT_BOSS_THREADS = 1;
    public static final int DEFAULT_WORKER_THREADS = Runtime.getRuntime().availableProcessors() * 2;
    public static final int DEFAULT_PORT = 8080;

    private ChannelFuture future;
    private int bossThreads;
    private int workerThreads;
    private int port;

    private final SessionManager sessionManager;
    private final WebSocketSessionHandler webSocketSessionHandler;
    private final WebSocketWorldHandler webSocketWorldHandler;
    private final WebSocketTextEncoder webSocketTextEncoder;
    private final WebSocketTextDecoder webSocketTextDecoder;

    public WebSocketServer(int port, int bossThreads, int workerThreads) {
        this.bossThreads = bossThreads;
        this.workerThreads = workerThreads;
        this.port = port;

        this.sessionManager = new SessionManager();
        this.webSocketWorldHandler = new WebSocketWorldHandler();
        this.webSocketSessionHandler = new WebSocketSessionHandler(this.sessionManager, this.webSocketWorldHandler);
        this.webSocketTextEncoder = new WebSocketTextEncoder();
        this.webSocketTextDecoder = new WebSocketTextDecoder();
    }

    public WebSocketServer(int port) {
        this(port, DEFAULT_BOSS_THREADS, DEFAULT_WORKER_THREADS);
    }

    public WebSocketServer() {
        this(DEFAULT_PORT, DEFAULT_BOSS_THREADS, DEFAULT_WORKER_THREADS);
    }

    public void start() throws InterruptedException {
        EventLoopGroup boss = new NioEventLoopGroup(bossThreads);
        EventLoopGroup worker = new NioEventLoopGroup(workerThreads);
        ServerBootstrap boot = new ServerBootstrap();
        boot.group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(getWebSocketServerInitializer());
        try {
            future = boot.bind(port).sync(); // wait until binding
            future.addListener(evt -> logger.info("started wsserver, active port: " + port));
            future.channel().closeFuture().addListener(evt -> {
                logger.info("closed wsserver");
            }).sync();
            // immediately stop when can't bind
        } catch (InterruptedException e) {
            throw e;
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
            logger.info("shutdown event loop groups");
        }
    }

    public void stop() {
        if (future != null && future.channel().isOpen()) {
            future.channel().close();
        }
    }

    private ChannelInitializer<Channel> getWebSocketServerInitializer() {
        return new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(getHttpServerCodec());
                pipeline.addLast(getHttpObjectAggregator());
                pipeline.addLast(getWebSocketServerProtocolHandler());
                pipeline.addLast(getWebSocketTextEncoder());
                pipeline.addLast(getWebSocketTextDecoder());
                pipeline.addLast(WebSocketSessionHandler.NAME, getWebSocketSessionHandler());
            }
        };
    }

    private HttpServerCodec getHttpServerCodec() {
        return new HttpServerCodec();
    }

    private HttpObjectAggregator getHttpObjectAggregator() {
        return new HttpObjectAggregator(Integer.MAX_VALUE);
    }

    private WebSocketServerProtocolHandler getWebSocketServerProtocolHandler() {
        return new WebSocketServerProtocolHandler(
                WebSocketServerProtocolConfig.newBuilder()
                        .websocketPath("/ws")
                        .build());
    }

    private WebSocketSessionHandler getWebSocketSessionHandler() {
        return webSocketSessionHandler;
    }

    private WebSocketTextEncoder getWebSocketTextEncoder() {
        return webSocketTextEncoder;
    }

    private WebSocketTextDecoder getWebSocketTextDecoder() {
        return webSocketTextDecoder;
    }
}
