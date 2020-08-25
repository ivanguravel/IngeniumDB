package org.forfree.ingenium.server.handlers;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ProcessingHandler extends ChannelInboundHandlerAdapter {

    // TODO
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ChannelFuture future = ctx.writeAndFlush(msg);
        future.addListener(ChannelFutureListener.CLOSE);
    }
}
