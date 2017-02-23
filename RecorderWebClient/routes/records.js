var express = require('express');
var router = express.Router();

var mysql      = require('mysql');
var connection = mysql.createConnection({
  host     : '172.16.28.3',
  user     : 'televoto',
  password : 'w3w4x3f9',
  database : 'televoto'
});


router.get('/', function(req, res, next) {
  
  //connection.connect();

  connection.query('SELECT * from televoto.records', function(err, rows, fields) {

    if (err) throw err;

    res.render('records', {records: rows} )

  });

  //connection.end();
  
});


router.get('/download/:call_id', function(req, res, next) {
  
    var records_path = __dirname.replace("/RecorderWebClient/routes", "") + "/records/"

    res.download(records_path + req.params.call_id + ".wav")
  
});






module.exports = router;
