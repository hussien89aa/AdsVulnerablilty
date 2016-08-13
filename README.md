#Detecting dangerous permissions that used by Android Third-party libraries  
###Hussein Al Rubaye

 ##Abstract
 Android dangerous permission which is most import security part for Android developers. Because it contain  access to user privacy information such as ( user location, phone, photo, SMS, Contact, Microphone and camera) . The developer have to keep this information secure from being used by anyone. Third party library which is part from Android apps could access to same permission that granted to the app. To prevent third -party library from using user apps permissions, We build tool that help Android developers to  detect the permissions that used in third-party  by searching in deep code. We tested 15 library and find the dangerous permission that used in this libraries. As results, We categorize this  permissions into two types symbol. First, We used “X” for the library that used dangerous permissions without any convincing reason. Second, We used  “O”   for the library that used dangerous permissions with convincing reason.


##Introduction

 	Android is open source system which is used in wide range of handheld devices around the world. Android Apps different from PC apps in dealing with user privacy informations.  Because Android  be in touch with most of user’s private information such as Photos, Videos, Voice Recording, and user’s location. Even Though Android provide model for User’s permissions that allows to end user to give or deny any permissions required by any apps. So if the app ask to access to user’s location and the user deny the request the app cannot use this permission. 
	However there are many security leaks in android Apps in play store. This leaks could be used to steal user’s private information by using third party apps such as Ads library or Malicious apps. When we used these  Third-party library, we add it in our Android App, it will be as part of our Apps. That is mean this libraries will have access to same permissions that our app could access to.   If the user allows to our app to access to his location, the Third-party Library could access to his location also. Because Third-party Library could use all permission that granted the parent app.  Because these Third-party library being part from the app, and could use all permissions that app have it. So we have to protect user privacy, and we have to make sure these library are not  using  user’s private information for advertisement. 
 	We build tool that help Android developers to keep the privacy information of their apps’ users more secure.  By test Third-party libraries permissions in deep code to find the dangerous permissions that used by the Third-party library which  could use developer app permissions to access to user privacy information. 


##Related works
There are many researches done in this area. However most of these research focused on Ads library only. They never focused on Another Third-party libraries that developer use to reduce the effort that have have to do with his app.
 One of researchers  implement  Compac [1] tool which could restrict the using of App’s permissions from being used by third party library. We could define which permission could be used by the Ads library and which of them could be used by the App. This tool will deny any permission requested by Ads library which is not defined in his package space permission even it defined to be using by the app. So we will protect App permissions from being used by any third party library. 
Another researcher implement AdsHoneyDroid [3] tool which executes the apps and detects malicious advertisement . This tool  prevent Ads library from doing sensitive system calls such as  SmsManager.sendTextMessage and TelephonyManager.getLine1Number  
	Most of this researches focused on real-time permissions using. They look for  Third-party permission using in real-time which is not good.  Because  We cannot find all Third-party permission leaks by testing it for period of time we have to test Third-party code and check for all permissions leaks, and that is what our tool doing.


##Implementation:

	 To detect third party libraries permission, we collected  14 f most  popular Android Third-party libraries that used by developer in Google play store. We select these library from different categories. For example we select  (Aquery, Android Async HTTP ,Gson, ion, Ksoap, OkHttp, dagger, retrofit, volley, and Picasso)  from network libraries that allow apps to  collect data from web serve. Furthermore, we selected ( Adcolony, Apps flye, Appodeals, Chartboost, In mobile ) from Ads libraries which allows advertisements  to show in the app.  
Then after we collect these libraries we developed  Linux script that take every library as input then convert to to cover Java file using cfr_0_114 library which is decompiler library  that convert any .JAR library to java source code. Then we  search in the  extracted Java files for dangerous permissions that used by this library  by identify set of dangerous code that identified by Google developer [1], we looked for using these code inside the Third-part library, and display the used  dangerous code and the name of the file that hold it, as it explain in figure (1). 


Figure (1) : Extraction system
  So when we run the script on any library it will shows which dangerous permissions this library used without asking for using this permission in library manifest  in app manifest .

Figure (2) : Extraction system running

In Figure(2), We show our project Structure in the right which contain:
Folder for JAR files which contain the Third-party libraries.
Folder for output files which contain Java code for Third-party libraries after extraction. 
 cfr_0_114 library that used to get Java code from JAR library.
Our Linux script named “extract.command” which  extract Jar library  to Java source code and find out which dangerous permissions are used.

By run “extract.command” from the terminal and give him the Third-party library path in JAR folder, the  “extract.command”  will convert .JAR library to source code and save Java source code in output Folder, and show which dangerous permissions are used by this library.


##Results:
	After we run our script on 15 of Third-party libraries as it show in Table(1). We Find out the dangerous permissions that used by every library and  we categorize the used permissions into two types symbol. First, We used “X” for the library that used dangerous permissions without any convincing reason for using this permission. For example; Aquery library asking for access to user location, it is not convincing to allow to HTTP library to access to user location, it just  used to get data from HTTP server. Second, We used  “O”   for the library that used dangerous permissions with convincing reason for using this permission. For example; Picasso library has convincing reason to access to storage, because it need to cache HTTP images into user phone, so when we ask same HTTP url again, it will bring it from user phone.

Table(1) Dangerous  permission using
 	After analysis the permissions that used by every library we find out. Aquery which  is HTTP library which helps to get HTTP response from webservice in easy way, we find out this library  try to access to user location, we think  that there is not convincing reason for  HTTP library   to access to user location.  Picasso which  is HTTP cache library which helps to get Images  from web service, and cache it in user phone. We find out this library  try to access to phone status and storage. We think  that access to phone storage, it is normal, because it this library need to save images in user phone for cache purpose so when we call same URL again it will get it from user phone. While we think  that there is not convincing reason for  HTTP library   to access to phone status. 
 AppoDeal which  is Ads library that display Ads on user phone, try to access to (user location, storage, and phone network status). We think that access to phone network it is normal, because it helps Ads company to target the user according their location and that what we find in the Java code, however we think that  there is not convincing reason to access to user location or phone storage. If they are use user location for Ads purpose it it better to  target audience according to phone network is enough.  
AppsFlye, and adcolony which  is Ads library that display Ads on user phone, try to access to  phone network status. We think  that access to phone network it is normal, because it helps Ads company to target the user according their location and that what we find in the Java code. 
chartboost which  is Ads library that display Ads on user phone, try to access to ( user location, and phone network status). We think  that access to phone network it is normal, because it helps Ads company to target the user according their location and that what we find in the Java code, however we think that  there is not convincing reason to access to user location. If they are use user location for Ads purpose it it better to  target audience according to phone network is enough.
  In Mobile which  is Ads library that display Ads on user phone, try to access to (Calendar, user location, storage, and phone network status). We think  that access to phone network it is normal, because it helps Ads company to target the user according their location and that what we find in the Java code, however we think that  there is not convincing reason to access to  Calendar, user location or phone storage.  If they are use user location for Ads purpose it it better to  target audience according to phone network is enough.
Ion, retrofit, okhttp, volley, gson, Dagger, and HTTP-Async , we find these library did not access to any of user dangerous permissions which is good. 
 

##Conclusion :
	 We build tool that helps Android developers to keep the privacy information of their apps’ users more secure.  By look for Third-party libraries permissions in deep code to find the dangerous permissions that used by the Third-party library which  could use developer app permissions to access to user privacy information. The developer could test these libraries before add them to his app and make sure the library is secure, which lead to more secure apps and keep Google play’s apps users privacy information more secure by detect dangerous permissions that used by the third-party library  inside developers’ apps.



##References
###1- Wang, Y., Hariharan, S., Zhao, C., Liu, J., & Du, W. (2014, March). Compac: Enforce component-level access control in Android. In Proceedings of the 4th ACM conference on Data and application security and privacy (pp. 25-36). ACM.

###2- Felt, A. P., Chin, E., Hanna, S., Song, D., & Wagner, D. (2011, October). Android permissions demystified. In Proceedings of the 18th ACM conference on Computer and communications security (pp. 627-638). ACM.

###3- Wang, D., Dai, S., Ding, Y., Li, T., & Han, X. (2014, November). POSTER: AdHoneyDroid--Capture Malicious Android Advertisements. In Proceedings of the 2014 ACM SIGSAC Conference on Computer and Communications Security (pp. 1514-1516). ACM.

###4- Google developer ,https://developer.android.com/guide/topics/security/permissions.html


