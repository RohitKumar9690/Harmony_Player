package com.harmonyplayer.service;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.CharsetUtil;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.File;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WebStreamService {

    private JmDNS jmdns;
    private ServiceInfo serviceInfo;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    private int httpPort = 8080;
    private volatile boolean serverRunning = false;

    private volatile File currentAudioFile;
    private volatile boolean shouldStream = false;

    // fed by MainWindow (via NetworkStreamController.updatePlaybackPositionMs)
    private volatile long playbackPositionMs = 0;

    private volatile String currentSongTitle = "No song playing";
    private volatile String localIP = "localhost";

    private final Set<String> connectedClients = ConcurrentHashMap.newKeySet();

    public void startServer() {
        try {
            localIP = getLocalIPAddress();

            jmdns = JmDNS.create(InetAddress.getByName(localIP));
            serviceInfo = ServiceInfo.create(
                    "_http._tcp.local.",
                    "HarmonyPlayer",
                    httpPort,
                    "HarmonyPlayer Stream - http://" + localIP + ":" + httpPort);
            jmdns.registerService(serviceInfo);

            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup();

            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new HttpServerCodec());
                            ch.pipeline().addLast(new HttpObjectAggregator(65536));
                            ch.pipeline().addLast(new ChunkedWriteHandler());
                            ch.pipeline().addLast(new WebHandler());
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            serverChannel = bootstrap.bind(httpPort).sync().channel();
            serverRunning = true;

            System.out.println("✅ Web streaming server started: " + getConnectionURL());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopServer() {
        try {
            shouldStream = false;
            serverRunning = false;
            connectedClients.clear();

            if (jmdns != null) {
                jmdns.unregisterService(serviceInfo);
                jmdns.close();
            }
            if (serverChannel != null)
                serverChannel.close();
            if (bossGroup != null)
                bossGroup.shutdownGracefully();
            if (workerGroup != null)
                workerGroup.shutdownGracefully();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setAudioFile(File audioFile) {
        this.currentAudioFile = audioFile;
        this.shouldStream = (audioFile != null && audioFile.isFile());
        this.playbackPositionMs = 0;
    }

    public void stopStreaming() {
        this.shouldStream = false;
    }

    public void resumeStreaming() {
        if (currentAudioFile != null && currentAudioFile.isFile()) {
            this.shouldStream = true;
        }
    }

    public void updatePlaybackPositionMs(long ms) {
        this.playbackPositionMs = Math.max(0, ms);
    }

    public void setCurrentSong(String title, String artist) {
        String t = title == null ? "" : title.trim();
        String a = artist == null ? "" : artist.trim();
        String s = (t + " - " + a).trim();
        this.currentSongTitle = s.isBlank() ? "No song playing" : s;
    }

    public boolean isStreaming() {
        return serverRunning;
    }

    public int getConnectedClientsCount() {
        return connectedClients.size();
    }

    public List<String> getConnectedDevices() {
        return new ArrayList<>(connectedClients);
    }

    public String getConnectionURL() {
        return "http://" + localIP + ":" + httpPort;
    }

    public String getQRCodeURL() {
        String url = getConnectionURL();
        return "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=" +
                URLEncoder.encode(url, StandardCharsets.UTF_8);
    }

    private String getStreamContentType() {
        File f = currentAudioFile;
        if (f == null)
            return "audio/mpeg";
        String name = f.getName().toLowerCase(Locale.ROOT);
        if (name.endsWith(".mp3"))
            return "audio/mpeg";
        if (name.endsWith(".wav"))
            return "audio/wav";
        if (name.endsWith(".m4a") || name.endsWith(".mp4"))
            return "audio/mp4";
        if (name.endsWith(".aac"))
            return "audio/aac";
        return "application/octet-stream";
    }

    private class WebHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
            if (request.method() == HttpMethod.OPTIONS) {
                FullHttpResponse response = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
                response.headers().set("Access-Control-Allow-Origin", "*");
                response.headers().set("Access-Control-Allow-Methods", "GET, OPTIONS");
                response.headers().set("Access-Control-Allow-Headers", "Content-Type, Range");
                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                return;
            }

            String uri = request.uri();

            if (request.method() == HttpMethod.HEAD) {
                if (request.uri().startsWith("/audio")) {
                    handleAudio(ctx, request); // will send headers+body; acceptable
                    return;
                }
            }

            if (uri.equals("/") || uri.equals("/index.html")) {
                sendHtml(ctx);
                return;
            }
            if (uri.equals("/status")) {
                sendStatus(ctx);
                return;
            }
            if (uri.startsWith("/audio")) {
                handleAudio(ctx, request);
                return;
            }

            send404(ctx);
        }

        private void sendHtml(ChannelHandlerContext ctx) {
            String html = createWebPlayerHTML();

            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                    Unpooled.copiedBuffer(html, CharsetUtil.UTF_8));

            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            response.headers().set(HttpHeaderNames.CACHE_CONTROL, "no-cache");

            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }

        private void sendStatus(ChannelHandlerContext ctx) {
            long now = System.currentTimeMillis();
            String json = "{"
                    + "\"serverRunning\":" + serverRunning + ","
                    + "\"shouldStream\":" + shouldStream + ","
                    + "\"clients\":" + getConnectedClientsCount() + ","
                    + "\"song\":\"" + currentSongTitle.replace("\"", "'") + "\","
                    + "\"positionMs\":" + playbackPositionMs + ","
                    + "\"serverTimeMs\":" + now
                    + "}";

            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                    Unpooled.copiedBuffer(json, CharsetUtil.UTF_8));

            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            response.headers().set(HttpHeaderNames.CACHE_CONTROL, "no-cache");
            response.headers().set("Access-Control-Allow-Origin", "*");

            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }

        private void handleAudio(ChannelHandlerContext ctx, FullHttpRequest request) {
            String remote = String.valueOf(ctx.channel().remoteAddress());
            connectedClients.add(remote);
            ctx.channel().closeFuture().addListener(f -> connectedClients.remove(remote));

            if (!shouldStream || currentAudioFile == null || !currentAudioFile.isFile()) {
                FullHttpResponse response = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND,
                        Unpooled.copiedBuffer("No stream", CharsetUtil.UTF_8));
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                return;
            }

            final File file = currentAudioFile;

            RandomAccessFile raf;
            try {
                raf = new RandomAccessFile(file, "r");
            } catch (Exception e) {
                send500(ctx, "Cannot open file");
                return;
            }

            long fileLength;
            try {
                fileLength = raf.length();
            } catch (Exception e) {
                try {
                    raf.close();
                } catch (Exception ignored) {
                }
                send500(ctx, "Cannot read file length");
                return;
            }

            long start = 0;
            long end = fileLength - 1;

            String range = request.headers().get(HttpHeaderNames.RANGE);
            boolean isRange = range != null && range.startsWith("bytes=");

            if (isRange) {
                try {
                    String r = range.substring("bytes=".length());
                    String[] parts = r.split("-", 2);
                    start = Long.parseLong(parts[0]);
                    if (parts.length == 2 && !parts[1].isBlank()) {
                        end = Long.parseLong(parts[1]);
                    }
                    if (end >= fileLength)
                        end = fileLength - 1;
                    if (start < 0)
                        start = 0;
                    if (start > end)
                        start = 0;
                } catch (Exception ignored) {
                    isRange = false;
                    start = 0;
                    end = fileLength - 1;
                }
            }

            long contentLength = end - start + 1;

            HttpResponseStatus status = isRange ? HttpResponseStatus.PARTIAL_CONTENT : HttpResponseStatus.OK;
            DefaultHttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status);

            response.headers().set(HttpHeaderNames.CONTENT_TYPE, getStreamContentType());
            response.headers().set(HttpHeaderNames.ACCEPT_RANGES, "bytes");
            response.headers().set(HttpHeaderNames.CACHE_CONTROL, "no-cache");
            response.headers().set("Access-Control-Allow-Origin", "*");

            if (isRange) {
                response.headers().set(HttpHeaderNames.CONTENT_RANGE,
                        "bytes " + start + "-" + end + "/" + fileLength);
            }

            HttpUtil.setContentLength(response, contentLength);

            boolean keepAlive = HttpUtil.isKeepAlive(request);
            if (keepAlive) {
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            }

            ctx.write(response);

            ChannelFuture sendFileFuture = ctx.write(
                    new DefaultFileRegion(raf.getChannel(), start, contentLength),
                    ctx.newProgressivePromise());

            ChannelFuture last = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);

            last.addListener(f -> {
                try {
                    raf.close();
                } catch (Exception ignored) {
                }
                if (!keepAlive)
                    ctx.close();
            });

            sendFileFuture.addListener(f -> {
                if (!f.isSuccess()) {
                    try {
                        raf.close();
                    } catch (Exception ignored) {
                    }
                }
            });
        }

        private void send404(ChannelHandlerContext ctx) {
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND,
                    Unpooled.copiedBuffer("404 Not Found", CharsetUtil.UTF_8));
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }

        private void send500(ChannelHandlerContext ctx, String msg) {
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                    Unpooled.copiedBuffer(msg, CharsetUtil.UTF_8));
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }
    }

    private String createWebPlayerHTML() {
        return "<!DOCTYPE html>\n" +
                "<html><head><meta charset='utf-8' />\n" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0' />\n" +
                "<title>HarmonyPlayer Stream</title>\n" +
                "<style>\n" +
                "body{font-family:Segoe UI,system-ui,sans-serif;background:linear-gradient(135deg,#667eea,#764ba2);min-height:100vh;display:flex;align-items:center;justify-content:center;color:#fff;padding:20px;}\n"
                +
                ".box{background:rgba(255,255,255,.15);backdrop-filter:blur(18px);border-radius:20px;padding:26px;max-width:520px;width:100%;text-align:center;box-shadow:0 10px 30px rgba(0,0,0,.35);}\n"
                +
                "audio{width:100%;margin:16px 0;}\n" +
                "button{background:#1118;color:#fff;border:1px solid #fff3;padding:12px 18px;border-radius:14px;margin:6px;}\n"
                +
                "</style></head>\n" +
                "<body><div class='box'>\n" +
                "<h2>🎵 HarmonyPlayer Stream</h2>\n" +
                "<div id='song'>Loading...</div>\n" +
                "<div id='clients'></div>\n" +
                "<audio id='audio' controls playsinline preload='none'></audio>\n" +
                "<div>\n" +
                " <button onclick='syncPlay()'>▶ Play (Sync)</button>\n" +
                " <button onclick='audio.pause()'>⏸ Pause</button>\n" +
                "</div>\n" +
                "<small>Tap Play (Sync) on each device (iOS blocks autoplay).</small>\n" +
                "</div>\n" +
                "<script>\n" +
                "const audio=document.getElementById('audio');\n" +
                "const song=document.getElementById('song');\n" +
                "const clients=document.getElementById('clients');\n" +
                "\n" +
                "async function getStatus(){ const r=await fetch('/status'); return await r.json(); }\n" +
                "\n" +
                "async function syncPlay(){\n" +
                "  const st=await getStatus();\n" +
                "  song.textContent='♪ '+st.song;\n" +
                "  clients.textContent='📱 '+st.clients+' device(s) connected';\n" +
                "  if(!st.shouldStream){ alert('No song streaming right now'); return; }\n" +
                "\n" +
                "  const target=(st.positionMs||0)/1000.0;\n" +
                "  const newSrc='/audio?v=' + Date.now();\n" +
                "\n" +
                "  // Set src and load\n" +
                "  if(audio.src !== location.origin + newSrc){\n" +
                "    audio.src = newSrc;\n" +
                "    audio.load();\n" +
                "  }\n" +
                "\n" +
                "  const go = async () => {\n" +
                "    try {\n" +
                "      audio.currentTime = target;\n" +
                "      await audio.play();\n" +
                "    } catch(e) {\n" +
                "      // iOS sometimes needs a second tap\n" +
                "      console.log(e);\n" +
                "      alert('Tap Play (Sync) again');\n" +
                "    }\n" +
                "  };\n" +
                "\n" +
                "  // Wait for metadata before seeking (IMPORTANT for mobile)\n" +
                "  if (isFinite(audio.duration) && audio.duration > 0) {\n" +
                "    await go();\n" +
                "  } else {\n" +
                "    audio.onloadedmetadata = async () => { audio.onloadedmetadata=null; await go(); };\n" +
                "  }\n" +
                "}\n" +
                "\n" +
                "async function periodicSync(){\n" +
                "  try{\n" +
                "    const st=await getStatus();\n" +
                "    song.textContent='♪ '+st.song;\n" +
                "    clients.textContent='📱 '+st.clients+' device(s) connected';\n" +
                "    if(!st.shouldStream) return;\n" +
                "    if(audio.paused) return;\n" +
                "    const target=(st.positionMs||0)/1000.0;\n" +
                "    const diff=target-audio.currentTime;\n" +
                "    if(Math.abs(diff)>0.35){ audio.currentTime=target; }\n" +
                "    else if(diff>0.08){ audio.playbackRate=1.03; }\n" +
                "    else if(diff<-0.08){ audio.playbackRate=0.97; }\n" +
                "    else { audio.playbackRate=1.0; }\n" +
                "  }catch(e){}\n" +
                "}\n" +
                "setInterval(periodicSync, 1500);\n" +
                "periodicSync();\n" +
                "</script></body></html>";
    }

    private String getLocalIPAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp())
                    continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr.isSiteLocalAddress() && !addr.isLoopbackAddress()) {
                        String ip = addr.getHostAddress();
                        if (ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+"))
                            return ip;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "localhost";
    }
}