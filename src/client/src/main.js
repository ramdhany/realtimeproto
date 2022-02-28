/****************************************************************************
/* FILE:                main.js                								*/
/* DESCRIPTION:         NumberGeneratorSequence service client	 	        */
/* VERSION:             (see git)                                       	*/
/* DATE:                (see git)                                       	*/
/* AUTHOR:              Rajiv Ramdhany <nirish777@gmail.com>    		    */
/****************************************************************************/


// ---------------------------------------------------------
//  Imports, Globals, etc.
// ---------------------------------------------------------



var PROTO_PATH = __dirname + "../../../proto/numseq.proto";

var commandLineArgs = require("command-line-args");
var commandLineUsage = require("command-line-usage");
var Logger = require("./logger");
var  url = require("url");
var async = require('async');
var fs = require('fs');
var path = require('path');
var _ = require('lodash');
var grpc = require('@grpc/grpc-js');
var protoLoader = require('@grpc/proto-loader');
const { count } = require("console");

var packageDefinition = protoLoader.loadSync(
    PROTO_PATH,
    {keepCase: true,
     longs: String,
     enums: String,
     defaults: true,
     oneofs: true
    });


var numbergenerator = grpc.loadPackageDefinition(packageDefinition).numbergenerator;
var grpcNumSeqClient; // grpc service client stub


var config={
    serviceURL: undefined,
    mode: undefined,
    count: 0
};
var logger;



// ---------------------------------------------------------
//  Utility functions 
// ---------------------------------------------------------

Promise.retry = function(fn, args, times, delay) {
	return new Promise(function(resolve, reject){
		var error;
		var attempt = function() {
			if (times == 0) {
				reject(error);
			} else {
				fn(args).then(resolve).catch(
					function (e) {
						times--;
						error = e;
						setTimeout(function () { attempt(); }, delay);
					}
				);
			}
		};
		attempt();
	});
};

// ---------------------------------------------------------
// ---------------------------------------------------------
//  Start
// ---------------------------------------------------------

// command line usage guide
const sections = [
    {
      header: 'Number Sequence Node JS Client',
      content: 'Gets and processes a stream of numbers from the NumberGeneratorSequence service'
    },
    {
      header: 'Options',
      optionList: [
        {
            name: 'help',
            description: 'Display usage guide.',
            alias: 'h',
            type: Boolean
          },
          {
            name: 'server',
            description: 'Number sequence generator service host:port',
            alias: 'u',
            type: String,
            typeLabel: '{underline host:port}'
          },
          {
            name: 'mode',
            description: 'Service mode: 0 - STATELESS, 1 - STATEFUL',
            alias: 'm',
            type: Number,
            typeLabel: '{underline number}'
          },
          {
            name: 'count',
            description: 'Total count of numbers to receive. Must be > 0',
            alias: 'c',
            type: Number,
            typeLabel: '{underline number}'
          },
          {
            name: 'verbose',
            description: 'Log level',
            alias: 'v',
            type: Boolean,
          }
      ]
    }
  ]
const usage = commandLineUsage(sections)


var optionDefinitions = [
    { name: "serviceUrl", alias: "s", type: String},
    { name: "mode", alias: "m", type: Number },
    { name: "count", alias: "c", type: Number },
    { name: "verbose", alias: "v", type: Boolean, defaultValue: false },
    { name: "help", alias: "h", type: Boolean },
];


function main() {
    
    try {
        var options = commandLineArgs(optionDefinitions);
        
        if (((typeof  options.serviceUrl === "undefined") || (  options.serviceUrl === null)) 
            || ((typeof  options.mode === "undefined") || (  options.mode === null)) 
            || ((typeof  options.count === "undefined") || (  options.count === null) || (options.count <= 0)) 
        )
        {
            console.log(usage);
            console.error("Invalid/missing one of these parameters: --serviceUrl (-s), --mode (-m) --count (-c)");
            return;
        }


        if (options.help == true)
        {
            console.log(usage);
            return;
        }

        

        options.loglevel = options.verbose ? "development" : "info";
        logger = Logger.getNewInstance(options.loglevel, "num-sequence-generator");

        config.serviceURL = options.serviceUrl;
        config.mode = options.mode;
        config.count = options.count;

        
        logger.debug("config: ", JSON.stringify(config));

        var sum = 0;
        var finalNumValues =0;
        var args = {
          serviceURL: config.serviceURL,
          count: config.count,
          serverMode: config.mode,
          processorFn: (value) =>{
            sum += value;
            logger.info(value, " sum: ", sum);
          },
        }

        GetNumberSequence(args).catch((status)=>{

          logger.error("Error: ", status.error.details, "  . Received ", status.countReceived, " numbers. Last value: " , status.lastValReceived);

          logger.info("Retrying for remaining values ...")
          finalNumValues += status.countReceived;
          // update parameters for rety call
          args.count -= status.countReceived;
          args.restartValue = status.lastValReceived;
          return Promise.retry(GetNumberSequence, args, 4, 10000);
        }).then((countReceived)=>{

          finalNumValues += countReceived;
          logger.info("Received ", finalNumValues, " numbers. Total sum of all numbers: ", sum );   


        });



        // CRTL-C handler
        process.on("SIGINT", function () {
            process.exit();
        });



    } catch (e) {
        logger.error(e);
    }
}


main();



// ---------------------------------------------------------
//  Operations
// ---------------------------------------------------------


/**
 * Async function to get a sequence of numbers from NumberSeqGenerator service
 * @param {*} args object with the following properties and values
 * @param {string} args.serviceURL  grpc server name and port e.g. localhost:5000
 * @param {number} args.count  number of values to receive from service
 * @param {number} args.count  number of values to receive from service
 * @param {function} args.processorFn an aggregator function to process received values
 * @param {*} args.restartValue Optional start value
 * @returns a Promise
 */
function GetNumberSequence(args)
{

  // create a grpc client stub
  var client  =  new numbergenerator.NumberSequenceGenerator(args.serviceURL, grpc.credentials.createInsecure());
  var count = args.count;
  var processorFn = args.processorFn;
  var restartValue = args.restartValue;
  var serviceMode = args.serverMode ;

  return new Promise(function(resolve, reject){

    logger.info("Getting ", count, " numbers from NumberSequenceGenerator service ...");
    
       
    // create a request object
    var numSeqRequest = {
        clientId: "testclient",
        startNumber: restartValue || 0,
        intervalMs: 0,
        numTotalMessages: count,
        serviceMode: serviceMode
    };
    logger.debug(numSeqRequest);

    var i = 0;  // counter for messages received from server
    var lastValueRecvd = 0;
    var stream  = client.startNumberSequence(numSeqRequest);
    

    stream.on("data", (response)=>{
      ++i;
      lastValueRecvd =  parseInt(response.number, 10);

      processorFn(lastValueRecvd);

      if (i >= count)
        stream.cancel();
      
    });

    stream.on("error", (error)=>{
      logger.debug("Error event: ", JSON.stringify(error));

        if (error.code !==1)
        {
          reject({ error: error, 
                   countReceived: i, 
                   lastValReceived: lastValueRecvd});
        } 
    });

    stream.on("end", ()=>{

      if (i === count)
      {
        resolve(count);
      }else
      {
        reject({ error: {code: 0, details: ""}, 
                countReceived: i, 
                lastValReceived: lastValueRecvd});
      }

    });

  });


    

}