cordova.define("com.cordova.DeviceSec.DeviceSec", function(require, exports, module) {
    var exec = require('cordova/exec');
    var PLUGIN_NAME = 'DeviceSec';
    
    
    var DeviceSec = {
        // isTampered: function(success, error, sig) {
        //     exec(success, error, "DeviceSec", "isTampered", [sig]);
        // },
        // isRooted: function(success, error) {
        //     exec(success, error, "DeviceSec", "isRooted", []);
        // },
        // isDebuggingEnabled: function(success, error) {
        //     exec(success, error, "DeviceSec", "isDebuggingEnabled", []);
        // },
        isRooted: function (arg) {
            return new Promise(function (resolve, reject) {
                cordova.exec(resolve, reject, 'DeviceSec', 'isRooted', []);
            });
        },
        isDebuggingEnabled: function (arg) {
            return new Promise(function (resolve, reject) {
                cordova.exec(resolve, reject, 'DeviceSec', 'isDebuggingEnabled', []);
            });
        },
        isTampered: function (arg) {
            return new Promise(function (resolve, reject) {
                cordova.exec(resolve, reject, 'DeviceSec', 'isTampered', [arg]);
            });
        }
    };
    
    module.exports = DeviceSec;
    });
    