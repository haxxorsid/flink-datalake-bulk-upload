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
import java.io.FileReader;
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

    private static final String CONSUMER_TOPIC = "<fill-in>";
    private static final String KAFKA_PROPERTIES_PATH = "/kafka.properties";
    private static final String DATABASE_PROPERTIES_PATH = "/database.properties";
    private static final Gson gson =
            new GsonBuilder().serializeSpecialFloatingPointValues().create();

    private static final String ACCOUNT_KEY = "<fill-in>";
    private static final String ACCOUNT_NAME = "<fill-in>";
    private static final String CONTAINER_NAME = "<fill-in>";
    private static Calendar calendar = Calendar.getInstance();

    private static Datalake dl = new Datalake(ACCOUNT_KEY, ACCOUNT_NAME, CONTAINER_NAME);
    public static void main(String... args) {
        try {
            Properties properties = getProperties(KAFKA_PROPERTIES_PATH);

            StreamExecutionEnvironment env =  StreamExecutionEnvironment.getExecutionEnvironment();
            DataStream<String> stream = env.addSource(new FlinkKafkaConsumer<>(CONSUMER_TOPIC, new SimpleStringSchema(), properties));

            DataStream<Image> messageStream = stream
                    .map(json -> gson.fromJson(json, Image.class));
            messageStream.print();

            DataStream<ImageWithCount> countStream = messageStream
                    .keyBy(value -> value.getBuffer())
                    .window(TumblingProcessingTimeWindows.of(Time.seconds(30)))
                    .aggregate(new CountingAggregator(), new ImageCountProcessFunction())
                    .returns(ImageWithCount.class);
            countStream.print();

            DataStream<ImageWithCount> updatedCountStream = countStream.map(new MapFunction<ImageWithCount, ImageWithCount>() {
                @Override
                public ImageWithCount map(ImageWithCount x) throws Exception {
                calendar.setTime(new Date(x.timeStamp));
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH)+1;
                int day = calendar.get(Calendar.DATE);
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                String minuteSecond = calendar.get(Calendar.MINUTE) + ":" + calendar.get(Calendar.SECOND);;

                DataLakeDirectoryClient directoryClient = dl.CreateDirectory(String.valueOf(year));
                directoryClient = dl.CreateSubDirectory(directoryClient, String.valueOf(month));
                directoryClient = dl.CreateSubDirectory(directoryClient, String.valueOf(day));
                directoryClient = dl.CreateSubDirectory(directoryClient, String.valueOf(hour));
                directoryClient = dl.CreateSubDirectory(directoryClient,minuteSecond);
                x.setUrl(dl.UploadFile(directoryClient, x.getImage()));
                return x;
            }});

            updatedCountStream.print();

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

            JdbcStatementBuilder jdbcStatementBuilder = new JdbcStatementBuilder<ImageWithCount>() {
                @Override
                public void accept(PreparedStatement preparedStatement, ImageWithCount iwc) throws SQLException {
                    preparedStatement.setString(1, iwc.getImage().getName());
                    preparedStatement.setLong(2, iwc.getCount());
                    preparedStatement.setString(3, iwc.getUrl());
                    preparedStatement.setTimestamp(4, new Timestamp(iwc.getTimeStamp()));
                }};

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

    static Properties getProperties(String filePath) throws IOException {
        InputStream propertiesInput = Upload.class.getResourceAsStream(filePath);
        Properties properties = new Properties();
        properties.load(propertiesInput);
        return properties;
    }
}
