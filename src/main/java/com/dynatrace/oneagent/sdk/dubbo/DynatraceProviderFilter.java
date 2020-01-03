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
import com.dynatrace.oneagent.sdk.api.IncomingRemoteCallTracer;
import com.dynatrace.oneagent.sdk.api.OneAgentSDK;

@Activate(group = Constants.PROVIDER, order = Integer.MIN_VALUE)
public class DynatraceProviderFilter implements Filter {

	private static final String DYNATRACE_TAG_KEY = "x-dynatrace-tag";

    private static final String DYNATRACE_DUBBO_DISABLED = "dynatrace.dubbo.disable";

	private final OneAgentSDK oneAgentSdk;

    private boolean isDisabled;

	public DynatraceProviderFilter() {
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

		IncomingRemoteCallTracer incomingRemoteCall = null;
		try {
			if (isActive()) {
				String tagString = invocation.getAttachments().get(DYNATRACE_TAG_KEY);
				String serviceMethod = invocation.getMethodName();
				String serviceName = invoker.getInterface().getSimpleName();
				String serviceEndpoint = invoker.getUrl().getPath();
				incomingRemoteCall = oneAgentSdk.traceIncomingRemoteCall(serviceMethod, serviceName, serviceEndpoint);
				incomingRemoteCall.setProtocolName("dubbo");
				incomingRemoteCall.setDynatraceStringTag(tagString);
				incomingRemoteCall.start();
			}
		} catch (Throwable t) {}

		try {





			//line 70=1.9.0
			//line 71=2.0.0
			return invoker.invoke(invocation);
		} catch (RpcException e) {
			throw e;
		} finally {
			try {
				if (incomingRemoteCall != null) {
					incomingRemoteCall.end();
				}
			} catch (Throwable t) {}
		}
	}

	public void version_dynatrace_oneagent_sdk_1_4_0(){}
	public void version_dynatrace_oneagent_dubbo_midea_2_0_0(){}
}
