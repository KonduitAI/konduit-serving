package ai.konduit.serving.vertx.protocols.grpc.api;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 * <pre>
 * The main grpc service definition.
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.20.0)",
    comments = "Source: grpc-service.proto")
public final class ApiGrpc {

  private ApiGrpc() {}

  private static <T> io.grpc.stub.StreamObserver<T> toObserver(final io.vertx.core.Handler<io.vertx.core.AsyncResult<T>> handler) {
    return new io.grpc.stub.StreamObserver<T>() {
      private volatile boolean resolved = false;
      @Override
      public void onNext(T value) {
        if (!resolved) {
          resolved = true;
          handler.handle(io.vertx.core.Future.succeededFuture(value));
        }
      }

      @Override
      public void onError(Throwable t) {
        if (!resolved) {
          resolved = true;
          handler.handle(io.vertx.core.Future.failedFuture(t));
        }
      }

      @Override
      public void onCompleted() {
        if (!resolved) {
          resolved = true;
          handler.handle(io.vertx.core.Future.succeededFuture());
        }
      }
    };
  }

  public static final String SERVICE_NAME = "ai.konduit.serving.Api";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme,
      ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme> getPredictMethod;

  public static io.grpc.MethodDescriptor<ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme,
      ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme> getPredictMethod() {
    io.grpc.MethodDescriptor<ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme, ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme> getPredictMethod;
    if ((getPredictMethod = ApiGrpc.getPredictMethod) == null) {
      synchronized (ApiGrpc.class) {
        if ((getPredictMethod = ApiGrpc.getPredictMethod) == null) {
          ApiGrpc.getPredictMethod = getPredictMethod = 
              io.grpc.MethodDescriptor.<ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme, ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "ai.konduit.serving.Api", "predict"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme.getDefaultInstance()))
                  .setSchemaDescriptor(new ApiMethodDescriptorSupplier("predict"))
                  .build();
          }
        }
     }
     return getPredictMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static ApiStub newStub(io.grpc.Channel channel) {
    return new ApiStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static ApiBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new ApiBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static ApiFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new ApiFutureStub(channel);
  }

  /**
   * Creates a new vertx stub that supports all call types for the service
   */
  public static ApiVertxStub newVertxStub(io.grpc.Channel channel) {
    return new ApiVertxStub(channel);
  }

  /**
   * <pre>
   * The main grpc service definition.
   * </pre>
   */
  public static abstract class ApiImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * predicts an output
     * </pre>
     */
    public void predict(ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme request,
        io.grpc.stub.StreamObserver<ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme> responseObserver) {
      asyncUnimplementedUnaryCall(getPredictMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getPredictMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme,
                ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme>(
                  this, METHODID_PREDICT)))
          .build();
    }
  }

  /**
   * <pre>
   * The main grpc service definition.
   * </pre>
   */
  public static final class ApiStub extends io.grpc.stub.AbstractStub<ApiStub> {
    public ApiStub(io.grpc.Channel channel) {
      super(channel);
    }

    public ApiStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ApiStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new ApiStub(channel, callOptions);
    }

    /**
     * <pre>
     * predicts an output
     * </pre>
     */
    public void predict(ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme request,
        io.grpc.stub.StreamObserver<ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getPredictMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   * The main grpc service definition.
   * </pre>
   */
  public static final class ApiBlockingStub extends io.grpc.stub.AbstractStub<ApiBlockingStub> {
    public ApiBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    public ApiBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ApiBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new ApiBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * predicts an output
     * </pre>
     */
    public ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme predict(ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme request) {
      return blockingUnaryCall(
          getChannel(), getPredictMethod(), getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * The main grpc service definition.
   * </pre>
   */
  public static final class ApiFutureStub extends io.grpc.stub.AbstractStub<ApiFutureStub> {
    public ApiFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    public ApiFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ApiFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new ApiFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * predicts an output
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme> predict(
        ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme request) {
      return futureUnaryCall(
          getChannel().newCall(getPredictMethod(), getCallOptions()), request);
    }
  }

  /**
   * <pre>
   * The main grpc service definition.
   * </pre>
   */
  public static abstract class ApiVertxImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * predicts an output
     * </pre>
     */
    public void predict(ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme request,
        io.vertx.core.Promise<ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme> response) {
      asyncUnimplementedUnaryCall(getPredictMethod(), ApiGrpc.toObserver(response));
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getPredictMethod(),
            asyncUnaryCall(
              new VertxMethodHandlers<
                ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme,
                ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme>(
                  this, METHODID_PREDICT)))
          .build();
    }
  }

  /**
   * <pre>
   * The main grpc service definition.
   * </pre>
   */
  public static final class ApiVertxStub extends io.grpc.stub.AbstractStub<ApiVertxStub> {
    public ApiVertxStub(io.grpc.Channel channel) {
      super(channel);
    }

    public ApiVertxStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ApiVertxStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new ApiVertxStub(channel, callOptions);
    }

    /**
     * <pre>
     * predicts an output
     * </pre>
     */
    public void predict(ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme request,
        io.vertx.core.Handler<io.vertx.core.AsyncResult<ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme>> response) {
      asyncUnaryCall(
          getChannel().newCall(getPredictMethod(), getCallOptions()), request, ApiGrpc.toObserver(response));
    }
  }

  private static final int METHODID_PREDICT = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final ApiImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(ApiImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_PREDICT:
          serviceImpl.predict((ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme) request,
              (io.grpc.stub.StreamObserver<ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static final class VertxMethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final ApiVertxImplBase serviceImpl;
    private final int methodId;

    VertxMethodHandlers(ApiVertxImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_PREDICT:
          serviceImpl.predict((ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme) request,
              (io.vertx.core.Promise<ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme>) io.vertx.core.Promise.<ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme>promise().future().setHandler(ar -> {
                if (ar.succeeded()) {
                  ((io.grpc.stub.StreamObserver<ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme>) responseObserver).onNext(ar.result());
                  responseObserver.onCompleted();
                } else {
                  responseObserver.onError(ar.cause());
                }
              }));
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class ApiBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    ApiBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return ai.konduit.serving.vertx.protocols.grpc.api.GrpcService.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("Api");
    }
  }

  private static final class ApiFileDescriptorSupplier
      extends ApiBaseDescriptorSupplier {
    ApiFileDescriptorSupplier() {}
  }

  private static final class ApiMethodDescriptorSupplier
      extends ApiBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    ApiMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (ApiGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new ApiFileDescriptorSupplier())
              .addMethod(getPredictMethod())
              .build();
        }
      }
    }
    return result;
  }
}
