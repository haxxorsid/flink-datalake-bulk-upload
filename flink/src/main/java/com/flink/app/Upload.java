package com.flink.app;

import com.azure.storage.file.datalake.DataLakeDirectoryClient;
import com.flink.operator.CountingAggregator;
import com.flink.operator.ImageCountProcessFunction;
import com.flink.proxies.Datalake;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.JdbcStatementBuilder;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;

import org.apache.flink.connector.jdbc.JdbcSink;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Upload {

    // Kafka topic name (Azure event hub name)
    private static final String CONSUMER_TOPIC = "<fill-in>";

    // path to properties files in src/main/resources folder.
    private static final String KAFKA_PROPERTIES_PATH = "/kafka.properties";
    private static final String DATABASE_PROPERTIES_PATH = "/database.properties";

    // Gson image to deserialize from Kafka message into Image object.
    private static final Gson gson =
            new GsonBuilder().serializeSpecialFloatingPointValues().create();

    // Account key for data lake
    private static final String ACCOUNT_KEY = "<fill-in>";
    // Data lake name
    private static final String ACCOUNT_NAME = "<fill-in>";
    // Container name inside the data lake
    private static final String CONTAINER_NAME = "<fill-in>";
    private static Calendar calendar = Calendar.getInstance();

    // Reference to datalake object
    private static Datalake dl = new Datalake(ACCOUNT_KEY, ACCOUNT_NAME, CONTAINER_NAME);
    public static void main(String... args) {
        try {

            // Get Kafka endpoint properties (endpoint, credentials)
            Properties properties = getProperties(KAFKA_PROPERTIES_PATH);

            // Generate stream from Kafka topic
            StreamExecutionEnvironment env =  StreamExecutionEnvironment.getExecutionEnvironment();
            DataStream<String> stream = env.addSource(new FlinkKafkaConsumer<>(CONSUMER_TOPIC, new SimpleStringSchema(), properties));

            // Deserialize kafka messages into Image objects
            DataStream<Image> messageStream = stream
                    .map(json -> gson.fromJson(json, Image.class));
            messageStream.print();

            // Calculate count of each images with 30 seconds window time.
            // Save count of each image, name of image, and time in ImageWithCount object
            DataStream<ImageWithCount> countStream = messageStream
                    .keyBy(value -> value.getBuffer())
                    .window(TumblingProcessingTimeWindows.of(Time.seconds(30)))
                    .aggregate(new CountingAggregator(), new ImageCountProcessFunction())
                    .returns(ImageWithCount.class);
            countStream.print();

            // Upload Images in DataLake
            DataStream<ImageWithCount> updatedCountStream = countStream.map(new MapFunction<ImageWithCount, ImageWithCount>() {
                @Override
                public ImageWithCount map(ImageWithCount x) throws Exception {

                // Get Year, Month, Day, Hour, Minute, Second for Directory structure in Data Lake.
                calendar.setTime(new Date(x.timeStamp));
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH)+1;
                int day = calendar.get(Calendar.DATE);
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                String minuteSecond = calendar.get(Calendar.MINUTE) + ":" + calendar.get(Calendar.SECOND);;

                // Root folder is Year
                DataLakeDirectoryClient directoryClient = dl.CreateDirectory(String.valueOf(year));
                // Inside Year, we have month folder
                directoryClient = dl.CreateSubDirectory(directoryClient, String.valueOf(month));
                // Inside Month, we have day folder
                directoryClient = dl.CreateSubDirectory(directoryClient, String.valueOf(day));
                // Inside day, we have hour folder
                directoryClient = dl.CreateSubDirectory(directoryClient, String.valueOf(hour));
                // Inside hour, we have month+second folder.
                directoryClient = dl.CreateSubDirectory(directoryClient,minuteSecond);

                // Upload image and set URL 
                x.setUrl(dl.UploadFile(directoryClient, x.getImage()));
                return x;
            }});

            updatedCountStream.print();

            // Get database properties like connection string and use jdbc driver
            properties = getProperties(DATABASE_PROPERTIES_PATH);

            JdbcConnectionOptions jdbcConnectionOptions = new
                    JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
                    .withUrl(properties.getProperty("url"))
                    .withDriverName(properties.getProperty("driver"))
                    .withUsername(properties.getProperty("user"))
                    .withPassword(properties.getProperty("password"))
                    .build();

            JdbcExecutionOptions optionsBuilder = JdbcExecutionOptions.builder()
                    .withBatchSize(1)
                    .withBatchIntervalMs(0)
                    .withMaxRetries(5)
                    .build();

            // Substitute values in placeholders in Insert query.
            JdbcStatementBuilder jdbcStatementBuilder = new JdbcStatementBuilder<ImageWithCount>() {
                @Override
                public void accept(PreparedStatement preparedStatement, ImageWithCount iwc) throws SQLException {
                    preparedStatement.setString(1, iwc.getImage().getName());
                    preparedStatement.setLong(2, iwc.getCount());
                    preparedStatement.setString(3, iwc.getUrl());
                    preparedStatement.setTimestamp(4, new Timestamp(iwc.getTimeStamp()));
                }};

            // Sink all streamed images into the SQL database using SQL Query.
            updatedCountStream.addSink(JdbcSink.sink("INSERT INTO UploadSummary (FileName, Count, Url, CreatedAt) VALUES (?, ?, ?, ?)",
                    jdbcStatementBuilder,
                    optionsBuilder,
                    jdbcConnectionOptions));
            env.execute("Flink bulk image upload");

        } catch(FileNotFoundException e){
            System.out.println("FileNoteFoundException: " + e);
        } catch (Exception e){
            System.out.println("Failed with exception " + e);
        }
    }

    // Read kafka, database properties from properties file as part of JAR or not JAR.
    static Properties getProperties(String filePath) throws IOException {
        InputStream propertiesInput = Upload.class.getResourceAsStream(filePath);
        Properties properties = new Properties();
        properties.load(propertiesInput);
        return properties;
    }
}
