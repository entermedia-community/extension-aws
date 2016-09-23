importPackage( Packages.org.openedit.util );
importPackage( Packages.java.util );
importPackage( Packages.java.lang );
importPackage( Packages.java.io );
importPackage( Packages.org.entermediadb.modules.update );




var name = "extension-aws";

var war = "http://dev.entermediasoftware.com/jenkins/job/@BRANCH@_" + name + "/lastSuccessfulBuild/artifact/deploy/" + name + ".zip";

var root = moduleManager.getBean("root").getAbsolutePath();
var web = root + "/WEB-INF";
var tmp = web + "/tmp";

log.info("1. GET THE LATEST WAR FILE");



log.info("1. GET THE LATEST WAR FILE");
var downloader = new Downloader();
downloader.download( war, tmp + "/extension-aws.zip");

log.info("2. UNZIP WAR FILE");
var unziper = new ZipUtil();
unziper.unzip(  tmp + "/extension-aws.zip",  tmp );


log.info("3. Copy Over Site " + tmp + "/unzip/" + " " + "to " + root);
var files = new FileUtils();
files.deleteMatch( web + "/lib/@BRANCH@extension-aws*.jar");
files.deleteMatch( web + "/lib/aws-java*.jar");
files.deleteMatch( web + "/lib/jackson*.jar");
files.deleteMatch( web + "/lib/joda*.jar");
files.deleteMatch( web + "/base/aws*.jar");
files.deleteMatch( web + "/lib/dev_extension-aws*.jar");
files.deleteMatch( web + "/lib/extension-aws*.jar");



files.copyFileByMatch( tmp + "/lib/*extension-aws*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/lib/aws-java*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/lib/jackson*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/lib/joda*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/lib/aws*.jar", web + "/lib/");

files.deleteMatch( web + "/WEB-INF/base/aws/")
files.copyFileByMatch( tmp + "/base/aws/", root + "/WEB-INF/base/aws/");



log.info("5. CLEAN UP");
files.deleteAll(tmp);

log.info("6. UPGRADE COMPLETED");
