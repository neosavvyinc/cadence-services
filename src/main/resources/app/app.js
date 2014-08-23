var app = angular.module(
    "application",
    [
        'nvd3',
        'ui.bootstrap'
    ]
).
    config([
        function(){
            console.log("configuration...")
        }
    ])
    .run([
        "$rootScope",
        function($rootScope){
            console.log("Application is started")
        }
    ]
)

app.controller('CadenceAppController', [
        "$scope",
        "$rootScope",
        "$http",
        "$location",
        function(
            $scope,
            $rootScope,
            $http,
            $location){

            $scope.setup = function() {
                $http({
                    method: 'POST',
                    url: '/setup',
                    data: $scope.emailAddress
                    }
                ).then(function(data) {
                        console.log("com.cadence.User updated")
//                        $scope.loadUsers()
                    }, function(error){
                        console.log("com.cadence.User not updated...")
                    })
            }

            $scope.emailAddress = ""

            $scope.loadUsers = function() {
                $http({method: 'GET', url: '/listUsers'}).then(
                    function(response){
                        $scope.users = response.data;
                    }
                )
            }

            $scope.checkin = function() {
                var checkin = {
                    uuid: $scope.users[0].uuid
                }

                $http({
                    method: 'POST',
                    url: '/checkin',
                    data: JSON.stringify(checkin)
                }).then(
                    function(response) {

                    }
                )
            }

            $scope.onSelect = function(choice) {
                $scope.queryType = choice;
            }
            $scope.queryType = "Hour"
            $scope.queryTypeOptions = [
                "Minute",
                "Hour",
                "Day",
                "Month"
            ]

            $scope.$watch('queryType', function() {
                $scope.loadMetrics()
            })
            $scope.loadMetrics = function() {
                $http({
                    method: 'GET',
                    url: '/graphMetrics?q=' + $scope.queryType
                }).then(
                    function(response){
                        $scope.metrics = [{
                            values: [],
                            key: 'Metrics',
                            color: '#ff7f0e'
                        }];
                        _.forEach(response.data, function(item) {

                            $scope.metrics[0].values.push({
                                x: moment(item.time),
                                y: item.count
                            })

                        });
                    }
                )
            }

            $scope.users = []


            $scope.chartTypeOptions = [
                'discreteBarChart',
                'lineChart'
            ];
            $scope.selectedChartType = $scope.chartTypeOptions[1]

            $scope.$watch("selectedChartType", function() {
                if( $scope.api ) {
                    $scope.options.chart.type = $scope.selectedChartType
                    $scope.api.updateWithOptions($scope.options);
                }
            })

            $scope.options = {
                chart: {
                    type: $scope.selectedChartType,
                    height: 450,
                    margin : {
                        top: 20,
                        right: 20,
                        bottom: 40,
                        left: 55
                    },
                    xAxis: {
                        "axisLabel": "Check In Time",
                        "showMaxMin": false,
                        "staggerLabels": true,
                        "tickFormat": function(d) {
                            return d3.time.format("%y-%m-%d %H:%M")(new Date(d))
                        }
                    },
                    yAxis: {
                        axisLabel: 'Count',
                        tickFormat: function(d){
                            return d3.format('.02f')(d);
                        },
                        axisLabelDistance: 30
                    }
                },
                title: {
                    enable: true,
                    text: 'Summary of Checkins by Time'
                }
            };



            $scope.metrics = []

            $scope.loadUsers();
            $scope.loadMetrics();

            $scope.openSocket = function() {
                var ws = new ReconnectingWebSocket("ws://" + $location.host() + ":" + $location.port())
                ws.onmessage = function (event) {
                    console.log("Receiving a message from the server");
                    console.log(event.data);

                    var message = JSON.parse(event.data)
                    if( message.event === "dataChanged" ) {
                        $scope.loadUsers();
                    }
                    else if( message.event === "metricsChanged" ) {
                        $scope.loadMetrics()
                    }
                }

            }

            $scope.openSocket();
        }
    ]
)