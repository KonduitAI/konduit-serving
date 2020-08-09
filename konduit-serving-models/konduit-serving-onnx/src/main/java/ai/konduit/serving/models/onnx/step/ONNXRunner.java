package ai.konduit.serving.models.onnx.step;

import ai.konduit.serving.annotation.runner.CanRun;
import ai.konduit.serving.pipeline.api.context.Context;
import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.NDArray;
import ai.konduit.serving.pipeline.api.step.PipelineStep;
import ai.konduit.serving.pipeline.api.step.PipelineStepRunner;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.indexer.*;
import org.bytedeco.onnxruntime.*;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.bytedeco.onnxruntime.global.onnxruntime.*;

@Slf4j
@CanRun({ONNXStep.class})
public class ONNXRunner implements PipelineStepRunner {

    private  ONNXStep onnxStep;
    private Session session;

    public ONNXRunner(ONNXStep onnxStep) {
        this.onnxStep = onnxStep;
        Env env = new Env(ORT_LOGGING_LEVEL_WARNING, new BytePointer("konduit-serving-onnx-session" + System.currentTimeMillis()));

        try (SessionOptions sessionOptions = new SessionOptions()) {
            try (Pointer bp = Loader.getPlatform().toLowerCase().startsWith("windows") ? new CharPointer(onnxStep.modelUri()) : new BytePointer(onnxStep.modelUri())) {
                session = new Session(env, bp, sessionOptions);
            }
        }

    }


    @Override
    public void close() {
        if(session != null) {
            session.close();
        }
    }

    @Override
    public PipelineStep getPipelineStep() {
        return onnxStep;
    }

    @Override
    public Data exec(Context ctx, Data data) {
        try (PointerScope scope = new PointerScope()) {
            // model should be replicated & initialized here
            List<Map<String,INDArray>> inputs = new ArrayList<>();
            Map<String,INDArray> input = new LinkedHashMap<>();

            long numInputNodes = session.GetInputCount();
            long numOutputNodes = session.GetOutputCount();
            try (AllocatorWithDefaultOptions allocator = new AllocatorWithDefaultOptions();
                 PointerPointer<BytePointer> inputNodeNames = new PointerPointer<>(numInputNodes);
                 PointerPointer<BytePointer> outputNodeNames = new PointerPointer<>(numOutputNodes)) {

                LongPointer[] inputNodeDims = new LongPointer[(int) numInputNodes];

                for (int i = 0; i < numOutputNodes; i++) {
                    BytePointer outputName = session.GetOutputName(i, allocator.asOrtAllocator());
                    outputNodeNames.put(i, outputName);
                }

                long[] inputSizes = new long[(int) numInputNodes];
                int[] inputTypes = new int[(int) numInputNodes];


                for (int i = 0; i < numInputNodes; i++) {
                    BytePointer inputName = session.GetInputName(i, allocator.asOrtAllocator());

                    inputNodeNames.put(i, inputName);

                    TypeInfo typeInfo = session.GetInputTypeInfo(i);
                    inputTypes[i] = typeInfo.GetONNXType();

                    TensorTypeAndShapeInfo tensorInfo = typeInfo.GetTensorTypeAndShapeInfo();
                    inputNodeDims[i] = tensorInfo.GetShape();

                    int acc = 1;
                    for (long j = 0; j < inputNodeDims[i].capacity(); j++)
                        acc *= inputNodeDims[i].get(j);

                    inputSizes[i] = acc;
                    input.put(inputName.getString(),data.getNDArray(inputName.getString()).getAs(INDArray.class));
                }

                Data ret = Data.empty();
                Map<String, INDArray> output = doInference(
                        input,
                        inputNodeNames,
                        outputNodeNames,
                        inputTypes,
                        inputSizes,
                        inputNodeDims);

                for(String outputName : onnxStep.outputNames()) {
                    ret.put(outputName, NDArray.create(output.get(outputName)));
                }

                return ret;

            }
        }

    }


    private Map<String, INDArray> doInference(Map<String, INDArray> input, PointerPointer<BytePointer> inputNodeNames,
                                              PointerPointer<BytePointer> outputNodeNames, int[] inputTypes, long[] inputSizes, LongPointer[] inputNodeDims) {

        long numInputNodes = session.GetInputCount();
        long numOutputNodes = session.GetOutputCount();


        try {
            Value[] inputTensors = new Value[(int) numInputNodes];

            for (int i = 0; i < numInputNodes; i++) {
                BytePointer inputName = inputNodeNames.get(BytePointer.class, i);
                Value inputTensor = getTensor(input.get(inputName.getString()), inputTypes[i], inputSizes[i], inputNodeDims[i]);
                inputTensors[i] = inputTensor;
            }

            Value inputVal = new Value(numInputNodes);

            for (int i = 0; i < numInputNodes; i++) {
                inputVal.position(i).put(inputTensors[i]);
            }

            ValueVector outputVector = session.Run(new RunOptions(), inputNodeNames, inputVal.position(0), numInputNodes, outputNodeNames, numOutputNodes);

            Map<String, INDArray> output = new LinkedHashMap<>();

            for (int i = 0; i < numOutputNodes; i++) {
                Value outValue = outputVector.get(i);

                DataBuffer buffer = getDataBuffer(outValue);
                output.put((outputNodeNames.get(BytePointer.class, i)).getString(), Nd4j.create(buffer));
            }

            return  output;

        } catch (Exception e) {
            log.error("Error occurred doing inference", e);
        }

        return null;
    }


    protected void validateType(DataType expected, INDArray array) {
        if (!array.dataType().equals(expected))
            throw new RuntimeException("INDArray data type (" + array.dataType() + ") does not match required ONNX data type (" + expected + ")");
    }

    private Value getTensor(INDArray ndArray, int type, long size, LongPointer dims) {
        Pointer inputTensorValuesPtr = ndArray.data().pointer();

        long sizeInBytes;
        MemoryInfo memoryInfo = MemoryInfo.CreateCpu(OrtArenaAllocator, OrtMemTypeDefault);
        Pointer inputTensorValues;
        switch (type) {
            case ONNX_TENSOR_ELEMENT_DATA_TYPE_FLOAT:
                validateType(DataType.FLOAT, ndArray);
                inputTensorValues = inputTensorValuesPtr;
                sizeInBytes = size * 4;
                break;
            case ONNX_TENSOR_ELEMENT_DATA_TYPE_UINT8:
                validateType(DataType.UINT8, ndArray);
                inputTensorValues = inputTensorValuesPtr;
                sizeInBytes = size;
                break;
            case ONNX_TENSOR_ELEMENT_DATA_TYPE_INT8:
                validateType(DataType.INT8, ndArray);
                inputTensorValues = inputTensorValuesPtr;
                sizeInBytes = size;
                break;
            case ONNX_TENSOR_ELEMENT_DATA_TYPE_UINT16:
                validateType(DataType.UINT16, ndArray);
                inputTensorValues = inputTensorValuesPtr;
                sizeInBytes = size * 2;
                break;
            case ONNX_TENSOR_ELEMENT_DATA_TYPE_INT16:
                validateType(DataType.INT16, ndArray);
                inputTensorValues = inputTensorValuesPtr;
                sizeInBytes = size * 2;
                break;
            case ONNX_TENSOR_ELEMENT_DATA_TYPE_INT32:
                validateType(DataType.INT32, ndArray);
                inputTensorValues = inputTensorValuesPtr;
                sizeInBytes = size * 4;
                break;
            case ONNX_TENSOR_ELEMENT_DATA_TYPE_INT64:
                validateType(DataType.INT64, ndArray);
                inputTensorValues = inputTensorValuesPtr;
                sizeInBytes = size * 8;
                break;
            case ONNX_TENSOR_ELEMENT_DATA_TYPE_BOOL:
                validateType(DataType.BOOL, ndArray);
                inputTensorValues = inputTensorValuesPtr; //Casting Boolean to Bool here, sizes could different on some platforms
                sizeInBytes = size;
                break;
            case ONNX_TENSOR_ELEMENT_DATA_TYPE_FLOAT16:
                validateType(DataType.FLOAT16, ndArray);
                inputTensorValues = inputTensorValuesPtr;
                sizeInBytes = size * 2;
                break;
            case ONNX_TENSOR_ELEMENT_DATA_TYPE_DOUBLE:
                validateType(DataType.DOUBLE, ndArray);
                inputTensorValues = inputTensorValuesPtr;
                sizeInBytes = size * 8;
                break;
            case ONNX_TENSOR_ELEMENT_DATA_TYPE_UINT32:
                validateType(DataType.UINT32, ndArray);
                inputTensorValues = inputTensorValuesPtr;
                sizeInBytes = size * 4;
                break;
            case ONNX_TENSOR_ELEMENT_DATA_TYPE_UINT64:
                validateType(DataType.UINT64, ndArray);
                inputTensorValues = inputTensorValuesPtr;
                sizeInBytes = size * 8;
                break;
            case ONNX_TENSOR_ELEMENT_DATA_TYPE_BFLOAT16:
                validateType(DataType.BFLOAT16, ndArray);
                inputTensorValues = inputTensorValuesPtr;
                sizeInBytes = size * 2;
                break;
            default:
                throw new RuntimeException("Unsupported data type encountered");
        }
        return Value.CreateTensor(memoryInfo.asOrtMemoryInfo(), inputTensorValues, sizeInBytes, dims, dims.capacity(), type);
    }

    private DataBuffer getDataBuffer(Value tens) {
        try (PointerScope scope = new PointerScope()) {
            DataBuffer buffer = null;
            int type = tens.GetTensorTypeAndShapeInfo().GetElementType();
            long size = tens.GetTensorTypeAndShapeInfo().GetElementCount();
            switch (type) {
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_FLOAT:
                    FloatPointer pFloat = tens.GetTensorMutableDataFloat().capacity(size);
                    FloatIndexer floatIndexer = FloatIndexer.create(pFloat);
                    buffer = Nd4j.createBuffer(pFloat, DataType.FLOAT, size, floatIndexer);
                    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_UINT8:
                    BytePointer pUint8 = tens.GetTensorMutableDataUByte().capacity(size);
                    Indexer uint8Indexer = ByteIndexer.create(pUint8);
                    buffer = Nd4j.createBuffer(pUint8, DataType.UINT8, size, uint8Indexer);
                    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_INT8:
                    BytePointer pInt8 = tens.GetTensorMutableDataByte().capacity(size);
                    Indexer int8Indexer = ByteIndexer.create(pInt8);
                    buffer = Nd4j.createBuffer(pInt8, DataType.UINT8, size, int8Indexer);
                    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_UINT16:
                    ShortPointer pUint16 = tens.GetTensorMutableDataUShort().capacity(size);
                    Indexer uint16Indexer = ShortIndexer.create(pUint16);
                    buffer = Nd4j.createBuffer(pUint16, DataType.UINT16, size, uint16Indexer);
                    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_INT16:
                    ShortPointer pInt16 = tens.GetTensorMutableDataShort().capacity(size);
                    Indexer int16Indexer = ShortIndexer.create(pInt16);
                    buffer = Nd4j.createBuffer(pInt16, DataType.INT16, size, int16Indexer);
                    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_INT32:
                    IntPointer pInt32 = tens.GetTensorMutableDataInt().capacity(size);
                    Indexer int32Indexer = IntIndexer.create(pInt32);
                    buffer = Nd4j.createBuffer(pInt32, DataType.INT32, size, int32Indexer);
                    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_INT64:
                    LongPointer pInt64 = tens.GetTensorMutableDataLong().capacity(size);
                    Indexer int64Indexer = LongIndexer.create(pInt64);
                    buffer = Nd4j.createBuffer(pInt64, DataType.INT64, size, int64Indexer);
                    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_STRING:
                    BytePointer pString = tens.GetTensorMutableDataByte().capacity(size);
                    Indexer stringIndexer = ByteIndexer.create(pString);
                    buffer = Nd4j.createBuffer(pString, DataType.INT8, size, stringIndexer);
                    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_BOOL:
                    BoolPointer pBool = tens.GetTensorMutableDataBool().capacity(size);
                    Indexer boolIndexer = BooleanIndexer.create(new BooleanPointer(pBool)); //Converting from JavaCPP Bool to Boolean here - C++ bool type size is not defined, could cause problems on some platforms
                    buffer = Nd4j.createBuffer(pBool, DataType.BOOL, size, boolIndexer);
                    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_FLOAT16:
                    ShortPointer pFloat16 = tens.GetTensorMutableDataShort().capacity(size);
                    Indexer float16Indexer = ShortIndexer.create(pFloat16);
                    buffer = Nd4j.createBuffer(pFloat16, DataType.FLOAT16, size, float16Indexer);
                    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_DOUBLE:
                    DoublePointer pDouble = tens.GetTensorMutableDataDouble().capacity(size);
                    Indexer doubleIndexer = DoubleIndexer.create(pDouble);
                    buffer = Nd4j.createBuffer(pDouble, DataType.DOUBLE, size, doubleIndexer);
                    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_UINT32:
                    IntPointer pUint32 = tens.GetTensorMutableDataUInt().capacity(size);
                    Indexer uint32Indexer = IntIndexer.create(pUint32);
                    buffer = Nd4j.createBuffer(pUint32, DataType.UINT32, size, uint32Indexer);
                    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_UINT64:
                    LongPointer pUint64 = tens.GetTensorMutableDataULong().capacity(size);
                    Indexer uint64Indexer = LongIndexer.create(pUint64);
                    buffer = Nd4j.createBuffer(pUint64, DataType.UINT64, size, uint64Indexer);
                    break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_BFLOAT16:
                    ShortPointer pBfloat16 = tens.GetTensorMutableDataShort().capacity(size);
                    Indexer bfloat16Indexer = ShortIndexer.create(pBfloat16);
                    buffer = Nd4j.createBuffer(pBfloat16, DataType.BFLOAT16, size, bfloat16Indexer);
                    break;
                default:
                    throw new RuntimeException("Unsupported data type encountered");
            }
            return buffer;
        }
    }
}
