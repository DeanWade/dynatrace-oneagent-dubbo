package com.dynatrace.oneagent.sdk.dubbo;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
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

	private static final String DYNATRACE_TAG_KEY = "dtdTraceTagInfo";
	
	private static final String DYNATRACE_DUBBO_SERVICE_FULLNAME = "dynatrace.dubbo.service.fullname";

	private final OneAgentSDK oneAgentSdk;
	
	private boolean isFullName = false;

	public DynatraceProviderFilter() {
		oneAgentSdk = OneAgentSDKFactory.createInstance();
		isFullName=Boolean.parseBoolean(System.getProperty(DYNATRACE_DUBBO_SERVICE_FULLNAME));
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
		IncomingRemoteCallTracer incomingRemoteCall = null;
		try {
			String tagString = invocation.getAttachments().get(DYNATRACE_TAG_KEY);
			if (tagString != null && isActive()) {
				String serviceMethod = invocation.getMethodName();
				String serviceName = isFullName?invoker.getInterface().getName():invoker.getInterface().getSimpleName();
				String serviceEndpoint = invoker.getUrl().toServiceString();
				incomingRemoteCall = oneAgentSdk.traceIncomingRemoteCall(serviceMethod, serviceName, serviceEndpoint);
				incomingRemoteCall.setDynatraceStringTag(tagString);
				incomingRemoteCall.start();
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
