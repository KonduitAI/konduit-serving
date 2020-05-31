package ai.konduit.serving.vertx.protocols.grpc.api;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 * <pre>
 * The main grpc service definition.
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.20.0)",
    comments = "Source: grpc-service.proto")
public final class InferenceGrpc {

  private InferenceGrpc() {}

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

  public static final String SERVICE_NAME = "ai.konduit.serving.Inference";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme,
      ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme> getPredictMethod;

  public static io.grpc.MethodDescriptor<ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme,
      ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme> getPredictMethod() {
    io.grpc.MethodDescriptor<ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme, ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme> getPredictMethod;
    if ((getPredictMethod = InferenceGrpc.getPredictMethod) == null) {
      synchronized (InferenceGrpc.class) {
        if ((getPredictMethod = InferenceGrpc.getPredictMethod) == null) {
          InferenceGrpc.getPredictMethod = getPredictMethod = 
              io.grpc.MethodDescriptor.<ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme, ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "ai.konduit.serving.Inference", "predict"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme.getDefaultInstance()))
                  .setSchemaDescriptor(new InferenceMethodDescriptorSupplier("predict"))
                  .build();
          }
        }
     }
     return getPredictMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static InferenceStub newStub(io.grpc.Channel channel) {
    return new InferenceStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static InferenceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new InferenceBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static InferenceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new InferenceFutureStub(channel);
  }

  /**
   * Creates a new vertx stub that supports all call types for the service
   */
  public static InferenceVertxStub newVertxStub(io.grpc.Channel channel) {
    return new InferenceVertxStub(channel);
  }

  /**
   * <pre>
   * The main grpc service definition.
   * </pre>
   */
  public static abstract class InferenceImplBase implements io.grpc.BindableService {

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
  public static final class InferenceStub extends io.grpc.stub.AbstractStub<InferenceStub> {
    public InferenceStub(io.grpc.Channel channel) {
      super(channel);
    }

    public InferenceStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected InferenceStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new InferenceStub(channel, callOptions);
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
  public static final class InferenceBlockingStub extends io.grpc.stub.AbstractStub<InferenceBlockingStub> {
    public InferenceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    public InferenceBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected InferenceBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new InferenceBlockingStub(channel, callOptions);
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
  public static final class InferenceFutureStub extends io.grpc.stub.AbstractStub<InferenceFutureStub> {
    public InferenceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    public InferenceFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected InferenceFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new InferenceFutureStub(channel, callOptions);
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
  public static abstract class InferenceVertxImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * predicts an output
     * </pre>
     */
    public void predict(ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme request,
        io.vertx.core.Promise<ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme> response) {
      asyncUnimplementedUnaryCall(getPredictMethod(), InferenceGrpc.toObserver(response));
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
  public static final class InferenceVertxStub extends io.grpc.stub.AbstractStub<InferenceVertxStub> {
    public InferenceVertxStub(io.grpc.Channel channel) {
      super(channel);
    }

    public InferenceVertxStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected InferenceVertxStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new InferenceVertxStub(channel, callOptions);
    }

    /**
     * <pre>
     * predicts an output
     * </pre>
     */
    public void predict(ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme request,
        io.vertx.core.Handler<io.vertx.core.AsyncResult<ai.konduit.serving.pipeline.impl.data.protobuf.DataProtoMessage.DataScheme>> response) {
      asyncUnaryCall(
          getChannel().newCall(getPredictMethod(), getCallOptions()), request, InferenceGrpc.toObserver(response));
    }
  }

  private static final int METHODID_PREDICT = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final InferenceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(InferenceImplBase serviceImpl, int methodId) {
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
    private final InferenceVertxImplBase serviceImpl;
    private final int methodId;

    VertxMethodHandlers(InferenceVertxImplBase serviceImpl, int methodId) {
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

  private static abstract class InferenceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    InferenceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return ai.konduit.serving.vertx.protocols.grpc.api.GrpcService.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("Inference");
    }
  }

  private static final class InferenceFileDescriptorSupplier
      extends InferenceBaseDescriptorSupplier {
    InferenceFileDescriptorSupplier() {}
  }

  private static final class InferenceMethodDescriptorSupplier
      extends InferenceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    InferenceMethodDescriptorSupplier(String methodName) {
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
      synchronized (InferenceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new InferenceFileDescriptorSupplier())
              .addMethod(getPredictMethod())
              .build();
        }
      }
    }
    return result;
  }
}
