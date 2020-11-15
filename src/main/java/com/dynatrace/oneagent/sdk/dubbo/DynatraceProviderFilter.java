package com.dynatrace.oneagent.sdk.dubbo;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.*;
import com.dynatrace.oneagent.sdk.OneAgentSDKFactory;
import com.dynatrace.oneagent.sdk.api.IncomingRemoteCallTracer;
import com.dynatrace.oneagent.sdk.api.OneAgentSDK;

@Activate(group = Constants.PROVIDER, order = Integer.MIN_VALUE)
public class DynatraceProviderFilter implements Filter {

	private static final String DYNATRACE_TAG_KEY = "x-dynatrace-tag";

	private final OneAgentSDK oneAgentSdk;

	public DynatraceProviderFilter() {
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

	private void isVersion20201112_Dubbo253_SDK14() {
	}

	public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
		IncomingRemoteCallTracer incomingRemoteCall = null;
		try {
			if (isActive()) {
				String tagString = invocation.getAttachments().get(DYNATRACE_TAG_KEY);
				if(tagString != null && !"".equals(tagString)){
					String serviceMethod = invocation.getMethodName();
					String serviceName = invoker.getInterface().getSimpleName();
					String serviceEndpoint = invoker.getUrl().getPath();
					incomingRemoteCall = oneAgentSdk.traceIncomingRemoteCall(serviceMethod, serviceName, serviceEndpoint);
					incomingRemoteCall.setProtocolName("dubbo");
					incomingRemoteCall.setDynatraceStringTag(tagString);
					incomingRemoteCall.start();
				}
			}
		} catch (Throwable t) {
		}
		Result result = null;
		try {
			result = invoker.invoke(invocation);
			return result;
		} catch (RpcException e) {
			if (incomingRemoteCall != null) {
				incomingRemoteCall.error(e);
			}
			throw e;
		} finally {
			if (incomingRemoteCall != null) {
				if(result != null && result.hasException()){
					incomingRemoteCall.error(result.getException());
				}
				incomingRemoteCall.end();
			}
		}

	}

}
