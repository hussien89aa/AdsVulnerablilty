 #read jar path from screen
 echo "enter jar file path :";
 read JarFileName;
 # convert APK to jar
#dex2jar-2.0/d2j-dex2jar.sh -f -o output_jar.jar /Users/hussienalrubaye/Desktop/Ads\ Library/story.apk 
echo "******** starting extract file *******";
#covert jar to code 
OutputPath="./output/${JarFileName}"
mkdir $OutputPath;
 java -jar cfr_0_114.jar   $JarFileName   --outputdir $OutputPath;
echo "********** extract done ***********";
echo "********** start searching for vulnerability  ***********";
cd $OutputPath ;
#Search for dangerous permisssion
grep -R 'android.location.LocationManager' *   ;
grep -R 'android.telephony.SmsManager' *  ;
grep -R 'android.provider.Telephony.Sms' *  ;
grep -R 'android.provider.ContactsContract' * ;
grep -R 'android.provider.MediaStore' * ;
grep -R 'android.hardware.SensorManager' * ;
grep -R 'android.telephony.TelephonyManager' * ;
grep -R 'android.media.AudioRecord' * ;
grep -R 'android.graphics.Camera' * ;
grep -R 'android.provider.CalendarContract' *  ;
 
#open the output file
#open .;
echo "********** searching is done ***********";
echo "********** Results will be in "output" folder ***********";
