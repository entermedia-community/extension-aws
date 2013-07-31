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