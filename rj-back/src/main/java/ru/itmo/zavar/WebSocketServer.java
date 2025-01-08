package ru.itmo.zavar;

import static ru.itmo.zavar.World.logger;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolConfig;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;

public class WebSocketServer {
    public static final int DEFAULT_BOSS_THREADS = 1;
    public static final int DEFAULT_WORKER_THREADS = Runtime.getRuntime().availableProcessors() * 2;
    public static final int DEFAULT_PORT = 8080;

    private ChannelFuture future;
    private int bossThreads;
    private int workerThreads;
    private int port;

    public WebSocketServer(int port, int bossThreads, int workerThreads) {
        this.bossThreads = bossThreads;
        this.workerThreads = workerThreads;
        this.port = port;
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

    public ChannelInitializer<Channel> getWebSocketServerInitializer() {
        return new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(getHttpServerCodec());
                pipeline.addLast(getHttpObjectAggregator());
                pipeline.addLast(getWebSocketServerProtocolHandler());
                pipeline.addLast(getWebSocketFrameHandler());
            }
        };
    }

    public HttpServerCodec getHttpServerCodec() {
        return new HttpServerCodec();
    }

    public HttpObjectAggregator getHttpObjectAggregator() {
        return new HttpObjectAggregator(Integer.MAX_VALUE);
    }

    public WebSocketServerProtocolHandler getWebSocketServerProtocolHandler() {
        return new WebSocketServerProtocolHandler(
                WebSocketServerProtocolConfig.newBuilder()
                        .websocketPath("/ws")
                        .build());
    }

    public WebSocketFrameHandler getWebSocketFrameHandler() {
        return new WebSocketFrameHandler();
    }

    // @ChannelHandler.Sharable
    // private static class PingPongWebSocketHandler extends SimpleChannelInboundHandler<PingWebSocketFrame> {
    //     @Override
    //     protected void channelRead0(ChannelHandlerContext ctx, PingWebSocketFrame msg) throws Exception {
    //         ctx.writeAndFlush(new PongWebSocketFrame(msg.content().retain()));
    //     }
    // }

    public static class WebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
            if (frame instanceof TextWebSocketFrame) {
                String request = ((TextWebSocketFrame) frame).text().trim();
                logger.fine("received '" + request + "'");
                String response = request.toUpperCase();
                ctx.channel().writeAndFlush(new TextWebSocketFrame(response));
                logger.fine("sent '" + response + "'");
            } else {
                String message = "unsupported frame type: " + frame.getClass().getName();
                throw new UnsupportedOperationException(message);
            }
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
                super.userEventTriggered(ctx, evt);
            }
        }
    }

}
