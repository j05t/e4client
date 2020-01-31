# e4client
Custom client for the E4 wristband from Empatica. Developed for https://www.fh-joanneum.at.

Based on https://github.com/empatica/empalink-sample-project-android.

<img src="https://github.com/j05t/e4client/blob/master/Screenshot_0.jpg" alt="Screenshot" width="22%" height="22%" /> <img src="https://github.com/j05t/e4client/blob/master/Screenshot_1.jpg" alt="Screenshot" width="22%" height="22%" /> <img src="https://github.com/j05t/e4client/blob/master/Screenshot_3.jpg" alt="Screenshot" width="22%" height="22%" /> 

<img src="https://github.com/j05t/e4client/blob/master/Screenshot_4.jpg" alt="Screenshot" width="22%" height="22%" /> <img src="https://github.com/j05t/e4client/blob/master/Screenshot_2.jpg" alt="Screenshot" width="22%" height="22%" /> <img src="https://github.com/j05t/e4client/blob/master/Screenshot_5.jpg" alt="Screenshot" width="22%" height="22%" />

### Setup:

Google Fit: https://console.cloud.google.com/apis/credentials
* Create OAuth2 key and insert Android Studio SHA1 fingerprint
* https://developers.google.com/android/guides/client-auth
* Configure Google Fit Consent Screen

Edit apikeys.properties
* Insert SciChart License: https://www.scichart.com/

### todo:
* Calculate average heart rate from blood volume pulse. 
* Log average heart rate every 1000ms.
* Validate HRV metrics. 
* Use foreground service for Bluetooth connection.
* Fix Google Fit integration.
* Check filters for correctness.

https://support.empatica.com/hc/en-us/articles/360030058011-E4-data-IBI-expected-signal

https://stackoverflow.com/questions/22583391/peak-signal-detection-in-realtime-timeseries-data

https://stackoverflow.com/questions/31753062/calculate-heart-rate-from-ecg-stream-java-nymi-band
