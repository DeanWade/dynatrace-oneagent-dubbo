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
		Result result = null;
		try {
			//line 70=1.9.0
			//line 71=2.0.0
			//line 72=2.1.0
			//line 73=2.2.0
			//line 74=2.3.0
			result = invoker.invoke(invocation);
			return result;
		} catch (RpcException e) {
			try {
				if (outgoingRemoteCall != null) {
					if(e.getCause() != null){
						String causeExceptionName = e.getCause().getClass().getName();
						if(causeExceptionName.endsWith("BaseException") || causeExceptionName.endsWith("RuntimeException")){
							//do nothing
						}else{
							outgoingRemoteCall.error(e);
						}
					}else{
						outgoingRemoteCall.error(e);
					}
				}
			} catch (Throwable t) {}
			throw e;
		} finally {
			try {
				if (outgoingRemoteCall != null) {
					if(result != null && result.hasException()){
						Throwable e = result.getException();
						if( e instanceof  RpcException){//RPCException
							if(e.getCause() != null){
								String causeExceptionName = e.getCause().getClass().getName();
								if(causeExceptionName.endsWith("BaseException") || causeExceptionName.endsWith("RuntimeException")){
									//do nothing
								}else{
									outgoingRemoteCall.error(e); // not BaseException
								}
							}else{
								outgoingRemoteCall.error(e); // no cause exception
							}
						}else{// not RpcException
							String causeExceptionName = e.getClass().getName();
							if(causeExceptionName.endsWith("BaseException") || causeExceptionName.endsWith("RuntimeException")){
								//do nothing
							}else{
								outgoingRemoteCall.error(e); // not BaseException
							}
						}
					}
					outgoingRemoteCall.end();
				}
			} catch (Throwable t) {}
		}
	}

	public void version_dynatrace_oneagent_sdk_1_4_0(){}
	public void version_dynatrace_oneagent_dubbo_2_3_0(){}
}
