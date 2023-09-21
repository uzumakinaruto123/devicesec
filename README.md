# DeviceSec


Cordova plugin to get current loction using Native Location Manager.

<br>
##### Platforms

- Android

<br>

##### Installation

```
cordova plugin add https://github.com/uzumakinaruto123/DeviceSec.git
```


##### Usage

```
declare var DeviceSec:any;

...

DeviceSec.getCurrentLocation((res)=>{
	console.log(res.latitude+', '+res.longitude);
},(err)=>{
	console.log('Error getting location', err);
});
```


##### Note: 
Application should have Location permissions to get current location