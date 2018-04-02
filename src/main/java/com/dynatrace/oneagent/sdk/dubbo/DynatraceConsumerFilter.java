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

//    private static final com.alibaba.dubbo.common.logger.Logger logger = LoggerFactory.getLogger(DynatraceConsumerFilter.class);

    private static final String DYNATRACE_TAG_KEY = "dynatrace-trace-tag";

    private static final String DYNATRACE_DUBBO_DISABLED = "dynatrace.dubbo.disabled";

	private static final String DYNATRACE_DUBBO_SERVICE_FULLNAME = "dynatrace.dubbo.service.fullname";

	private final OneAgentSDK oneAgentSdk;

    private boolean isDisabled = false;

    private boolean isFullName = false;


	public DynatraceConsumerFilter() {
		oneAgentSdk = OneAgentSDKFactory.createInstance();
        isDisabled=Boolean.parseBoolean(System.getProperty(DYNATRACE_DUBBO_DISABLED));
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
	    if(isDisabled){
            return invoker.invoke(invocation);
        }
		OutgoingRemoteCallTracer outgoingRemoteCall = null;
		try {
			if (isActive()) {
				String serviceMethod = invocation.getMethodName();
				String serviceName = isFullName?invoker.getInterface().getName():invoker.getInterface().getSimpleName();
				String serviceEndpoint = invoker.getUrl().toServiceString();
				String channelEndpoint = invoker.getUrl().getAddress();
				outgoingRemoteCall = oneAgentSdk.traceOutgoingRemoteCall(serviceMethod, serviceName, serviceEndpoint,
						ChannelType.TCP_IP, channelEndpoint);
				outgoingRemoteCall.start();
				String outgoingTag = outgoingRemoteCall.getDynatraceStringTag();
				invocation.getAttachments().put(DYNATRACE_TAG_KEY, outgoingTag);
			}
		} catch (Exception e) {
		}
		Result result = null;
		try {
			result = invoker.invoke(invocation);
			return result;
		} catch (RpcException e) {
			if (outgoingRemoteCall != null) {
				outgoingRemoteCall.error(e);
			}
			throw e;
		} finally {
			if (outgoingRemoteCall != null) {
				if(result != null && result.hasException()){
					outgoingRemoteCall.error(result.getException());
				}
				outgoingRemoteCall.end();
			}
		}
	}

}
