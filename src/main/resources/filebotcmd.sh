root@ubuntu-2gb-nbg1-1:~/filebot# pwd
/root/filebot
root@ubuntu-2gb-nbg1-1:~/filebot# ls
'Dexter New Blood - Temporada 1 [HDTV 720p][Cap.106][AC3 5.1 Castellano][www.atomixHQ.NET]'
root@ubuntu-2gb-nbg1-1:~/filebot# nano license.psm
root@ubuntu-2gb-nbg1-1:~/filebot# filebot --license license.psm 
Activate License [P21330104] on [Tue Dec 14 09:33:38 UTC 2021]
Write [FileBot License P21330104 (Valid-Until: 2021-12-20)] to [/root/.filebot/license.txt]
FileBot License P21330104 (Valid-Until: 2021-12-20) has been activated successfully.
root@ubuntu-2gb-nbg1-1:~/filebot# filebot -rename .
Classify media files
No media files: [/root/filebot/license.psm]
Failure (×_×)⌒☆
root@ubuntu-2gb-nbg1-1:~/filebot# ls
'Dexter New Blood - Temporada 1 [HDTV 720p][Cap.106][AC3 5.1 Castellano][www.atomixHQ.NET]'   license.psm
root@ubuntu-2gb-nbg1-1:~/filebot# filebot -rename 
Dexter New Blood - Temporada 1 [HDTV 720p][Cap.106][AC3 5.1 Castellano][www.atomixHQ.NET]/ license.psm
root@ubuntu-2gb-nbg1-1:~/filebot# filebot -rename Dexter\ New\ Blood\ -\ Temporada\ 1\ \[HDTV\ 720p\]\[Cap.106\]\[AC3\ 5.1\ Castellano\]\[www.atomixHQ.NET\]/
Classify media files
* Consider specifying --db TheTVDB or --db TheMovieDB explicitly
Rename episodes using [TheTVDB] with [Airdate]
Lookup via [Dexter: New Blood, Dexter, Dexter's Laboratory]
Fetching episode data for [Dexter: New Blood]
Fetching episode data for [New Blood]
Fetching episode data for [Dexter]
Fetching episode data for [Dexter's Laboratory]
Multiple Options: /root/filebot/Dexter New Blood - Temporada 1 [HDTV 720p][Cap.106][AC3 5.1 Castellano][www.atomixHQ.NET]/Dexter New Blood - Temporada 1 [HDTV 720p][Cap.106][AC3 5.1 Castellano][www.atomixHQ.NET].mkv: [Dexter: New Blood, New Blood, Dexter, Dexter's Laboratory]
* Consider using -non-strict to enable advanced auto-selection
* Consider using --filter "id in [412366]" or --q "Dexter: New Blood" to select one specific series
* Consider using --mode interactive to enable interactive mode
Failed to process group: {Movie=null, Series=dexter new blood} [/root/filebot/Dexter New Blood - Temporada 1 [HDTV 720p][Cap.106][AC3 5.1 Castellano][www.atomixHQ.NET]/Dexter New Blood - Temporada 1 [HDTV 720p][Cap.106][AC3 5.1 Castellano][www.atomixHQ.NET].mkv]
└ Failed to identify or process any files
root@ubuntu-2gb-nbg1-1:~/filebot# filebot -rename Dexter\ New\ Blood\ -\ Temporada\ 1\ \[HDTV\ 720p\]\[Cap.106\]\[AC3\ 5.1\ Castellano\]\[www.atomixHQ.NET\]/ --db TheTVDB
Rename episodes using [TheTVDB] with [Airdate]
Lookup via [Dexter: New Blood, Dexter, Dexter's Laboratory]
Fetching episode data for [Dexter: New Blood]
Fetching episode data for [New Blood]
Fetching episode data for [Dexter]
Fetching episode data for [Dexter's Laboratory]
Multiple Options: /root/filebot/Dexter New Blood - Temporada 1 [HDTV 720p][Cap.106][AC3 5.1 Castellano][www.atomixHQ.NET]/Dexter New Blood - Temporada 1 [HDTV 720p][Cap.106][AC3 5.1 Castellano][www.atomixHQ.NET].mkv: [Dexter: New Blood, New Blood, Dexter, Dexter's Laboratory]
* Consider using -non-strict to enable advanced auto-selection
* Consider using --filter "id in [412366]" or --q "Dexter: New Blood" to select one specific series
* Consider using --mode interactive to enable interactive mode
Failed to identify or process any files
Failure (×_×)⌒☆
root@ubuntu-2gb-nbg1-1:~/filebot# filebot -rename Dexter\ New\ Blood\ -\ Temporada\ 1\ \[HDTV\ 720p\]\[Cap.106\]\[AC3\ 5.1\ Castellano\]\[www.atomixHQ.NET\]/ --db TheTVDB --q "Dexter: New Blood"
Rename episodes using [TheTVDB] with [Airdate]
Lookup via [Dexter: New Blood]
Fetching episode data for [Dexter: New Blood]
[MOVE] from [/root/filebot/Dexter New Blood - Temporada 1 [HDTV 720p][Cap.106][AC3 5.1 Castellano][www.atomixHQ.NET]/Dexter New Blood - Temporada 1 [HDTV 720p][Cap.106][AC3 5.1 Castellano][www.atomixHQ.NET].mkv] to [/root/filebot/Dexter New Blood - Temporada 1 [HDTV 720p][Cap.106][AC3 5.1 Castellano][www.atomixHQ.NET]/Dexter New Blood - 1x06 - Too Many Tuna Sandwiches.mkv]
Processed 1 file
root@ubuntu-2gb-nbg1-1:~/filebot# ls
'Dexter New Blood - Temporada 1 [HDTV 720p][Cap.106][AC3 5.1 Castellano][www.atomixHQ.NET]'   license.psm
root@ubuntu-2gb-nbg1-1:~/filebot# ls Dexter\ New\ Blood\ -\ Temporada\ 1\ \[HDTV\ 720p\]\[Cap.106\]\[AC3\ 5.1\ Castellano\]\[www.atomixHQ.NET\]/
'Dexter New Blood - 1x06 - Too Many Tuna Sandwiches.mkv'
root@ubuntu-2gb-nbg1-1:~/filebot# 
