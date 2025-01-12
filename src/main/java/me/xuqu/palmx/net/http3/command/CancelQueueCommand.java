/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.xuqu.palmx.net.http3.command;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.DefaultHttp2ResetFrame;
import io.netty.handler.codec.http2.Http2Error;

public class CancelQueueCommand extends QuicStreamChannelCommand {
    private final Http2Error error;

    public CancelQueueCommand(QuicStreamChannelFuture streamChannelFuture, Http2Error error) {
        super(streamChannelFuture);
        this.error = error;
    }

    public static CancelQueueCommand createCommand(QuicStreamChannelFuture streamChannelFuture, Http2Error error) {
        return new CancelQueueCommand(streamChannelFuture, error);
    }

    @Override
    public void doSend(ChannelHandlerContext ctx, ChannelPromise promise) {
        ctx.write(new DefaultHttp2ResetFrame(error), promise);
    }
}
