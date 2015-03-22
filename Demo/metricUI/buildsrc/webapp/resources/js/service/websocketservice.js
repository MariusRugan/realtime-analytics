angular.module("pulsarRtaDemoApp")
.factory('WebSocketService',
     [
         '$resource',
         /**
          *
          * @param $resource
          * @returns {*}
          */
         function WebSocketService($resource) {
             "use strict";

             var pulsarmetric = {
                 connect: connect,
                 connection: connection
             };

             var connection = null;

             function connect(source, query, callback) {

                 if (connection != null) {
                     connection.close();
                 }

                 if (!window.MozWebSocket && !window.WebSocket) {
                     alert('Please use Firefox or Chrome.');
                     return null;
                 }

                 if (query == null || query.length == 0) {
                     query = '&';
                 }

                 query = query.replace(/=/g, 'equal');

                 var WS = window.MozWebSocket ? MozWebSocket : WebSocket;
                 pulsarmetric.connection = new WS('ws://' + location.host + '/websocket', [source, query]);

                 pulsarmetric.connection.onmessage = function (evt) {
                     if (callback != null) {
                         callback(evt.data, evt.timeStamp);
                     }
                 };
                 return pulsarmetric;
             }

             return pulsarmetric;

         }
     ]
);
