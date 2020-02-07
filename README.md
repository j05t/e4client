# e4client
Custom client for the E4 wristband from Empatica. Developed for https://www.fh-joanneum.at, based on https://github.com/empatica/empalink-sample-project-android. Project description: https://github.com/j05t/e4client/blob/master/project_description.pdf. A 64 bit build is not supported by the Empatica SDK.

Version 0.1 APK files available for download at
* https://github.com/j05t/e4client/releases/tag/0.1
* https://play.google.com/store/apps/details?id=com.jstappdev.e4client


<img src="https://github.com/j05t/e4client/blob/master/Screenshot_0.jpg" alt="Screenshot" width="22%" height="22%" /> <img src="https://github.com/j05t/e4client/blob/master/Screenshot_1.jpg" alt="Screenshot" width="22%" height="22%" /> <img src="https://github.com/j05t/e4client/blob/master/Screenshot_3.jpg" alt="Screenshot" width="22%" height="22%" /> 

<img src="https://github.com/j05t/e4client/blob/master/Screenshot_4.jpg" alt="Screenshot" width="22%" height="22%" /> <img src="https://github.com/j05t/e4client/blob/master/Screenshot_2.jpg" alt="Screenshot" width="22%" height="22%" /> <img src="https://github.com/j05t/e4client/blob/master/Screenshot_5.jpg" alt="Screenshot" width="22%" height="22%" />

### Setup:

Google Fit: https://console.cloud.google.com/apis/credentials
* Create OAuth2 key and insert Android Studio SHA1 fingerprint
* https://developers.google.com/android/guides/client-auth
* Configure Google Fit consent screen

Edit apikeys.properties
* Insert SciChart License: https://www.scichart.com/

### todo:
* Calculate average heart rate from blood volume pulse
* Validate HRV metrics: https://www.ncbi.nlm.nih.gov/pmc/articles/PMC5624990/
* Use foreground service for Bluetooth connection
* Full Google Fit sync (only session metadata works currently)
* Check median/average filters for correctness

https://support.empatica.com/hc/en-us/articles/360030058011-E4-data-IBI-expected-signal

https://stackoverflow.com/questions/22583391/peak-signal-detection-in-realtime-timeseries-data

https://stackoverflow.com/questions/31753062/calculate-heart-rate-from-ecg-stream-java-nymi-band

https://www.researchgate.net/figure/HRV-time-domain-measures_tbl1_320078994

https://link.springer.com/article/10.1007%2Fs00521-019-04278-7
