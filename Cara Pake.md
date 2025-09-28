Buat build supaya jadi .cs3 plugin gunain ini

`.\gradlew.bat Amoytoge:make`

Itu bakal bikin .cs3 di folder `build`

Buat development plugin ke WSA

- Jalanin WSA
- Connect adb ke WSA gunain `adb connect 127.0.0.1:58526`
- build gunain :pushToDevice ex:`.\gradlew.bat BokepKamu:pushToDevice`
- Itu bakal nge build .cs3 terus push ke WSA client ke `/sdcard/Cloudstream3/plugins/`
- Path itu untuk ngetes pluginnya

Cek di discord bisa pake
.\gradlew.bat amoytoge:Deploywithadb

Todo:
https://kingbokep.tv/
