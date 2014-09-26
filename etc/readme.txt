#sudo apt-get remove ffmpeg x264 libx264-dev

yum remove ffmpeg x264 libx264-dev faac-devel lame-devel

wget ftp://ftp.videolan.org/pub/x264/snapshots/last_x264.tar.bz2

./configure --enable-shared --enable-static --prefix=/usr
./configure --enable-shared  --prefix=/usr
#./configure --enable-static

make
make install
ldconfig

yum install faac-devel lame-devel

wget http://git.libav.org/?p=libav.git;a=snapshot;h=HEAD;sf=tgz

./configure --enable-libx264  --enable-libfaac --enable-libmp3lame   --enable-gpl --enable-nonfree

make
make install

#known bugs: -deinterlace crashes


 <!-- 
  
  http://videomam.news.com.au.s3.amazonaws.com/media/2014/07/160f3782-6dfc-4870-89f2-32dfb487f51d/NewsCorpAustralia.mov
  
  vs. 
  
  http://s3-ap-southeast-2.amazonaws.com/videomam.news.com.au/media/2014/07/160f3782-6dfc-4870-89f2-32dfb487f51d/NewsCorpAustralia.mov
  
  aws s3 ls s3://videomam.news.com.au/media/2014/07/160f3782-6dfc-4870-89f2-32dfb487f51d/NewsCorpAustralia.mov
  
   aws s3 ls s3://videomam.news.com.au/media/   s3-ap-southeast-2.amazonaws.com/videosmam.news.com.au
   
   aws s3 cp version.txt s3://videomam.news.com.au/media/ --grants read=uri=http://acs.amazonaws.com/groups/global/AllUsers
   

http://docs.aws.amazon.com/cli/latest/reference/s3api/put-bucket-policy.html
http://docs.aws.amazon.com/AmazonS3/latest/dev/HostingWebsiteOnS3Setup.html

aws s3api put-bucket-policy --bucket videomam.news.com.au --policy file://policy.json

   <mount path="/WEB-INF/data/mam/catalog/originals"  externalpath="./s3cache/originals" repositorytype="S3Repository">
	  <property name="accesskey">AKIAJNQ3JLXXX</property>
	  <property name="secretkey">70xTYAbOvfLenjgGqjXXXX</property>
	  <property name="bucket">videomam.news.com.au</property>
  </mount>
  
   <property name="accesskey">AKIAJ3I5YRDDG66QBU3A</property>
	  <property name="secretkey">XD1CeXAhP5imBp3wvRKXXXXX</property>
	  <property name="bucket">entermedia.test</property>
	  
	  
  
	