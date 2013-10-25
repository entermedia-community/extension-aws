importPackage( Packages.com.openedit.util );
importPackage( Packages.java.util );
importPackage( Packages.java.lang );
importPackage( Packages.com.openedit.modules.update );

var root = moduleManager.getBean("root").getAbsolutePath();
var eml = root + "/showcase";
var tmp = root + "/WEB-INF/temp/upgrade/";


var files = new FileUtils();
log.add("4. UPGRADE BASE DIR");
files.deleteAll( root + "/WEB-INF/base/showcase");
files.copyFiles( tmp + "/showcase", root + "/WEB-INF/base/showcase/");

