angular.module("pulsarRtaDemoApp")
.factory('MetricService',
    [
        '$resource',
        function MetricService($resource) {
            "use strict";
            return $resource(
                'http://' + location.host + '/pulsar/metric?:params' + '',
                {params: null},
                {query: {method: 'GET', cache: false, isArray: true}}
            );
        }
    ]
);
