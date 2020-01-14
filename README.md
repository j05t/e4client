# e4client
Custom client for the E4 wristband from Empatica

Based on https://github.com/empatica/empalink-sample-project-android

<img src="https://github.com/j05t/e4client/blob/master/Screenshot_0.jpg" alt="Screenshot" width="30%" height="30%" /> <img src="https://github.com/j05t/e4client/blob/master/Screenshot_3.jpg" alt="Another Screenshot" width="30%" height="30%" /> <img src="https://github.com/j05t/e4client/blob/master/Screenshot_2.jpg" alt="Another Screenshot" width="30%" height="30%" />
<img src="https://github.com/j05t/e4client/blob/master/Screenshot_1.jpg" alt="Screenshot" width="91%" height="91%" />

## Setup:

Google Fit: https://console.cloud.google.com/apis/credentials
* Create OAuth2 key and insert Android Studio SHA1 fingerprint 
* https://developers.google.com/android/guides/client-auth

Edit apikeys.properties
* insert Empatica API key: https://www.empatica.com/connect/developer.php
* insert SciChart License: https://www.scichart.com/

## TODO:
Calculate average heart rate from blood volume pulse. Log average heart rate every 1000ms.

https://stackoverflow.com/questions/22583391/peak-signal-detection-in-realtime-timeseries-data

https://stackoverflow.com/questions/31753062/calculate-heart-rate-from-ecg-stream-java-nymi-band
