/*
 *
 *  * ******************************************************************************
 *  *  * Copyright (c) 2015-2019 Skymind Inc.
 *  *  * Copyright (c) 2019 Konduit AI.
 *  *  *
 *  *  * This program and the accompanying materials are made available under the
 *  *  * terms of the Apache License, Version 2.0 which is available at
 *  *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  *  * License for the specific language governing permissions and limitations
 *  *  * under the License.
 *  *  *
 *  *  * SPDX-License-Identifier: Apache-2.0
 *  *  *****************************************************************************
 *
 *
 */

package ai.konduit.serving.train;

import ai.konduit.serving.normalizer.CustomImagePreProcessingScaler;

@lombok.extern.slf4j.Slf4j
public class TrainUtils {

    private static final String basePath = System.getProperty("java.io.tmpdir") + "/mnist";
    private static final String dataUrl = "http://github.com/myleott/mnist_png/raw/master/mnist_png.tar.gz";



    public static org.datavec.api.transform.schema.Schema getAudOutputSchema() {
        org.datavec.api.transform.schema.Schema.Builder schemaBuilder = new org.datavec.api.transform.schema.Schema.Builder();
        schemaBuilder.addColumnDouble("xgbValue");
        return schemaBuilder.build();
    }

    public static org.datavec.api.transform.schema.Schema getAuditInputSchema() {
              /*
        * Age,Employment,Education,Marital,Occupation,Income,Gender,Deductions,Hours,Adjusted
38,Private,College,Unmarried,Service,81838,Female,FALSE,72,0
        * */
        org.datavec.api.transform.schema.Schema.Builder inputSchemaBuilder = new org.datavec.api.transform.schema.Schema.Builder();
        inputSchemaBuilder.addColumnInteger("Age");
        inputSchemaBuilder.addColumnString("Employment");
        inputSchemaBuilder.addColumnString("Education");
        inputSchemaBuilder.addColumnString("Martial");
        inputSchemaBuilder.addColumnString("Occupation");
        inputSchemaBuilder.addColumnInteger("Income");
        inputSchemaBuilder.addColumnString("Gender");
        inputSchemaBuilder.addColumn(new org.datavec.api.transform.metadata.BooleanMetaData("Deductions"));
        inputSchemaBuilder.addColumnInteger("Hours");
        inputSchemaBuilder.addColumnDouble("Adjusted");

        org.datavec.api.transform.schema.Schema schema = inputSchemaBuilder.build();
        return schema;

    }


    public static org.datavec.api.transform.schema.Schema getPmmlIrisOutputSchema() {
        org.datavec.api.transform.schema.Schema.Builder outputSchemaBuilder = new org.datavec.api.transform.schema.Schema.Builder();
        outputSchemaBuilder.addColumnDouble("setosa");
        outputSchemaBuilder.addColumnDouble("versicolor");
        outputSchemaBuilder.addColumnDouble("virginica");
        org.datavec.api.transform.schema.Schema outputSchema = outputSchemaBuilder.build();
        return outputSchema;
    }

    public static org.datavec.api.transform.schema.Schema getPmmlIrisInputSchema() {
        String[] columnNames = {"Petal.Length","Petal.Width"};
        org.datavec.api.transform.schema.Schema.Builder schemaBuilder = new org.datavec.api.transform.schema.Schema.Builder();
        for(int i = 0; i < columnNames.length; i++) {
            schemaBuilder.addColumnDouble(columnNames[i]);
        }

        org.datavec.api.transform.schema.Schema schema = schemaBuilder.build();
        return schema;
    }

    public static org.datavec.api.transform.TransformProcess getTransformProcessForIrisInput() {
        org.datavec.api.transform.schema.Schema schema = getIrisInputSchema();
        org.datavec.api.transform.TransformProcess.Builder transformProcessBuilder = new org.datavec.api.transform.TransformProcess.Builder(schema);
        for(int i = 0; i < schema.numColumns(); i++) {
            transformProcessBuilder.convertToDouble(schema.getName(i));
        }

        org.datavec.api.transform.TransformProcess transformProcess = transformProcessBuilder.build();
        return transformProcess;
    }

    public static org.datavec.api.transform.schema.Schema getIrisOutputSchema() {
        org.datavec.api.transform.schema.Schema.Builder outputSchemaBuilder = new org.datavec.api.transform.schema.Schema.Builder();
        outputSchemaBuilder.addColumnDouble("setosa");
        outputSchemaBuilder.addColumnDouble("versicolor");
        outputSchemaBuilder.addColumnDouble("virginica");

        org.datavec.api.transform.schema.Schema outputSchema = outputSchemaBuilder.build();
        return  outputSchema;
    }

    public static org.datavec.api.transform.schema.Schema getIrisInputSchema() {
        org.datavec.api.transform.schema.Schema.Builder schemaBuilder = new org.datavec.api.transform.schema.Schema.Builder();
        schemaBuilder.addColumnString("petal_length")
                .addColumnString("petal_width")
                .addColumnString("sepal_width")
                .addColumnString("sepal_height");

        org.datavec.api.transform.schema.Schema schema = schemaBuilder.build();
        return  schema;
    }

    public static synchronized  org.nd4j.linalg.primitives.Pair<org.deeplearning4j.nn.multilayer.MultiLayerNetwork, org.nd4j.linalg.dataset.api.preprocessor.DataNormalization> getTrainedNetwork() throws Exception {
        //First: get the dataset using the record reader. CSVRecordReader handles loading/parsing
        int numLinesToSkip = 0;
        char delimiter = ',';
        org.datavec.api.records.reader.RecordReader recordReader = new org.datavec.api.records.reader.impl.csv.CSVRecordReader(numLinesToSkip,delimiter);
        recordReader.initialize(new org.datavec.api.split.FileSplit(new org.nd4j.linalg.io.ClassPathResource("iris.txt").getFile()));

        //Second: the RecordReaderDataSetIterator handles conversion to DataSet objects, ready for use in neural network
        int labelIndex = 4;     //5 values in each row of the iris.txt CSV: 4 input features followed by an integer label (class) index. Labels are the 5th value (index 4) in each row
        int numClasses = 3;     //3 classes (types of iris flowers) in the iris data set. Classes have integer values 0, 1 or 2
        int batchSize = 150;    //Iris data set: 150 examples total. We are loading all of them into one DataSet (not recommended for large data sets)

        org.nd4j.linalg.dataset.api.iterator.DataSetIterator iterator = new org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator(recordReader,batchSize,labelIndex,numClasses);
        org.nd4j.linalg.dataset.DataSet allData = iterator.next();
        allData.shuffle();
        org.nd4j.linalg.dataset.SplitTestAndTrain testAndTrain = allData.splitTestAndTrain(0.65);  //Use 65% of data for training

        org.nd4j.linalg.dataset.DataSet trainingData = testAndTrain.getTrain();
        org.nd4j.linalg.dataset.DataSet testData = testAndTrain.getTest();

        //We need to normalize our data. We'll use NormalizeStandardize (which gives us mean 0, unit variance):
        org.nd4j.linalg.dataset.api.preprocessor.DataNormalization normalizer = new org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize();
        normalizer.fit(trainingData);           //Collect the statistics (mean/stdev) from the training data. This does not modify the input data
        normalizer.transform(trainingData);     //Apply normalization to the training data
        normalizer.transform(testData);         //Apply normalization to the test data. This is using statistics calculated from the *training* set


        final int numInputs = 4;
        int outputNum = 3;
        long seed = 6;


        org.deeplearning4j.nn.conf.MultiLayerConfiguration conf = new org.deeplearning4j.nn.conf.NeuralNetConfiguration.Builder()
                .seed(seed)
                .activation(org.nd4j.linalg.activations.Activation.TANH)
                .weightInit(org.deeplearning4j.nn.weights.WeightInit.XAVIER)
                .updater(new org.nd4j.linalg.learning.config.Sgd(0.1))
                .l2(1e-4)
                .list()
                .layer(0, new org.deeplearning4j.nn.conf.layers.DenseLayer.Builder().nIn(numInputs).nOut(3)
                        .build())
                .layer(1, new org.deeplearning4j.nn.conf.layers.DenseLayer.Builder().nIn(3).nOut(3)
                        .build())
                .layer(2, new org.deeplearning4j.nn.conf.layers.OutputLayer.Builder(org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                        .activation(org.nd4j.linalg.activations.Activation.SOFTMAX)
                        .nIn(3).nOut(outputNum).build())
                .build();

        //run the model
        org.deeplearning4j.nn.multilayer.MultiLayerNetwork model = new org.deeplearning4j.nn.multilayer.MultiLayerNetwork(conf);
        model.init();
        model.setListeners(new org.deeplearning4j.optimize.listeners.ScoreIterationListener(100));

        for(int i = 0; i < 1000; i++ ) {
            model.fit(trainingData);
        }

        return org.nd4j.linalg.primitives.Pair.of(model,normalizer);

    }

    public static TestConfig getPretrainedMnist() throws Exception {
        int height = 28;
        int width = 28;
        int channels = 1; // single channel for grayscale images
        int outputNum = 10; // 10 digits classification
        int batchSize = 54;
        int nEpochs = 1;

        int seed = 1234;
        java.util.Random randNumGen = new java.util.Random(seed);

        log.debug("Data load and vectorization...");
        String localFilePath = basePath + "/mnist_png.tar.gz";
        if (DataUtilities.downloadFile(dataUrl, localFilePath))
            log.debug("Data downloaded from {}", dataUrl);
        if (!new java.io.File(basePath + "/mnist_png").exists())
           DataUtilities.extractTarGz(localFilePath, basePath);

        // vectorization of train data
        java.io.File trainData = new java.io.File(basePath + "/mnist_png/training");
        org.datavec.api.split.FileSplit trainSplit = new org.datavec.api.split.FileSplit(trainData, org.datavec.image.loader.NativeImageLoader.ALLOWED_FORMATS, randNumGen);
        org.datavec.api.io.labels.ParentPathLabelGenerator labelMaker = new org.datavec.api.io.labels.ParentPathLabelGenerator(); // parent path as the image label
        org.datavec.image.recordreader.ImageRecordReader trainRR = new org.datavec.image.recordreader.ImageRecordReader(height, width, channels, labelMaker);
        trainRR.initialize(trainSplit);
        org.nd4j.linalg.dataset.api.iterator.DataSetIterator trainIter = new org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator(trainRR, batchSize, 1, outputNum);

        // pixel values from 0-255 to 0-1 (min-max scaling)
        org.nd4j.linalg.dataset.api.preprocessor.DataNormalization scaler = new CustomImagePreProcessingScaler(0, 1);
        scaler.fit(trainIter);
        trainIter.setPreProcessor(scaler);

        // vectorization of test data
        java.io.File testData = new java.io.File(basePath + "/mnist_png/testing");
        org.datavec.api.split.FileSplit testSplit = new org.datavec.api.split.FileSplit(testData, org.datavec.image.loader.NativeImageLoader.ALLOWED_FORMATS, randNumGen);
        org.datavec.image.recordreader.ImageRecordReader testRR = new org.datavec.image.recordreader.ImageRecordReader(height, width, channels, labelMaker);
        testRR.initialize(testSplit);
        org.nd4j.linalg.dataset.api.iterator.DataSetIterator testIter = new org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator(testRR, batchSize, 1, outputNum);
        testIter.setPreProcessor(scaler); // same normalization for better results

        log.debug("Network configuration and training...");
        java.util.Map<Integer, Double> lrSchedule = new java.util.HashMap<>();
        lrSchedule.put(0, 0.06); // iteration #, learning rate
        lrSchedule.put(200, 0.05);
        lrSchedule.put(600, 0.028);
        lrSchedule.put(800, 0.0060);
        lrSchedule.put(1000, 0.001);

        org.deeplearning4j.nn.conf.MultiLayerConfiguration conf = new org.deeplearning4j.nn.conf.NeuralNetConfiguration.Builder()
                .seed(seed)
                .l2(0.0005)
                .updater(new org.nd4j.linalg.learning.config.Nesterovs(new org.nd4j.linalg.schedule.MapSchedule(org.nd4j.linalg.schedule.ScheduleType.ITERATION, lrSchedule)))
                .weightInit(org.deeplearning4j.nn.weights.WeightInit.XAVIER)
                .list()
                .layer(0, new org.deeplearning4j.nn.conf.layers.ConvolutionLayer.Builder(5, 5)
                        .nIn(channels)
                        .stride(1, 1)
                        .nOut(20)
                        .activation(org.nd4j.linalg.activations.Activation.IDENTITY)
                        .build())
                .layer(1, new org.deeplearning4j.nn.conf.layers.SubsamplingLayer.Builder(org.deeplearning4j.nn.conf.layers.SubsamplingLayer.PoolingType.MAX)
                        .kernelSize(2, 2)
                        .stride(2, 2)
                        .build())
                .layer(2, new org.deeplearning4j.nn.conf.layers.ConvolutionLayer.Builder(5, 5)
                        .stride(1, 1) // nIn need not specified in later layers
                        .nOut(50)
                        .activation(org.nd4j.linalg.activations.Activation.IDENTITY)
                        .build())
                .layer(3, new org.deeplearning4j.nn.conf.layers.SubsamplingLayer.Builder(org.deeplearning4j.nn.conf.layers.SubsamplingLayer.PoolingType.MAX)
                        .kernelSize(2, 2)
                        .stride(2, 2)
                        .build())
                .layer(4, new org.deeplearning4j.nn.conf.layers.DenseLayer.Builder().activation(org.nd4j.linalg.activations.Activation.RELU)
                        .nOut(500).build())
                .layer(5, new org.deeplearning4j.nn.conf.layers.OutputLayer.Builder(org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                        .nOut(outputNum)
                        .activation(org.nd4j.linalg.activations.Activation.SOFTMAX)
                        .build())
                .setInputType(org.deeplearning4j.nn.conf.inputs.InputType.convolutionalFlat(28, 28, 1)) // InputDataType.convolutional for normal image
               .build();

        org.deeplearning4j.nn.multilayer.MultiLayerNetwork net = new org.deeplearning4j.nn.multilayer.MultiLayerNetwork(conf);
        net.init();
        net.setListeners(new org.deeplearning4j.optimize.listeners.ScoreIterationListener(10));
        log.debug("Total num of params: {}", net.numParams());

        // evaluation while training (the score should go down)
        for (int i = 0; i < nEpochs; i++) {
            net.fit(trainIter);
            log.debug("Completed epoch {}", i);
            org.nd4j.evaluation.classification.Evaluation eval = net.evaluate(testIter);
            log.debug(eval.stats());
            trainIter.reset();
            testIter.reset();
        }

        return TestConfig.builder()
                .computationGraph(net.toComputationGraph())
                .dataNormalization(scaler).
                        build();

    }

}
