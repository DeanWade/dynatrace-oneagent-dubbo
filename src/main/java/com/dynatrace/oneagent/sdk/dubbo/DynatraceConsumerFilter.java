package com.dynatrace.oneagent.sdk.dubbo;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.dynatrace.oneagent.sdk.OneAgentSDKFactory;
import com.dynatrace.oneagent.sdk.api.OneAgentSDK;
import com.dynatrace.oneagent.sdk.api.OutgoingRemoteCallTracer;
import com.dynatrace.oneagent.sdk.api.enums.ChannelType;

import java.util.logging.Logger;

@Activate(group = Constants.CONSUMER, order = Integer.MAX_VALUE)
public class DynatraceConsumerFilter implements Filter {

    private static final String DYNATRACE_TAG_KEY = "x-dynatrace-tag";

    private static final String DYNATRACE_DUBBO_DISABLED = "dynatrace.dubbo.disable";

	private final OneAgentSDK oneAgentSdk;

    private boolean isDisabled;

	public DynatraceConsumerFilter() {
		oneAgentSdk = OneAgentSDKFactory.createInstance();
        isDisabled=Boolean.parseBoolean(System.getProperty(DYNATRACE_DUBBO_DISABLED));
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
	    if(isDisabled){
            return invoker.invoke(invocation);
        }
		OutgoingRemoteCallTracer outgoingRemoteCall = null;
		try {
			if (isActive()) {
				String serviceMethod = invocation.getMethodName();
				String serviceName = invoker.getInterface().getSimpleName();
				String serviceEndpoint = invoker.getUrl().getPath();
				String channelEndpoint = invoker.getUrl().getAddress();
				outgoingRemoteCall = oneAgentSdk.traceOutgoingRemoteCall(
						serviceMethod, serviceName, serviceEndpoint,
						ChannelType.TCP_IP, channelEndpoint);
				outgoingRemoteCall.setProtocolName("dubbo");
				outgoingRemoteCall.start();
				String outgoingTag = outgoingRemoteCall.getDynatraceStringTag();
				invocation.getAttachments().put(DYNATRACE_TAG_KEY, outgoingTag);
			}
		} catch (Exception e) { }

		try {
			//line 70=1.9.0
			//line 71=2.0.0
			return invoker.invoke(invocation);
		} catch (RpcException e) {
			throw e;
		} finally {
			try {
				if (outgoingRemoteCall != null) {
					outgoingRemoteCall.end();
				}
			} catch (Throwable t) {}
		}
	}

	public void version_dynatrace_oneagent_sdk_1_4_0(){}
	public void version_dynatrace_oneagent_dubbo_2_0_0(){}
}
