importPackage( Packages.com.openedit.util );
importPackage( Packages.java.util );
importPackage( Packages.java.lang );
importPackage( Packages.com.openedit.modules.update );

var name = "extension-aws";

var war = "http://dev.entermediasoftware.com/jenkins/job/@BRANCH@_" + name + "/lastSuccessfulBuild/artifact/deploy/" + name + ".zip";

var root = moduleManager.getBean("root").getAbsolutePath();
var web = root + "/WEB-INF";
var tmp = web + "/tmp";

log.info("1. GET THE LATEST WAR FILE");
var downloader = new Downloader();
downloader.download( war, tmp + "/" + name + ".zip");

log.info("2. UNZIP WAR FILE");
var unziper = new ZipUtil();
unziper.unzip(  tmp + "/" + name + ".zip",  tmp + "/unzip/" );

log.info("3. Copy Over Site " + tmp + "/unzip/" + " " + "to " + root);
var files = new FileUtils();
files.deleteMatch( web + "/lib/@BRANCH@extension-aws*.jar");
files.deleteMatch( web + "/lib/aws-java*.jar");
files.deleteMatch( web + "/lib/jackson*.jar");
files.deleteMatch( web + "/lib/joda*.jar");
files.deleteMatch( web + "/base/aws*.jar");
files.copyFiles( tmp + "/unzip", root);


files.deleteMatch( web + "/lib/dev_extension-aws*.jar");
files.deleteMatch( web + "/lib/extension-aws*.jar");



files.copyFileByMatch( tmp + "/lib/@BRANCH@extension-aws*.jar", web + "/lib/");




log.info("5. CLEAN UP");
files.deleteAll(tmp);

log.info("6. UPGRADE COMPLETED");
