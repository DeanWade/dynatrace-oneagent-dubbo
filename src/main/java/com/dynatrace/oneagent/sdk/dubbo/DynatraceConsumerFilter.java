package com.dynatrace.oneagent.sdk.dubbo;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.dynatrace.oneagent.sdk.OneAgentSDKFactory;
import com.dynatrace.oneagent.sdk.api.OneAgentSDK;
import com.dynatrace.oneagent.sdk.api.OutgoingRemoteCallTracer;
import com.dynatrace.oneagent.sdk.api.enums.ChannelType;

@Activate(group = Constants.CONSUMER, order = Integer.MAX_VALUE)
public class DynatraceConsumerFilter implements Filter {

	private static final String DYNATRACE_TAG_KEY = "dtdTraceTagInfo";

	private final OneAgentSDK oneAgentSdk;

	public DynatraceConsumerFilter() {
		oneAgentSdk = OneAgentSDKFactory.createInstance();
	}

	private boolean isActive() {
		switch (oneAgentSdk.getCurrentState()) {
		case ACTIVE:
			return true;
		case PERMANENTLY_INACTIVE:
			return false;
		case TEMPORARILY_INACTIVE:
			return false;
		default:
			return false;
		}
	}

	public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
		OutgoingRemoteCallTracer outgoingRemoteCall = null;
		try {
			if (isActive()) {
				String serviceMethod = invocation.getMethodName();
				String serviceName = invoker.getInterface().getName();
				String serviceEndpoint = invoker.getUrl().getAbsolutePath();
				String channelEndpoint = invoker.getUrl().getAddress();
				outgoingRemoteCall = oneAgentSdk.traceOutgoingRemoteCall(serviceMethod, serviceName, serviceEndpoint,
						ChannelType.TCP_IP, channelEndpoint);
				outgoingRemoteCall.start();
				String outgoingTag = outgoingRemoteCall.getDynatraceStringTag();
				invocation.getAttachments().put(DYNATRACE_TAG_KEY, outgoingTag);
			}
		} catch (Exception e) {
		}

		try {
			return invoker.invoke(invocation);
		} catch (RpcException e) {
			if (outgoingRemoteCall != null) {
				outgoingRemoteCall.error(e);
			}
			throw e;
		} finally {
			if (outgoingRemoteCall != null) {
				outgoingRemoteCall.end();
			}
		}

	}

}
