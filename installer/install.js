importPackage( Packages.com.openedit.util );
importPackage( Packages.java.util );
importPackage( Packages.java.lang );
importPackage( Packages.com.openedit.modules.update );

var root = moduleManager.getBean("root").getAbsolutePath();
var tmp = root + "/WEB-INF/temp/upgrade/";


var files = new FileUtils();
log.add("UPGRADE BASE DIR");
files.deleteAll( root + "/WEB-INF/base/aws");
files.copyFiles( tmp + "/WEB-INF/base/aws", root + "/WEB-INF/base/aws");

