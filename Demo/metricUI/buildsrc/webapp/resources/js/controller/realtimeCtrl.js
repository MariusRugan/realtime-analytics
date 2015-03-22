/*
 * Copyright 2013-2015 eBay Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @description
 * @author
 * @class
 * @memberOf
 * @namespace
 */
app.controller('realtimeCtrl',
    [
        '$scope',
        '$timeout',
        'WebSocketService',
        function realtimeCtrl($scope, $timeout, WebSocketService) {
            'use strict';
            /* jshint validthis: true */
            var vm = this;

            vm.data = {};

            vm.trendDatas = [];

            vm.tableTopCountryCountData = {
                data: [],
                timestamp: null
            };

            vm.OSDatas = [];
            vm.browserDatas = [];

            vm.tableDeviceCountData = {
                    data: [],
                    timestamp: null
            };

            vm.initCtrl = function() {
                vm.initRealTimeCounter = true;
                vm.realTimeCounter = 5;
                vm.initRealTimeWebsocket();
            };

            vm.initRealTimeWebsocket = function(){
                vm.realTimeWebsocketMetric = WebSocketService.connect(
                    'MC_Metric&PulsarOsCount&PulsarBrowserCount&PulsarTopCountryCount&PulsarDeviceCount',
                    '1',
                    vm.realTimeRenderData
                );
            };

            vm.realTimeCountdown = function(){
                if(vm.realTimeCounter > 1){
                    vm.realTimeCounter--;
                    vm.realTimeCountdownPromise = $timeout(vm.realTimeCountdown, 1000);
                }
            };

            vm.realTimeRenderData = function(data, timestamp){

                var objs = JSON.parse(data);

                var dataType;

                if (objs && objs.length > 0){

                    dataType = objs[0].js_ev_type;

                    if (dataType == 'MC_Metric') {
                        vm.trendDatas = [];
                        if (vm.initRealTimeCounter){
                            if(vm.realTimeCountdownPromise){
                                $timeout.cancel(vm.realTimeCountdownPromise);
                            }
                            //console.info('reset realtime counter');
                            vm.realTimeCounter = 10;
                            vm.realTimeCountdownPromise = $timeout(vm.realTimeCountdown, 1000);
                        } else {
                            vm.initRealTimeCounter = true;
                        }
                    } else if(dataType == 'PulsarTopCountryCount') {
                        vm.tableTopCountryCountData = {
                            data: [],
                            timestamp: null
                        };
                    } else if (dataType == 'PulsarBrowserCount') {
                        vm.browserDatas = [];
                    } else if  (dataType == 'PulsarOsCount') {
                        vm.OSDatas = [];
                    } else if (dataType === 'PulsarDeviceCount') {
                        vm.tableDeviceCountData = {
                            data: [],
                            timestamp: null
                        }
                    }
                }

                //Adjust data
                objs.forEach(function(el){

                    if (dataType == 'MC_Metric'){

                        // Trend chart
                        var trendData = {x:el.timestamp, y:el.value};
                        vm.trendDatas.push(trendData);
                        vm.trendChartData = [{key:'Metrics',values: vm.trendDatas}];

                    } else if(dataType == 'PulsarTopCountryCount') {

                        // TopCountry table
                        vm.tableTopCountryCountData.data.push({country:el.country, value:el.value});
                        vm.tableTopCountryCountData.timestamp = timestamp;

                        vm.tableTopCountryCountData.data.sort(function (a, b) {
                            return (+a.value) < (+b.value);
                        });

                    } else if (dataType == 'PulsarBrowserCount') {

                        // Browser chart
                        var browserData = {key:el.browser, val:el.value};
                        vm.browserDatas.push(browserData);
                        vm.browserChartData = vm.browserDatas;

                    } else if  (dataType == 'PulsarOsCount') {

                        // OS chart
                        var OSData = {key:el.os, val:el.value};
                        vm.OSDatas.push(OSData);
                        vm.osChartData = vm.OSDatas;

                    } else if  (dataType === 'PulsarDeviceCount') {

                        // Devices table
                        var _deviceName = (el.device) ? el.device : "unknown";
                        vm.tableDeviceCountData.data.push({key:_deviceName, val:el.value});
                        vm.tableDeviceCountData.timestamp = timestamp;
                        vm.tableDeviceCountData.data.sort(function (a, b) {
                            return (+a.value) < (+b.value);
                        });

                    }
                });
            };

            //trend chart x axis format
            vm.xTimeFormat = function (data) {
                return moment(data).format('H:mm:ss');
            };

            $scope.$on('$destroy',function(){
                vm.realTimeWebsocketMetric.connection.onclose = function onClose() {
                    if (vm.realTimeCountdownPromise) {
                        $timeout.cancel(vm.realTimeCountdownPromise);
                    }
                    vm.initRealTimeCounter = false;
                };
                vm.realTimeWebsocketMetric.connection.close();
            });
        }
    ]
);
