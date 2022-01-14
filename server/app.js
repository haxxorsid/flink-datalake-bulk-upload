var express = require('express');
var app = express();
var {Kafka} = require('kafkajs');
var sql = require('mssql');
var multer = require('multer')
var cors = require('cors');

app.use(cors());
app.use(express.urlencoded({ extended: true }));

//  Configuration to push messages in the Kafka endpoint (Azure Event Hub)
var producer = new Kafka({
  clientId: 'flink-upload',
  brokers: ['<fill-in>.servicebus.windows.net:9093'],
  ssl: true,
  sasl :{
    mechanism: 'plain',
    username: '<fill-in>',
    password: '<fill-in>'
  }
}).producer();

// Apache kafka topic name (Event Hub name)
var topicName = '<fill-in>';

// SQL Database connection string to fetch UploadSummary
const sqlConnectionString = "<fill-in>";

// pushing submitted images in kafka topic
app.post('/push', multer().array('file'), async function(req, res) {
    if(req.files !== undefined) {
      try {
        // empty array to create a batch of images together
          var messages = [];
          for(var x =0;x<req.files.length;x++) {
            console.log(req.files[x].originalname)
            let file = req.files[x];
            // sending name, content, encoding type in the Kafka message
            var value = Buffer.from(`{ "name": "${file.originalname}", "buffer": "${file.buffer.toString('base64')}", "encoding": "${file.encoding}", "mimetype": "${file.mimetype}"}`);
            console.log(value);
            console.log(file.buffer);
            var key = "key-"+x;
            messages.push({key, value});
          }
          // push all together as a batch
          producer.send({topic: topicName, messages});
      } catch(err) {
        console.log(err);
        res.status(500).send('Something went wrong while pushing messages!');
      }
  }
  res.status(200).send('Done!');
});

// getting summary of all previous image uploads
app.get('/summary', async function(req, res) {
  try {
    await sql.connect(sqlConnectionString);
    // query to get upload summary ordered by time in descending to show latest results at the top of grid.
    const result = await sql.query`select * from [dbo].[UploadSummary] order by [CreatedAt] desc, [UploadSummaryId] desc;`
    console.log(result.recordset);
    res.status(200).send(result.recordset);
  } catch (err) {
    console.log(err);
    res.status(500).send('Something went wrong while fetching upload summary from SQL database!');
  }
});

producer.connect();

// error handler
app.use(function(err, req, res, next) {
  // set locals, only providing error in development
  res.locals.message = err.message;
  res.locals.error = req.app.get('env') === 'development' ? err : {};

  // render the error page
  res.status(err.status || 500);
  res.render('error');
});

module.exports = app;