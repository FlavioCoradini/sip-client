var express = require('express');
var router = express.Router();

var Ports = require('../models/ports');


var mysql      = require('mysql');
var connection = mysql.createConnection({
  host     : '172.16.28.3',
  user     : 'televoto',
  password : 'w3w4x3f9',
  database : 'televoto'
});



router.get('/', function(req, res, next) {

    res.render('ports', {ports: Ports})

});


router.get('/getAvailable', function(req, res, next) {

    var port = Ports.getAvailable()

    Ports.disavailablePort(port)

    res.json({port: port})

})



router.get('/available/:port', function(req, res, next) {

  Ports.availablePort(req.params.port)

  res.json({message: 'Port '+ req.params.port +' Enabled'})
  
});








module.exports = router;
