package org.deeplearning4j.spark.canova;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.canova.api.records.reader.impl.SVMLightRecordReader;
import org.deeplearning4j.spark.BaseSparkTest;
import org.junit.Test;
import org.nd4j.linalg.dataset.DataSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import static org.junit.Assert.*;

/**
 * Handle dividing things up by mini batch
 */
public class MiniBatchTests extends BaseSparkTest {
    private static final Logger log = LoggerFactory.getLogger(MiniBatchTests.class);

    @Test
    public void testMiniBatches() throws Exception {
        log.info("Setting up Spark Context...");

        JavaRDD<String> lines = sc.textFile(new ClassPathResource("data/svmLight/iris_svmLight_0.txt").getFile().toURI().toString()).cache();
        long count = lines.count();
        assertEquals(300,count);
        // gotta map this to a Matrix/INDArray
        JavaRDD<DataSet> points = lines.map(new RecordReaderFunction(new SVMLightRecordReader(), 4, 3)).cache();
        count = points.count();
        assertEquals(300,count);

        JavaRDD<DataSet> miniBatches = new RDDMiniBatches(10,points).miniBatchesJava();
        count = miniBatches.count();
        assertEquals(30,count);

        miniBatches.map(new DataSetAssertionFunction());

    }


    public static class DataSetAssertionFunction implements Function<DataSet,Object> {

        @Override
        public Object call(DataSet dataSet) throws Exception {
            assertTrue(dataSet.getFeatureMatrix().columns() == 150);
            assertTrue(dataSet.numExamples() == 30);
            return null;
        }
    }


}
